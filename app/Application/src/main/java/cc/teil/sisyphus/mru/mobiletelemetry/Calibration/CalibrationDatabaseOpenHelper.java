package cc.teil.sisyphus.mru.mobiletelemetry.Calibration;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by mru on 18.01.16.
 */
public class CalibrationDatabaseOpenHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "calibration.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_CALIBRATION = "calibration";
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_VALUE_A = "a";
    public static final String COLUMN_VALUE_B = "b";
    public static final String COLUMN_VALUE_C = "c";
    public static final String[] COLUMNS = new String[] {COLUMN_KEY, COLUMN_VALUE_A, COLUMN_VALUE_B, COLUMN_VALUE_C};

    private static final String DATABASE_CREATE = String.format(
                    "create table %s (" +
                    "  %s text primary key," +
                    "  %s double not null," +
                    "  %s double not null," +
                    "  %s double not null" +
                    ")",
            TABLE_CALIBRATION,
            COLUMN_KEY,
            COLUMN_VALUE_A,
            COLUMN_VALUE_B,
            COLUMN_VALUE_C);


    public CalibrationDatabaseOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);

    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {

            ContentValues cv_cell1 = new ContentValues();
            cv_cell1.put(COLUMN_VALUE_A, 0.0);
            cv_cell1.put(COLUMN_VALUE_B, 1.0);
            cv_cell1.put(COLUMN_VALUE_C, 0.0);

            ContentValues cv_cell2 = new ContentValues();
            cv_cell1.put(COLUMN_VALUE_A, 0.0);
            cv_cell1.put(COLUMN_VALUE_B, 1.0);
            cv_cell1.put(COLUMN_VALUE_C, 0.0);

            ContentValues cv_current = new ContentValues();
            cv_cell1.put(COLUMN_VALUE_A, 0.0);
            cv_cell1.put(COLUMN_VALUE_B, 1.0);
            cv_cell1.put(COLUMN_VALUE_C, 0.0);

            db.insertWithOnConflict(TABLE_CALIBRATION, null, cv_cell1, SQLiteDatabase.CONFLICT_IGNORE);
            db.insertWithOnConflict(TABLE_CALIBRATION, null, cv_cell2, SQLiteDatabase.CONFLICT_IGNORE);
            db.insertWithOnConflict(TABLE_CALIBRATION, null, cv_current, SQLiteDatabase.CONFLICT_IGNORE);

        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: we can do better than this!
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALIBRATION);
        onCreate(db);
    }
}
