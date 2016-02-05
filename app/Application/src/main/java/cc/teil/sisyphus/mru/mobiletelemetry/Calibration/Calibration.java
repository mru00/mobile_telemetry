// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

package cc.teil.sisyphus.mru.mobiletelemetry.Calibration;

import android.content.ContentValues;

/**
 * Created by mru on 18.01.16.
 */
public class Calibration {
    public double a;
    public double b;
    public double c;

    public Calibration(double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.put(CalibrationDatabaseOpenHelper.COLUMN_VALUE_A, a);
        cv.put(CalibrationDatabaseOpenHelper.COLUMN_VALUE_B, b);
        cv.put(CalibrationDatabaseOpenHelper.COLUMN_VALUE_C, c);
        return cv;
    }

    public double evaluate(double x) {
        return a + b * x + c * x * x;
    }
}
