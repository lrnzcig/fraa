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
import tech.sisifospage.fraastream.cache.CacheService;
import tech.sisifospage.fraastream.stream.FraaStreamDataUnit;

public class StreamingActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "MetaWear.StreamActivity";
    private MetaWearBoard mwBoard;

    // connect to device
    private Button connect;

    // accelerometer
    private Switch accel_switch;

    // used by scanner to pass the device
    public static final String EXTRA_BT_DEVICE = "tech.sisifospage.fraastream.EXTRA_BT_DEVICE";
    private BluetoothDevice btDevice;

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
        ///< Typecast the binder to the service's LocalBinder class
        mwBoard = ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);

        if (! mwBoard.isConnected()) {
            // start cache service
            Log.i(TAG, "Restarting CacheService");
            Intent cacheServiceIntent = new Intent(this, CacheService.class);
            cacheServiceIntent.putExtra(CacheService.EXTRA_BT_DEVICE, btDevice);
            startService(cacheServiceIntent);
        }

        accel_switch = (Switch) findViewById(R.id.accel_switch);

        //cache = AccDataCacheSingleton.getInstance(this.getBaseContext());
        if (accel_switch.isChecked()) {
            Log.d(TAG, "Activity has been restarted");

        }

        accel_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.i("Switch State=", "" + isChecked);
                if (isChecked) {

                } else {

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
    }
}

