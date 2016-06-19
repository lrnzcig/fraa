package tech.sisifospage.fraastream;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Logging;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class StreamingActivity extends AppCompatActivity implements ServiceConnection {

    private MetaWearBleService.LocalBinder serviceBinder;

    private static final String server_url = "http://192.168.1.128:8080/fraastreamserver/webapi/";

    public static final String TAG = "MetaWear";
    private MetaWearBoard mwBoard;

    // connect to device
    private Button connect;

    // accelerometer
    private Switch accel_switch;
    private Accelerometer accelModule;
    // TODO línea original con frecuencia de muestreo del alcelerómetro
    //private static final float ACC_RANGE = 8.f, ACC_FREQ = 50.f;
    private static final float ACC_RANGE = 8.f, ACC_FREQ = 10.f;
    private static final String STREAM_KEY = "accel_stream";

    // device logging
    private Logging loggingModule;

    // used by scanner to pass the device
    public static final String EXTRA_BT_DEVICE = "tech.sisifospage.fraastream.EXTRA_BT_DEVICE";
    private BluetoothDevice btDevice;

    // handler for connecting to the board
    private final MetaWearBoard.ConnectionStateHandler stateHandler= new MetaWearBoard.ConnectionStateHandler() {
        @Override
        public void connected() {
            Log.i(TAG, "Connected");
        }

        @Override
        public void disconnected() {
            Log.i(TAG, "Connected Lost");
        }

        @Override
        public void failure(int status, Throwable error) {
            Log.e(TAG, "Error connecting", error);
        }
    };

    // index for data being sent to device
    private Integer index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);

        // disconnect button
        connect=(Button)findViewById(R.id.disconnect);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Clicked disconnect");
                mwBoard.setConnectionStateHandler(null);
                mwBoard.disconnect();
                finish();
            }
        });

        // from scanner
        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(TAG, "onServiceConnected");
        index = 1;
        ///< Typecast the binder to the service's LocalBinder class
        mwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(stateHandler);
        try {
            accelModule = mwBoard.getModule(Accelerometer.class);
            loggingModule = mwBoard.getModule(Logging.class);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
        }
        accel_switch = (Switch) findViewById(R.id.accel_switch);
        accel_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i("Switch State=", "" + isChecked);
                if (isChecked) {
                    accelModule.setOutputDataRate(ACC_FREQ);
                    accelModule.setAxisSamplingRange(ACC_RANGE);

                    // streaming
                    accelModule.routeData()
                            .fromAxes().stream(STREAM_KEY)
                            .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe(STREAM_KEY, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message message) {
                                    CartesianFloat axes = message.getData(CartesianFloat.class);
                                    Log.i(TAG, axes.toString());


                                    // Instantiate the RequestQueue.
                                    RequestQueue queue = Volley.newRequestQueue(StreamingActivity.this);
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
                                    unit.setIndex(BigInteger.valueOf(index));
                                    unit.setX(axes.x());
                                    unit.setY(axes.y());
                                    unit.setZ(axes.z());
                                    index++;

                                    FraaStreamData data = new FraaStreamData();
                                    data.setHeaderId(BigInteger.TEN);   // TODO get the header id from the server !!
                                    data.addDataUnit(unit);


                                    Map<String,String> headers = new HashMap<>();
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
                                                    Log.i(TAG, "Response is: "+ response.substring(0,3));
                                                }
                                            }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.i(TAG, "That didn't work!");
                                            Log.i(TAG, error.toString());
                                        }
                                    });
                                    // Add the request to the RequestQueue.
                                    queue.add(postRequest);
                                }

                            });
                        }

                        @Override
                        public void failure(Throwable error) {
                            Log.e(TAG, "Error committing route", error);
                        }
                    });


                    accelModule.enableAxisSampling(); //You must enable axis sampling before you can start
                    accelModule.start();
                } else {
                    accelModule.disableAxisSampling(); //Likewise, you must first disable axis sampling before stopping
                    accelModule.stop();
                }
            }

        });

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) { }

}

