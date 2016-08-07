package tech.sisifospage.fraastream.stream;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.mbientlab.metawear.data.CartesianFloat;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import tech.sisifospage.fraastream.StreamingActivity;
import tech.sisifospage.fraastream.cache.AccDataCacheSingleton;

public class UpstreamService extends Service {

    // extra parameters for init
    public static final String SQLITE_HEADER_ID = "SQLITE_HEADER_ID";
    public static final String SQLITE_HEADER_MAC_ADDR = "SQLITE_HEADER_MAC_ADDR";
    public static final String SQLITE_HEADER_CREATED_AT = "SQLITE_HEADER_CREATED_AT";
    public static final String SQLITE_HEADER_LABEL = "SQLITE_HEADER_LABEL";

    private int headerId;
    private String macAddress;
    private long createdAt;
    private String label;

    private int serverHeaderId;

    private Map<UUID, FraaStreamData> pendingRequests = new HashMap<>();

    // TODO app property
    private static final String server_url = "http://192.168.1.128:8080/fraastreamserver/webapi/";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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
        Bundle extras = intent.getExtras();
        this.headerId = (int) extras.get(SQLITE_HEADER_ID);
        Log.d(StreamingActivity.TAG, "headerId:" + this.headerId);
        this.macAddress = (String) extras.get(SQLITE_HEADER_MAC_ADDR);
        Log.d(StreamingActivity.TAG, "macAddress:" + this.macAddress);
        this.createdAt = (long) extras.get(SQLITE_HEADER_CREATED_AT);
        Log.d(StreamingActivity.TAG, "createdAt:" + this.createdAt);
        this.label = (String) extras.get(SQLITE_HEADER_LABEL);
        Log.d(StreamingActivity.TAG, "label:" + this.label);

        // get server side header id and update SQLite
        getHeaderIdFromServer();

        // flush all data already present in the device for previous headers
        sendAllButCurrentHeaderToServer();

        // process for sending data periodically
        checkDataEveryInterval();
        return out;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(StreamingActivity.TAG, "onCreate");
    }

    // TODO
    public void checkDataEveryInterval() {
        final Runnable checkData = new Runnable() {
            public void run() { Log.d(StreamingActivity.TAG, "new beep"); }
        };
        // runs every 10 seconds
        final ScheduledFuture<?> processHandle = scheduler.scheduleAtFixedRate(checkData, 10, 10, TimeUnit.SECONDS);    // TODO maybe schedule instead of AtFixedRate
        // stop after 1 hour ==> somebody has to stop it!
        scheduler.schedule(new Runnable() {
            public void run() {
                Log.d(StreamingActivity.TAG, "cancel beep");
                processHandle.cancel(true);
            }
        }, 60 * 3, TimeUnit.SECONDS);
    }


    private void getHeaderIdFromServer() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = server_url + "header";

        FraaStreamHeader header = new FraaStreamHeader();
        header.setMacAddress(macAddress);
        header.setCreatedAt(new Date(createdAt));
        header.setLabel(label);

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
        Log.d(StreamingActivity.TAG, "Looking for rows with id different to: " + headerId);
        Collection<FraaStreamData> list = obj.selectRowsHeaderNotEqualto(headerId);
        Log.d(StreamingActivity.TAG, "Sending data from previous runs, size of list: " + list.size());
        for (FraaStreamData data : list) {
            if (data.getHeaderId() == AccDataCacheSingleton.NULL_SERVER_HEADER_ID) {
                continue;
            }
            UUID requestId = UUID.randomUUID();
            data.setRequestId(requestId);
            pendingRequests.put(requestId, data);
            Log.d(StreamingActivity.TAG, "Sending data for server header id: " + data.getHeaderId());
            sendToServer(data);
        }
    }

    private void sendToServer(FraaStreamData data) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = server_url + "data";
        GsonRequest postRequest = new GsonRequest<FraaStreamData, UUID>(Request.Method.POST, url, data, null,
                new Response.Listener<UUID>() {
                    @Override
                    public void onResponse(UUID response) {
                        Log.i(StreamingActivity.TAG, "Response id to be removed: " + response + "(" + pendingRequests.get(response).getHeaderId() + ")");
                        // update new serverHeaderId
                        AccDataCacheSingleton obj = AccDataCacheSingleton.getInstance();
                        obj.removeFromDatabase(pendingRequests.get(response));
                        Log.i(StreamingActivity.TAG, "Response id finished removing: " + response + "(" + pendingRequests.get(response).getHeaderId() + ")");
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


}
