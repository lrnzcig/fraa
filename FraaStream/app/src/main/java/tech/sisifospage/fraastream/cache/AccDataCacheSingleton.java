package tech.sisifospage.fraastream.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import tech.sisifospage.fraastream.FraaStreamDataUnit;
import tech.sisifospage.fraastream.StreamingActivity;
import tech.sisifospage.fraastream.bbdd.AccDataContract;
import tech.sisifospage.fraastream.bbdd.FraaDbHelper;

/**
 * Created by lorenzorubio on 17/7/16.
 */
public class AccDataCacheSingleton {

    private static AccDataCacheSingleton accDataCacheSingleton;

    private static int LENGTH_OF_BUFFER = 500;

    private class Buffer {
        public FraaStreamDataUnit[] units;

        public Buffer() {
            this.units = new FraaStreamDataUnit[LENGTH_OF_BUFFER];
        }
    }
    private Buffer[] bufferOf2;
    private int bufferPointer;
    private int unitsPointer;
    private Integer bufferBackupPending;

    // context from activity
    private Context context;

    // database for local backup
    private SQLiteDatabase db;

    private int headerId;

    public static AccDataCacheSingleton getInstance() {
        if (accDataCacheSingleton == null) {
            Log.d(StreamingActivity.TAG, "Creating cache singleton");
            accDataCacheSingleton = new AccDataCacheSingleton();
        }
        return accDataCacheSingleton;
    }

    // this constructor is only initialized when calling from activity
    public void init(Context context, int headerId) {
        this.context = context.getApplicationContext();    // TODO alternative for background task?
        this.headerId = headerId;
        bufferOf2 = new Buffer[2];
        bufferPointer = 0;
        unitsPointer = 0;
        bufferBackupPending = null;
        bufferOf2[bufferPointer] = new Buffer();
    }

    public synchronized void add(FraaStreamDataUnit unit) {
        bufferOf2[bufferPointer].units[unitsPointer++] = unit;
        if (unitsPointer == LENGTH_OF_BUFFER) {
            Log.d(StreamingActivity.TAG, "Recreating singleton buffer");
            toggleBuffer();
            insertIntoDatabase();
        }
    }

    private void toggleBuffer() {
        //Log.d(StreamingActivity.TAG, "toggle");
        if (bufferBackupPending != null) {
            throw new RuntimeException("Trying to toggle buffer when data has not been backed-up yet");
        }
        bufferBackupPending = bufferPointer;
        bufferPointer = (bufferPointer == 0) ? 1 : 0;
        bufferOf2[bufferPointer] = new Buffer();
        unitsPointer = 0;
        //Log.d(StreamingActivity.TAG, "end toggle");
    }

    // careful with transactions
    private void insertIntoDatabase() {
        // open ddbb connection only once
        FraaDbHelper fraaDbHelper = new FraaDbHelper(context);  // TODO how to create a context in a background task?
        db = fraaDbHelper.getWritableDatabase();
        db.beginTransaction();
        Log.d(StreamingActivity.TAG, "transaction opened");
        Log.d(StreamingActivity.TAG, "buffer to copy " + bufferBackupPending);

        try {
            //int count = 0;
            for (FraaStreamDataUnit unit : bufferOf2[bufferBackupPending].units) {
                //Log.d(StreamingActivity.TAG, "count:" + count++);
                ContentValues values = new ContentValues();
                values.put(AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID, getHeaderId());
                values.put(AccDataContract.AccDataEntry.COLUMN_NAME_INDEX, unit.getIndex());
                values.put(AccDataContract.AccDataEntry.COLUMN_NAME_X, unit.getX());
                values.put(AccDataContract.AccDataEntry.COLUMN_NAME_Y, unit.getY());
                values.put(AccDataContract.AccDataEntry.COLUMN_NAME_Z, unit.getZ());
                db.insert(AccDataContract.AccDataEntry.TABLE_NAME,
                        null,
                        values);
            }
        } catch (Exception e) {
            // TODO !!!
            Log.i(StreamingActivity.TAG, "exception", e);
        }

        db.endTransaction();
        bufferBackupPending = null;
        Log.d(StreamingActivity.TAG, "transaction closed");
    }

    public int getHeaderId() {
        return headerId;
    }

    public void setHeaderId(int headerId) {
        this.headerId = headerId;
    }
}
