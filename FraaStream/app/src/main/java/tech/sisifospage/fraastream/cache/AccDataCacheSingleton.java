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
import java.util.UUID;
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

    private static final String TAG = "MetaWear.Singleton";

    private static AccDataCacheSingleton accDataCacheSingleton;

    private final Semaphore available = new Semaphore(1);

    private static int LENGTH_OF_BUFFER = 500;
    private static Integer REMOVE_CHUNK_SIZE = 250;

    public static int NULL_SERVER_HEADER_ID = -1;

    private static int TOGGLING_BUFFER_SIZE = 6;

    private static class Buffer {
        public FraaStreamDataUnit[] units;

        public Buffer() {
            this.units = new FraaStreamDataUnit[LENGTH_OF_BUFFER];
        }
    }
    private Buffer[] togglingBuffer;
    private int bufferPointer;
    private int unitsPointer;
    private Map<Integer, UUID> bufferBackupPending;

    // context from activity
    private Context context;

    private int headerId;
    private long createdAt;
    private String macAddress;
    private String headerLabel; //TODO
    private boolean started;

    // from activity
    // TODO review, expects caller passes this.getBaseContext()
    public synchronized static AccDataCacheSingleton getInstance(Context context) {
        if (accDataCacheSingleton == null && context != null) {
            Log.d(TAG, "Creating cache singleton");
            accDataCacheSingleton = new AccDataCacheSingleton();
            accDataCacheSingleton.context = context;
            accDataCacheSingleton.initBuffer();
        }
        return accDataCacheSingleton;
    }

    // from service
    public static AccDataCacheSingleton getInstance() {
        return getInstance(null);
    }


    private void initBuffer() {
        togglingBuffer = new Buffer[TOGGLING_BUFFER_SIZE];
        bufferPointer = 0;
        unitsPointer = 0;
        bufferBackupPending = new HashMap<>();
        togglingBuffer[accDataCacheSingleton.bufferPointer] = new Buffer();
    }

    // only called from activity
    public void start(String macAddress) {
        Log.d(TAG, "Start MAC " + macAddress + " and create new header/service");
        setMacAddress(macAddress);

        initBuffer();

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
        Log.d(TAG, "New header id: " + res);
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
        if (unitsPointer == LENGTH_OF_BUFFER) {
            Log.e(TAG, "Error, index has been skipped!!!");
            toggleBuffer();
        }
        togglingBuffer[bufferPointer].units[unitsPointer++] = unit;
        if (unitsPointer == LENGTH_OF_BUFFER) {
            Log.d(TAG, "Recreating singleton buffer");
            UUID id = toggleBuffer();
            new InsertIntoDatabaseTask().execute(id.toString());
        }
    }

    private UUID toggleBuffer() {
        //Log.d(StreamingActivity.TAG, "toggle");
        if (bufferBackupPending.get(bufferPointer) != null) {
            Log.e(TAG, "Data would get lost in this case... consider adding a semaphore?");
        }
        UUID id = UUID.randomUUID();
        // use database semaphore...
        getDbHelperWhenAvailable();
        bufferBackupPending.put(bufferPointer++, id);
        release();
        bufferPointer = (bufferPointer == TOGGLING_BUFFER_SIZE) ? 0 : bufferPointer;
        togglingBuffer[bufferPointer] = new Buffer();
        unitsPointer = 0;
        return id;
    }

    private class InsertIntoDatabaseTask extends AsyncTask<String, Integer, Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            UUID id = UUID.fromString(params[0]);
            Integer bufferToBackup = null;
            FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
            SQLiteDatabase db = fraaDbHelper.getWritableDatabase();
            for (Integer key : bufferBackupPending.keySet()) {
                UUID candidate = bufferBackupPending.get(key);
                if (candidate != null && candidate.equals(id)) {
                    bufferToBackup = key;
                }
            }
            if (bufferToBackup == null) {
                Log.e(TAG, "Data got lost... Don't find " + id.toString() + " anymore");
            }
            //db.beginTransaction();
            //Log.d(StreamingActivity.TAG, "transaction opened");
            //Log.d(TAG, "buffer to copy " + bufferToBackup);
            //Log.d(TAG, "header id:" + getHeaderId());

            for (FraaStreamDataUnit unit : togglingBuffer[bufferToBackup].units) {
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
            bufferBackupPending.put(bufferToBackup, null);
            Log.d(TAG, "copy to SQLite ok (" + bufferToBackup + ")");
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
            //Log.d(TAG, "Row count: " + c.getCount());
            if (c.moveToFirst()) {
                do {
                    int rowHeaderId = c.getInt(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID));
                    FraaStreamDataUnit unit = new FraaStreamDataUnit();
                    unit.setIndex(c.getInt(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_INDEX)));
                    unit.setX(c.getFloat(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_X)));
                    unit.setY(c.getFloat(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_Y)));
                    unit.setZ(c.getFloat(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_Z)));
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
            Log.d(TAG, "Header Id " + headerId + " has no server header id yet");
        }
        db.close();
        release();
        return output;
    }

    public int getLocalHeaderId(int serverHeaderId) {
        Integer output = null;
        FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
        SQLiteDatabase db = fraaDbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + HeaderContract.HeaderEntry.TABLE_NAME
                        + " WHERE " + HeaderContract.HeaderEntry.COLUMN_NAME_SERVER_HEADER_ID + " = ? ",
                new String[] {String.valueOf(serverHeaderId)});
        if (c != null) {
            if (c.moveToFirst()) {
                output = c.getInt(c.getColumnIndex(HeaderContract.HeaderEntry._ID));
            }
        }
        db.close();
        release();
        return output;
    }

    public void removeFromDatabase(FraaStreamData data) {
        // this is an access, has to be done before blocking the semaphore for removing
        String localServerId = String.valueOf(getLocalHeaderId(data.getHeaderId()));

        // TODO maybe bigger chunk size but block only while deleting?
        FraaDbHelper fraaDbHelper = getDbHelperWhenAvailable();
        SQLiteDatabase db = fraaDbHelper.getWritableDatabase();

        String[] indexArray = getIndexArray(data.getDataUnits());
        if (indexArray.length % REMOVE_CHUNK_SIZE != 0) {
            Log.e(TAG, "Packet size is not a multiple of " + REMOVE_CHUNK_SIZE + "??!!??? (" + indexArray.length + ")");
        }
        for (int queryNum = 1; queryNum <= indexArray.length / REMOVE_CHUNK_SIZE; queryNum++) {
            // divide the query in groups of REMOVE_CHUNK_SIZE indexes
            String[] params = new String[REMOVE_CHUNK_SIZE + 1];
            params[0] = localServerId;
            System.arraycopy(indexArray, (queryNum - 1)*REMOVE_CHUNK_SIZE, params, 1, REMOVE_CHUNK_SIZE);
            int count = db.delete(AccDataContract.AccDataEntry.TABLE_NAME,
                    AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID + "=? AND " +
                            AccDataContract.AccDataEntry.COLUMN_NAME_INDEX + " IN (" + new String(new char[REMOVE_CHUNK_SIZE-1]).replace("\0", "?,") + "?)",
                    params);
            Log.d(TAG, "Delete from " + localServerId + " this many indexes:" + count);
        }
        db.close();
        release();
    }

    public String[] getIndexArray(FraaStreamDataUnit[] units) {
        String[] output = new String[units.length];
        int index = 0;
        for (FraaStreamDataUnit unit : units) {
            output[index++] = unit.getIndex().toString();
        }
        return output;
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
