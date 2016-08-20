package tech.sisifospage.fraastream.cache;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import tech.sisifospage.fraastream.bbdd.HeaderContract;
import tech.sisifospage.fraastream.stream.FraaStreamData;
import tech.sisifospage.fraastream.stream.FraaStreamDataUnit;
import tech.sisifospage.fraastream.StreamingActivity;
import tech.sisifospage.fraastream.bbdd.AccDataContract;
import tech.sisifospage.fraastream.bbdd.FraaDbHelper;
import tech.sisifospage.fraastream.stream.FraaStreamHeader;
import tech.sisifospage.fraastream.stream.UpstreamService;

/**
 * Created by lorenzorubio on 17/7/16.
 */
public class AccDataCacheSingleton {

    private static AccDataCacheSingleton accDataCacheSingleton;

    private final Semaphore available = new Semaphore(1);

    private static int LENGTH_OF_BUFFER = 500;
    public static int NULL_SERVER_HEADER_ID = -1;

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

    private int headerId;
    private long createdAt;
    private String macAddress;
    private String headerLabel; //TODO
    private boolean started;

    public static AccDataCacheSingleton getInstance() {
        if (accDataCacheSingleton == null) {
            Log.d(StreamingActivity.TAG, "Creating cache singleton");
            accDataCacheSingleton = new AccDataCacheSingleton();
        }
        return accDataCacheSingleton;
    }

    // this constructor is only initialized when calling from activity
    public void init(Context context, String macAddress) {
        this.context = context;    // TODO review, expects caller passes this.getBaseContext()
        //this.fraaDbHelper = new FraaDbHelper(this.context);

        setMacAddress(macAddress);

        // init buffer
        bufferOf2 = new Buffer[2];
        bufferPointer = 0;
        unitsPointer = 0;
        bufferBackupPending = null;
        bufferOf2[bufferPointer] = new Buffer();

        // set next value of headerId for SQLite
        setNewHeaderId();

        // service for uploading to server
        Intent serviceIntent = new Intent(context, UpstreamService.class);
        context.startService(serviceIntent);

    }

    private FraaDbHelper getDbHelperWhenAvailable() {
        available.acquireUninterruptibly();
        return new FraaDbHelper(this.context);
    }

    private void release() {
        available.release();
    }

    private void setNewHeaderId() {
        FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
        SQLiteDatabase db = fraaDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        createdAt = System.currentTimeMillis();
        values.put(HeaderContract.HeaderEntry.COLUMN_NAME_CREATED_AT, createdAt);
        values.put(HeaderContract.HeaderEntry.COLUMN_NAME_MAC_ADDRESS, macAddress);
        values.put(HeaderContract.HeaderEntry.COLUMN_NAME_LABEL, "");
        values.put(HeaderContract.HeaderEntry.COLUMN_NAME_SERVER_HEADER_ID, NULL_SERVER_HEADER_ID);
        long res = db.insert(HeaderContract.HeaderEntry.TABLE_NAME, null, values);
        Log.d(StreamingActivity.TAG, "New header id: " + res);
        setHeaderId((int) res);

        db.close();
        release();

        return;
    }

    public FraaStreamHeader getHeader(int headerId) {
        FraaStreamHeader output = null;
        FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
        SQLiteDatabase db = fraaDbHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        Cursor c = db.rawQuery("SELECT * FROM " + HeaderContract.HeaderEntry.TABLE_NAME
                        + " WHERE " + HeaderContract.HeaderEntry._ID + " = ? ",
                new String[] {String.valueOf(headerId)});
        if (c != null) {
            if (c.moveToFirst()) {
                output = new FraaStreamHeader();
                output.setCreatedAt(new Date(c.getInt(c.getColumnIndex(HeaderContract.HeaderEntry.COLUMN_NAME_CREATED_AT))));
                output.setMacAddress(c.getString(c.getColumnIndex(HeaderContract.HeaderEntry.COLUMN_NAME_MAC_ADDRESS)));
                output.setLabel(c.getString(c.getColumnIndex(HeaderContract.HeaderEntry.COLUMN_NAME_LABEL)));
            }
        }

        db.close();
        release();

        return output;
    }

    public void setServerHeaderId(int headerId, int serverHeaderId) {
        FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
        SQLiteDatabase db = fraaDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HeaderContract.HeaderEntry.COLUMN_NAME_SERVER_HEADER_ID, serverHeaderId);
        db.update(HeaderContract.HeaderEntry.TABLE_NAME, values, HeaderContract.HeaderEntry._ID + "=" + headerId , null);
        db.close();
        release();
    }


    public synchronized void add(FraaStreamDataUnit unit) {
        bufferOf2[bufferPointer].units[unitsPointer++] = unit;
        if (unitsPointer == LENGTH_OF_BUFFER) {
            Log.d(StreamingActivity.TAG, "Recreating singleton buffer");
            toggleBuffer();
            new InsertIntoDatabaseTask().execute();
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

    private class InsertIntoDatabaseTask extends AsyncTask<String, Integer, Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
            // open ddbb connection only once
            SQLiteDatabase db = fraaDbHelper.getWritableDatabase();
            //db.beginTransaction();
            //Log.d(StreamingActivity.TAG, "transaction opened");
            Log.d(StreamingActivity.TAG, "buffer to copy " + bufferBackupPending);
            Log.d(StreamingActivity.TAG, "header id:" + getHeaderId());

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

            //db.endTransaction();
            bufferBackupPending = null;
            Log.d(StreamingActivity.TAG, "copy to SQLite ok");
            db.close();
            release();
            return 1;
        }
    }

    public Map<Integer, Collection<FraaStreamDataUnit>> selectRowsHeaderNotEqualTo(int headerId) {
        return selectRows(headerId, " != ");
    }

    public Map<Integer, Collection<FraaStreamDataUnit>> selectRowsHeaderEqualTo(int headerId) {
        return selectRows(headerId, " = ");
    }

    private Map<Integer, Collection<FraaStreamDataUnit>> selectRows(int headerId, String operator) {
        FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
        SQLiteDatabase db = fraaDbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + AccDataContract.AccDataEntry.TABLE_NAME
                + " WHERE " + AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID +  operator + "?"
                + " ORDER BY " + AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID
                + " , " + AccDataContract.AccDataEntry.COLUMN_NAME_INDEX,
                new String[] {String.valueOf(headerId)});

        Map<Integer, Collection<FraaStreamDataUnit>> output = new HashMap<>();
        if (c != null) {
            Log.d(StreamingActivity.TAG, "Row count: " + c.getCount());
            if (c.moveToFirst()) {
                do {
                    int rowHeaderId = c.getInt(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID));
                    FraaStreamDataUnit unit = new FraaStreamDataUnit();
                    unit.setIndex(c.getInt(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_INDEX)));
                    unit.setX(c.getFloat(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_X)));
                    unit.setY(c.getFloat(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_X)));
                    unit.setZ(c.getFloat(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_X)));
                    Collection<FraaStreamDataUnit> list = output.get(rowHeaderId);
                    if (list == null) {
                        list = new ArrayList<>();
                        output.put(rowHeaderId, list);
                    }
                    list.add(unit);
                } while (c.moveToNext());
            }
        }
        db.close();
        release();
        return output;
    }

    public int getServerHeaderId(int headerId) {
        Integer output = null;
        FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
        SQLiteDatabase db = fraaDbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + HeaderContract.HeaderEntry.TABLE_NAME
                + " WHERE " + HeaderContract.HeaderEntry._ID + " = ? ",
                new String[] {String.valueOf(headerId)});
        if (c != null) {
            if (c.moveToFirst()) {
                output = c.getInt(c.getColumnIndex(HeaderContract.HeaderEntry.COLUMN_NAME_SERVER_HEADER_ID));
            }
        }
        if (output == NULL_SERVER_HEADER_ID) {
            Log.d(StreamingActivity.TAG, "Header Id " + headerId + " has no server header id yet");
        }
        db.close();
        release();
        return output;
    }


    public void removeFromDatabase(FraaStreamData data) {
        FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
        SQLiteDatabase db = fraaDbHelper.getWritableDatabase();
        //db.beginTransaction();
        for (FraaStreamDataUnit unit : data.getDataUnits()) {
            // TODO result count is 0! try with _id in (list)
            int count = db.delete(AccDataContract.AccDataEntry.TABLE_NAME,
                    AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID + "=? AND " + AccDataContract.AccDataEntry.COLUMN_NAME_INDEX + "=?",
                    new String[] {String.valueOf(data.getHeaderId()), String.valueOf(unit.getIndex())} );
            //Log.d(StreamingActivity.TAG, "Delete (" + String.valueOf(data.getHeaderId()) + ", " + String.valueOf(unit.getIndex()) +") result:" + count);
        }
        //db.rawQuery("DELETE FROM " + AccDataContract.AccDataEntry.TABLE_NAME + " WHERE header_id=156",
        //        null );
        //Log.d(StreamingActivity.TAG, "Delete (" + String.valueOf(data.getHeaderId()) + ") result:");
        //int count2 = db.delete(AccDataContract.AccDataEntry.TABLE_NAME,
        //        null,
        //        null);
        //Log.d(StreamingActivity.TAG, "Delete (all) result:" + count2);
        //db.endTransaction();
        db.close();
        release();
    }


    public int getHeaderId() {
        return headerId;
    }

    public void setHeaderId(int headerId) {
        this.headerId = headerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getHeaderLabel() {
        return headerLabel;
    }

    public void setHeaderLabel(String headerLabel) {
        this.headerLabel = headerLabel;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }
}
