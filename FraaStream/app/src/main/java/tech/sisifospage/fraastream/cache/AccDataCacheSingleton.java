package tech.sisifospage.fraastream.cache;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Semaphore;

import tech.sisifospage.fraastream.bbdd.HeaderContract;
import tech.sisifospage.fraastream.stream.FraaStreamData;
import tech.sisifospage.fraastream.stream.FraaStreamDataUnit;
import tech.sisifospage.fraastream.StreamingActivity;
import tech.sisifospage.fraastream.bbdd.AccDataContract;
import tech.sisifospage.fraastream.bbdd.FraaDbHelper;
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

        // init buffer
        bufferOf2 = new Buffer[2];
        bufferPointer = 0;
        unitsPointer = 0;
        bufferBackupPending = null;
        bufferOf2[bufferPointer] = new Buffer();

        // set next value of headerId for SQLite
        setNewHeaderId(macAddress);

        // service for uploading to server
        Intent serviceIntent = new Intent(context, UpstreamService.class);
        serviceIntent.putExtra(UpstreamService.SQLITE_HEADER_ID, getHeaderId());
        serviceIntent.putExtra(UpstreamService.SQLITE_HEADER_MAC_ADDR, macAddress);
        serviceIntent.putExtra(UpstreamService.SQLITE_HEADER_CREATED_AT, createdAt);
        // TODO label should be passed to UpstreamService, when set
        serviceIntent.putExtra(UpstreamService.SQLITE_HEADER_LABEL, "");
        context.startService(serviceIntent);

    }

    private FraaDbHelper getDbHelperIfAvailable() {
        available.acquireUninterruptibly();
        return new FraaDbHelper(this.context);
    }

    private void release() {
        available.release();
    }

    private void setNewHeaderId(String macAddress) {
        FraaDbHelper fraaDbHelper = getDbHelperIfAvailable();
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


        /*
        // TODO remove this checks
        db = fraaDbHelper.getReadableDatabase();
        Cursor cursor = db.query(HeaderContract.HeaderEntry.TABLE_NAME, new String[]{"MAX(" + HeaderContract.HeaderEntry._ID + ") as max"},
                null, null, null, null, null);
        cursor.moveToFirst();
        Log.d(StreamingActivity.TAG, "Cursor move to 1st");
        int index = cursor.getColumnIndex("max");
        Log.d(StreamingActivity.TAG, "Max id: " + cursor.getString(index));
        db = fraaDbHelper.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, HeaderContract.HeaderEntry.TABLE_NAME,
                null, null);
        Log.i(StreamingActivity.TAG, "Total number of rows inserted " + count);
        */

        /*
        // TODO remove this check
        values = new ContentValues();
        values.put(AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID, getHeaderId());
        values.put(AccDataContract.AccDataEntry.COLUMN_NAME_INDEX, 1);
        values.put(AccDataContract.AccDataEntry.COLUMN_NAME_X, 1);
        values.put(AccDataContract.AccDataEntry.COLUMN_NAME_Y, 1);
        values.put(AccDataContract.AccDataEntry.COLUMN_NAME_Z, 1);
        db.insert(AccDataContract.AccDataEntry.TABLE_NAME,
                null,
                values);
        db = fraaDbHelper.getReadableDatabase();
        count = DatabaseUtils.queryNumEntries(db, AccDataContract.AccDataEntry.TABLE_NAME,
                AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID + "=?", new String[]{String.valueOf(res)});
        String message = "Total number of rows inserted " + count;
        Log.i(StreamingActivity.TAG, message);
        */

        return;
    }


    public void setServerHeaderId(int headerId, int serverHeaderId) {
        FraaDbHelper fraaDbHelper = getDbHelperIfAvailable();
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
        FraaDbHelper fraaDbHelper = getDbHelperIfAvailable();
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
        //Log.d(StreamingActivity.TAG, "transaction closed");
        db.close();
        release();
    }


    public Collection<FraaStreamData> selectRowsHeaderNotEqualto(int headerId) {
        Log.d(StreamingActivity.TAG, "Looking for rows with id different to: " + headerId);
        FraaDbHelper fraaDbHelper = getDbHelperIfAvailable();
        SQLiteDatabase db = fraaDbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + AccDataContract.AccDataEntry.TABLE_NAME
                + " WHERE " + AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID + " != ?"
                + " ORDER BY " + AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID,
                new String[] {String.valueOf(headerId)});

        Collection<FraaStreamData> output = new ArrayList<>();
        Integer currentHeaderId = null;
        FraaStreamData currentData = null;
        if (c != null) {
            Log.d(StreamingActivity.TAG, "Row count: " + c.getCount());
            if (c.moveToFirst()) {
                do {
                    int rowHeaderId = c.getInt(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_HEADER_ID));
                    if (currentHeaderId == null
                            || currentHeaderId != rowHeaderId) {
                        Log.d(StreamingActivity.TAG, "Row header id: " + rowHeaderId);
                        currentHeaderId = rowHeaderId;
                        currentData = new FraaStreamData();
                        currentData.setHeaderId(getServerHeaderId(rowHeaderId, fraaDbHelper));
                        output.add(currentData);
                    }
                    FraaStreamDataUnit unit = new FraaStreamDataUnit();
                    unit.setIndex(c.getInt(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_INDEX)));
                    unit.setX(c.getFloat(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_X)));
                    unit.setY(c.getFloat(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_X)));
                    unit.setZ(c.getFloat(c.getColumnIndex(AccDataContract.AccDataEntry.COLUMN_NAME_X)));
                    currentData.addDataUnit(unit);
                } while (c.moveToNext());
            }
        }
        db.close();
        release();
        return output;
    }

    private int getServerHeaderId(int headerId, FraaDbHelper fraaDbHelper) {
        Integer output = null;
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
        FraaDbHelper fraaDbHelper = getDbHelperIfAvailable();
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
}
