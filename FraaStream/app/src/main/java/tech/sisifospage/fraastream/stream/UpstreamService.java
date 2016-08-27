package tech.sisifospage.fraastream.stream;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import tech.sisifospage.fraastream.StreamingActivity;
import tech.sisifospage.fraastream.cache.AccDataCacheSingleton;

public class UpstreamService extends Service {

    private final int MAX_NUMBER_DATA_UNITS_UPSTREAM = 500;
    private final int MAX_NUMBER_ATTEMPTS_PER_REQUEST = 3;

    private int serverHeaderId;

    private Map<UUID, FraaStreamData> pendingRequests = new HashMap<>();
    private Map<UUID, Integer> pendingAttempts = new HashMap<>();
    private Map<Integer, Collection<FraaStreamMetaData>> pendingRequestsMetadata = new HashMap<>();

    // TODO app property
    private static final String server_url = "http://192.168.1.128:8080/fraastreamserver/webapi/";

    private final ScheduledExecutorService dataScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> dataProcessHandle = null;

    private final ScheduledExecutorService cacheScheduler = Executors.newScheduledThreadPool(1);

    public UpstreamService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int out = super.onStartCommand(intent, flags, startId);
        Log.d(StreamingActivity.TAG, "onStartCommand");

        // get server side header id and update SQLite
        getHeaderIdFromServer();

        // flush all data already present in the device for previous headers
        sendAllButCurrentHeaderToServer();

        // process for sending data periodically
        checkDataEveryInterval();

        // process for checking if the cache has added data
        checkCacheDataStream();

        return out;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(StreamingActivity.TAG, "onCreate");
    }

    public void checkDataEveryInterval() {
        final Runnable checkData = new Runnable() {
            public void run() {
                //Log.d(StreamingActivity.TAG, "new beep");
                boolean dataSent = false;
                for (UUID requestId : pendingRequests.keySet()) {
                    Integer attempts = pendingAttempts.get(requestId);
                    if (attempts == null) {
                        attempts = 1;
                    } else if (attempts == MAX_NUMBER_ATTEMPTS_PER_REQUEST) {
                        continue;
                    } else {
                        attempts++;
                    }
                    pendingAttempts.put(requestId, attempts);
                    FraaStreamData data = pendingRequests.get(requestId);
                    Log.d(StreamingActivity.TAG, "Sending data for server header id: " + data.getHeaderId()
                        + ", attempt: " + attempts + " request id: " + requestId);
                    sendToServer(data);
                    dataSent = true;
                    break;
                }
                if (! dataSent & ! isStarted()) {
                    Log.d(StreamingActivity.TAG, "No more upstream requests pending (or too many failed attempts)");
                    dataProcessHandle.cancel(true);
                } else {
                    //Log.d(StreamingActivity.TAG, "end beep");
                }
            }
        };
        // runs every 60 seconds
        dataProcessHandle = dataScheduler.scheduleAtFixedRate(checkData, 10, 20, TimeUnit.SECONDS);
    }

    public void checkCacheDataStream() {
        final Runnable checkData = new Runnable() {
            public void run() {
                AccDataCacheSingleton obj = AccDataCacheSingleton.getInstance();
                Collection<FraaStreamData> list = convertWithMaxDataSize(obj.selectRowsHeaderEqualTo(getHeaderId()));
                boolean dataPending = false;
                for (FraaStreamData data : list) {
                    if (data.getDataUnits().length == MAX_NUMBER_DATA_UNITS_UPSTREAM) {
                        addDataToPendingRequests(data);
                        dataPending = true;
                    }
                }
                if (dataPending && dataProcessHandle.isCancelled()) {
                    Log.d(StreamingActivity.TAG, "restarting data process");
                    checkDataEveryInterval();
                }
            }
        };
        // runs every 2 minutes, never stops
        // TODO adjust time interval depending on size of buffers & sampling rate
        cacheScheduler.scheduleAtFixedRate(checkData, 0, 2, TimeUnit.MINUTES);
    }

    private void getHeaderIdFromServer() {
        FraaStreamHeader header = new FraaStreamHeader();
        header.setMacAddress(getMacAddress());
        header.setCreatedAt(new Date(getCreatedAt()));
        header.setLabel(getHeaderLabel());

        getHeaderIdFromServer(header, getHeaderId());
    }

    private void getHeaderIdFromServer(FraaStreamHeader header, final int headerId) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = server_url + "header";

        GsonRequest postRequest = new GsonRequest<FraaStreamHeader, FraaStreamHeader>(Request.Method.POST, url, header, null,
                new Response.Listener<FraaStreamHeader>() {
                    @Override
                    public void onResponse(FraaStreamHeader response) {
                        serverHeaderId = response.getId();
                        Log.d(StreamingActivity.TAG, "Response header id: " + response.getId());
                        // update new serverHeaderId
                        AccDataCacheSingleton obj = AccDataCacheSingleton.getInstance();
                        obj.setServerHeaderId(headerId, serverHeaderId);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(StreamingActivity.TAG, "getHeaderIdFromServer didn't work!");
                Log.i(StreamingActivity.TAG, error.toString());
            }
        }, FraaStreamHeader.class);
        // Add the request to the RequestQueue.
        queue.add(postRequest);

    }


    private void sendAllButCurrentHeaderToServer() {
        AccDataCacheSingleton obj = AccDataCacheSingleton.getInstance();
        Collection<FraaStreamData> list = convertWithMaxDataSize(obj.selectRowsHeaderNotEqualTo(getHeaderId()));
        Log.d(StreamingActivity.TAG, "Data from previous runs, size of list: " + list.size());
        for (FraaStreamData data : list) {
            addDataToPendingRequests(data);
        }
    }

    private void addDataToPendingRequests(FraaStreamData data) {
        // check if exactly the same request has been added to pendingRequestsMetadata
        FraaStreamMetaData metaData = getMetaData(data);
        if (alreadyRequested(data, metaData)) {
            return;
        }
        // add metadata to pendingRequestsMetadata
        Collection<FraaStreamMetaData> list = pendingRequestsMetadata.get(data.getHeaderId());
        if (list == null) {
            list = new ArrayList<>();
            pendingRequestsMetadata.put(data.getHeaderId(), list);
        }
        list.add(metaData);
        // generate the request itself
        UUID requestId = UUID.randomUUID();
        data.setRequestId(requestId);
        pendingRequests.put(requestId, data);
        Log.d(StreamingActivity.TAG, "Added request to be processed: " + requestId);
    }

    private boolean alreadyRequested(FraaStreamData data, FraaStreamMetaData metaData) {
        Collection<FraaStreamMetaData> list = pendingRequestsMetadata.get(data.getHeaderId());
        if (list == null) {
            return false;
        }
        for (FraaStreamMetaData candidate : list) {
            if (candidate.maxIndex == metaData.maxIndex
                    && candidate.minIndex == metaData.minIndex) {
                return true;
            }
        }
        return false;
    }

    private FraaStreamMetaData getMetaData(FraaStreamData data) {
        Integer minIndex = null;
        Integer maxIndex = null;
        for (FraaStreamDataUnit unit : data.getDataUnits()) {
            int index = unit.getIndex();
            if (minIndex == null) {
                minIndex = index;
            } else if (minIndex > index) {
                minIndex = index;
            }
            if (maxIndex == null) {
                maxIndex = index;
            } else if (maxIndex < index) {
                maxIndex = index;
            }
        }
        Log.d(StreamingActivity.TAG, "Min index: " + minIndex + ", max index: " + maxIndex);
        return new FraaStreamMetaData(minIndex, maxIndex);
    }

    /**
     * Converts into a list for FraaStreamData taking into account the maximum size of a packet
     *
     * @param input
     * @return
     */
    private Collection<FraaStreamData> convertWithMaxDataSize(Map<Integer, Collection<FraaStreamDataUnit>> input) {
        Collection<FraaStreamData> output = new ArrayList<>();
        for (Integer headerId : input.keySet()) {
            AccDataCacheSingleton cache = AccDataCacheSingleton.getInstance();
            Integer serverHeaderId = cache.getServerHeaderId(headerId);
            if (serverHeaderId == AccDataCacheSingleton.NULL_SERVER_HEADER_ID) {
                Log.w(StreamingActivity.TAG, "Data from headerId: " + headerId + " does not have a server header id");
                FraaStreamHeader header = cache.getHeader(headerId);
                getHeaderIdFromServer(header, headerId);
                // note it cannot be added as a request until it gets a headerId
                continue;
            }
            Collection<FraaStreamDataUnit> units = input.get(headerId);
            int count = 0;
            FraaStreamData data = null;
            for (FraaStreamDataUnit unit : units) {
                if (count == 0 ||
                        (count%MAX_NUMBER_DATA_UNITS_UPSTREAM == 0)) {
                    Log.d(StreamingActivity.TAG, "count for new data:" + count);
                    data = new FraaStreamData();
                    data.setHeaderId(serverHeaderId);
                    output.add(data);
                }
                data.addDataUnit(unit);
                count++;
            }
        }
        return output;
    }

    private void sendToServer(final FraaStreamData data) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = server_url + "data";
        GsonRequest postRequest = new GsonRequest<FraaStreamData, UUID>(Request.Method.POST, url, data, null,
                new Response.Listener<UUID>() {
                    @Override
                    public void onResponse(UUID response) {
                        if (response == null || pendingRequests.get(response) == null) {
                            return;
                        }
                        Log.i(StreamingActivity.TAG, "Response id to be removed: " + response + "(" + pendingRequests.get(response).getHeaderId() + ")");
                        // update new serverHeaderId
                        AccDataCacheSingleton obj = AccDataCacheSingleton.getInstance();
                        obj.removeFromDatabase(pendingRequests.get(response));
                        pendingRequests.remove(response);
                        Log.i(StreamingActivity.TAG, "Response id finished removing: " + response + "(" + data.getHeaderId() + ")");
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(StreamingActivity.TAG, "sendToServer didn't work!");
                Log.i(StreamingActivity.TAG, error.toString());
            }
        }, UUID.class);
        // avoid sending the data twice (big message over a slow network)
        postRequest.setRetryPolicy(new DefaultRetryPolicy(0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        // Add the request to the RequestQueue.
        queue.add(postRequest);
    }

    public long getCreatedAt() {
        AccDataCacheSingleton cache = AccDataCacheSingleton.getInstance();
        return cache.getCreatedAt();
    }

    public int getHeaderId() {
        AccDataCacheSingleton cache = AccDataCacheSingleton.getInstance();
        return cache.getHeaderId();
    }

    public String getHeaderLabel() {
        AccDataCacheSingleton cache = AccDataCacheSingleton.getInstance();
        return cache.getHeaderLabel();
    }

    public String getMacAddress() {
        AccDataCacheSingleton cache = AccDataCacheSingleton.getInstance();
        return cache.getMacAddress();
    }

    public boolean isStarted() {
        AccDataCacheSingleton cache = AccDataCacheSingleton.getInstance();
        return cache.isStarted();
    }
}
