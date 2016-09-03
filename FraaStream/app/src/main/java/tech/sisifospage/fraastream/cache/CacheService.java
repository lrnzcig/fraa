package tech.sisifospage.fraastream.cache;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Logging;

import tech.sisifospage.fraastream.StreamingActivity;
import tech.sisifospage.fraastream.bbdd.AccDataContract;
import tech.sisifospage.fraastream.bbdd.FraaDbHelper;
import tech.sisifospage.fraastream.stream.FraaStreamDataUnit;

public class CacheService extends Service implements ServiceConnection {

    private static final String TAG = "MetaWear.CacheSrv";

    // used by scanner to pass the device
    public static final String EXTRA_BT_DEVICE = "tech.sisifospage.fraastream.EXTRA_BT_DEVICE";


    // device
    private MetaWearBoard mwBoard;
    private Accelerometer accelModule;
    // TODO línea original con frecuencia de muestreo del alcelerómetro
    //private static final float ACC_RANGE = 8.f, ACC_FREQ = 50.f;
    private static final float ACC_RANGE = 8.f, ACC_FREQ = 10.f;
    private static final String STREAM_KEY = "accel_stream";
    private BluetoothDevice btDevice;
    // device logging
    private Logging loggingModule;

    // cache for keeping the data
    private AccDataCacheSingleton cache;

    // index for data being read from device
    private Integer index;

    // handler for connecting to the board
    private final MetaWearBoard.ConnectionStateHandler stateHandler = new MetaWearBoard.ConnectionStateHandler() {
        @Override
        public void connected() {
            Log.i(TAG, "Connected");
            if (accelModule == null) {
                Log.d(TAG, "Get accelModule after reconnect");
                getAccelModule();
            }
            if (! cache.isStarted()) {
                Log.d(TAG, "Restart streaming after reconnect");
                startStreaming();
            }

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

    public CacheService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // from scanner

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "startCommand");
        int out = super.onStartCommand(intent, flags, startId);

        btDevice = intent.getParcelableExtra(EXTRA_BT_DEVICE);

        bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);
        return out;
    }

    private void getAccelModule() {
        try {
            accelModule = mwBoard.getModule(Accelerometer.class);
            loggingModule = mwBoard.getModule(Logging.class);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
        }
    }

    private void startStreaming() {
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
                        getIndexAndIncrement();

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
    }

    private synchronized int getIndexAndIncrement() {
        return index++;
    }


    private void stopStreaming() {
        accelModule.disableAxisSampling(); //Likewise, you must first disable axis sampling before stopping
        accelModule.stop();
        cache.setStarted(false);

        String message = "Total number of rows inserted " + index;
        Log.i(TAG, message);
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.i(TAG, "onServiceConnected");
        index = 1;
        ///< Typecast the binder to the service's LocalBinder class
        mwBoard = ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(stateHandler);
        getAccelModule();

        cache = AccDataCacheSingleton.getInstance(this.getBaseContext());
        startStreaming();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}