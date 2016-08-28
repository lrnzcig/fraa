package tech.sisifospage.fraastream;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Logging;

import tech.sisifospage.fraastream.bbdd.AccDataContract;
import tech.sisifospage.fraastream.bbdd.FraaDbHelper;
import tech.sisifospage.fraastream.cache.AccDataCacheSingleton;
import tech.sisifospage.fraastream.stream.FraaStreamDataUnit;

public class StreamingActivity extends AppCompatActivity implements ServiceConnection {

    private MetaWearBleService.LocalBinder serviceBinder;

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
    private final MetaWearBoard.ConnectionStateHandler stateHandler = new MetaWearBoard.ConnectionStateHandler() {
        @Override
        public void connected() {
            Log.i(TAG, "Connected");
        }

        @Override
        public void disconnected() {
            Log.i(TAG, "Connected Lost");
            mwBoard.connect();
        }

        @Override
        public void failure(int status, Throwable error) {
            Log.e(TAG, "Error connecting", error);
            Log.i(TAG, "Reconnecting");
            mwBoard.connect();
        }
    };


    // cache for keeping the data
    private AccDataCacheSingleton cache;

    // index for data being read from device
    private Integer index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streaming);

        // disconnect button
        connect = (Button) findViewById(R.id.disconnect);
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
        btDevice = getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
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
        mwBoard = ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(stateHandler);
        try {
            accelModule = mwBoard.getModule(Accelerometer.class);
            loggingModule = mwBoard.getModule(Logging.class);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
        }
        accel_switch = (Switch) findViewById(R.id.accel_switch);

        cache = AccDataCacheSingleton.getInstance(this.getBaseContext());

        accel_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i("Switch State=", "" + isChecked);
                if (isChecked) {
                    accelModule.setOutputDataRate(ACC_FREQ);
                    accelModule.setAxisSamplingRange(ACC_RANGE);

                    cache.start(mwBoard.getMacAddress());

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
                                    //Log.i(TAG, axes.toString());

                                    FraaStreamDataUnit unit = new FraaStreamDataUnit();
                                    unit.setIndex(index);
                                    unit.setX(axes.x());
                                    unit.setY(axes.y());
                                    unit.setZ(axes.z());
                                    index++;

                                    cache.add(unit);
                                    if (index % 100 == 0) {
                                        Log.i(TAG, "New row id inserted " + index);
                                    }

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
                    cache.setStarted(true);
                } else {
                    accelModule.disableAxisSampling(); //Likewise, you must first disable axis sampling before stopping
                    accelModule.stop();
                    cache.setStarted(false);

                    // TODO select count => move to Singleton
                    FraaDbHelper fraaDbHelper = new FraaDbHelper(getBaseContext());    // TODO is context ok?
                    SQLiteDatabase db = fraaDbHelper.getReadableDatabase();
                    long count = DatabaseUtils.queryNumEntries(db, AccDataContract.AccDataEntry.TABLE_NAME,
                            AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID + "=?", new String[]{"10"});

                    //String message = "Total number of rows inserted " + count;
                    String message = "Total number of rows inserted " + index;
                    Log.i(TAG, message);
                }
            }

        });

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "StreamingActivity onStop");
        // TODO backgroud process for UpstreamService
    }
}

