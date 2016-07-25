package tech.sisifospage.fraastream.bbdd;

import android.provider.BaseColumns;

/**
 * Created by lorenzorubio on 10/7/16.
 */
public class AccDataContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public AccDataContract() {}

    /* Inner class that defines the table contents */
    public static abstract class AccDataEntry implements BaseColumns {
        public static final String TABLE_NAME = "acc_data";
        public static final String COLUMN_NAME_HEADER_ID = "header_id";
        public static final String COLUMN_NAME_INDEX = "acc_index";
        public static final String COLUMN_NAME_X = "x";
        public static final String COLUMN_NAME_Y = "y";
        public static final String COLUMN_NAME_Z = "z";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + AccDataEntry.TABLE_NAME + " (" +
                    AccDataEntry._ID + " INTEGER PRIMARY KEY" +
                    BaseContract.COMMA_SEP + AccDataEntry.COLUMN_NAME_HEADER_ID + BaseContract.INTEGER_TYPE +
                    BaseContract.COMMA_SEP + AccDataEntry.COLUMN_NAME_INDEX + BaseContract.INTEGER_TYPE +
                    BaseContract.COMMA_SEP + AccDataEntry.COLUMN_NAME_X + BaseContract.REAL_TYPE +
                    BaseContract.COMMA_SEP + AccDataEntry.COLUMN_NAME_Y + BaseContract.REAL_TYPE +
                    BaseContract.COMMA_SEP + AccDataEntry.COLUMN_NAME_Z + BaseContract.REAL_TYPE +
            " )";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AccDataEntry.TABLE_NAME;
}
