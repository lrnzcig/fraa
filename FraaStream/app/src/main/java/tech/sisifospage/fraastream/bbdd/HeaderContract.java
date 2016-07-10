package tech.sisifospage.fraastream.bbdd;

import android.provider.BaseColumns;

/**
 * Created by lorenzorubio on 10/7/16.
 */
public class HeaderContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public HeaderContract() {}

    /* Inner class that defines the table contents */
    public static abstract class HeaderEntry implements BaseColumns {
        public static final String TABLE_NAME = "header";
        //public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_CREATED_AT = "created_at";
        public static final String COLUMN_NAME_MAC_ADDRESS = "mac_address";
        public static final String COLUMN_NAME_LABEL = "label";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + HeaderEntry.TABLE_NAME + " (" +
                    HeaderEntry._ID + " INTEGER PRIMARY KEY" +
                    BaseContract.COMMA_SEP + HeaderEntry.COLUMN_NAME_CREATED_AT + BaseContract.DATE_TYPE +
                    BaseContract.COMMA_SEP + HeaderEntry.COLUMN_NAME_MAC_ADDRESS + BaseContract.TEXT_TYPE +
                    BaseContract.COMMA_SEP + HeaderEntry.COLUMN_NAME_LABEL + BaseContract.TEXT_TYPE +
            " )";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + HeaderEntry.TABLE_NAME;
}
