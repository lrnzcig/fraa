package tech.sisifospage.fraastream.cache;

import android.util.Log;

import tech.sisifospage.fraastream.FraaStreamDataUnit;
import tech.sisifospage.fraastream.StreamingActivity;

/**
 * Created by lorenzorubio on 17/7/16.
 */
public class AccDataCacheSingleton {

    private static AccDataCacheSingleton accDataCacheSingleton;

    private static int LENGTH_OF_BUFFER = 10000;
    private static FraaStreamDataUnit[] units = null;
    private static int pointer = 0;

    public static AccDataCacheSingleton getInstance() {
        if (accDataCacheSingleton == null) {
            Log.d(StreamingActivity.TAG, "Creating cache singleton");
            accDataCacheSingleton = new AccDataCacheSingleton();
        }
        return accDataCacheSingleton;
    }

    public synchronized void add(FraaStreamDataUnit unit) {
        if (units == null) {
            initBuffer();
        }
        if (pointer == LENGTH_OF_BUFFER - 1) {
            Log.d(StreamingActivity.TAG, "Recreating singleton buffer");
            // TODO send buffer
            initBuffer();
        }
        units[pointer++] = unit;
    }

    private void initBuffer() {
        units = new FraaStreamDataUnit[LENGTH_OF_BUFFER];
        pointer = 0;
    }
}
