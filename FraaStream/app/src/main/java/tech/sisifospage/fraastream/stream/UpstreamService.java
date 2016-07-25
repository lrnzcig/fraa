package tech.sisifospage.fraastream.stream;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.mbientlab.metawear.data.CartesianFloat;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import tech.sisifospage.fraastream.StreamingActivity;

public class UpstreamService extends Service {

    // TODO app property
    private static final String server_url = "http://192.168.1.128:8080/fraastreamserver/webapi/";


    public UpstreamService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(StreamingActivity.TAG, "onCreate");
    }

    /**
     * TODO's:
     * - cleanup, in particular headers probably not necesary, improve helper class
     * - get execution index from server (and after async task reading from db, split this method in 2)
     *
     * @param axes
     */
    private void sendToServer(CartesianFloat axes) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = server_url + "data";

                                    /*
                                    // Request a string response from the provided URL.
                                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                                            new Response.Listener<String>() {
                                                @Override
                                                public void onResponse(String response) {
                                                    // Display the first 500 characters of the response string.
                                                    //Log.i(TAG, "Response is: "+ response.substring(0,500));
                                                    Log.i(TAG, "Response is: "+ response.substring(0,3));
                                                }
                                            }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.i(TAG, "That didn't work!");
                                        }
                                    });
                                    // Add the request to the RequestQueue.
                                    queue.add(stringRequest);
                                    */
        FraaStreamDataUnit unit = new FraaStreamDataUnit();
        unit.setIndex(unit.getIndex());
        unit.setX(axes.x());
        unit.setY(axes.y());
        unit.setZ(axes.z());

        FraaStreamData data = new FraaStreamData();
        data.setHeaderId(BigInteger.TEN);   // TODO get the header id from the server !!
        data.addDataUnit(unit);


        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        headers.put("content-length", "42");
        headers.put("accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
        headers.put("connection", "keep alive");
        headers.put("host", "192.168.1.130:8080");
        // TODO headers

        GsonRequest postRequest = new GsonRequest(Request.Method.POST, url, data, null,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        //Log.i(TAG, "Response is: "+ response.substring(0,500));
                        Log.i(StreamingActivity.TAG, "Response is: " + response.substring(0, 3));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(StreamingActivity.TAG, "That didn't work!");
                Log.i(StreamingActivity.TAG, error.toString());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(postRequest);

    }

}
