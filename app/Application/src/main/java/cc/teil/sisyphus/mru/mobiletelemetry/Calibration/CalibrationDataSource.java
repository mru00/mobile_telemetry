package cc.teil.sisyphus.mru.mobiletelemetry.Calibration;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;

/**
 * Created by mru on 18.01.16.
 */
public class CalibrationDataSource {
    private final CalibrationDatabaseOpenHelper dbHelper;

    public CalibrationDataSource(Context context) {
        dbHelper = new CalibrationDatabaseOpenHelper(context);
    }

    public void open() throws SQLException {
    }

    public void setCalibration(String key, Calibration data) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.update(CalibrationDatabaseOpenHelper.TABLE_CALIBRATION,
                data.getContentValues(),
                String.format("%s = ?", CalibrationDatabaseOpenHelper.COLUMN_KEY),
                new String[]{key});
        db.close();
    }

    public Calibration getCalibration(String key) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(CalibrationDatabaseOpenHelper.TABLE_CALIBRATION,
                CalibrationDatabaseOpenHelper.COLUMNS,
                String.format("%s = '?'", CalibrationDatabaseOpenHelper.COLUMN_KEY),
                new String[] {key},
                null, null, null, null);

        // TODO: is this ok? close before c.get()?
        db.close();
        return new Calibration(c.getDouble(1), c.getDouble(2), c.getDouble(3));
    }


}
