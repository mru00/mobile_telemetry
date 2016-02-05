// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

package cc.teil.sisyphus.mru.mobiletelemetry.Visualization;

import android.os.Parcel;
import android.util.Log;

import com.androidplot.xy.XYSeries;

/**
 * Created by mru on 10.01.16.
 */

public class MeasurementSeries extends AbstractFifoSeries<MeasurementPlotDataAdapter> {

    private final static String TAG = MeasurementSeries.class.getSimpleName();
    private final String[] axisNameLookup = new String[] { "X", "Y", "Z" };

    public MeasurementSeries() {
        Log.w(TAG, "constructor");
    }

    public MeasurementSeries(Parcel in) {
        setEntries(in.createTypedArrayList(MeasurementPlotDataAdapter.CREATOR));
    }

    public XYSeries getCellVoltage(final int targetIndex) {
        return new AbstractXYSeries() {
            private int idx = targetIndex;
            private String title = String.format("Cell %d", idx);

            public Number getData(MeasurementPlotDataAdapter data) {
                return data.data.getVoltage(idx);
            }

            @Override
            public String getTitle() { return title; }

            @Override
            public String getUnit() { return "V"; }
        };
    }

    public XYSeries getCurrent() {
        return new AbstractXYSeries() {
            public Number getData(MeasurementPlotDataAdapter data) {
                return data.data.getCurrent();
            }

            @Override
            public String getTitle() {
                return "Total";
            }

            @Override
            public String getUnit() { return "A"; }
        };
    }

    public XYSeries getAccelerometer(final int targetIndex) {

        return new AbstractXYSeries() {
            private final int idx = targetIndex;
            public Number getData(MeasurementPlotDataAdapter data) {
                return data.data.getAcc(targetIndex);
            }

            @Override
            public String getTitle() {
                return axisNameLookup[targetIndex];
            }

            @Override
            public String getUnit() { return "g"; }
        };
    }

    public XYSeries getGyroscope(final int targetIndex) {


        return new AbstractXYSeries() {
            private final int idx = targetIndex;
            public Number getData(MeasurementPlotDataAdapter data) {
                return data.data.getGyro(targetIndex);
            }

            @Override
            public String getTitle() {
                return axisNameLookup[targetIndex];
            }

            @Override
            public String getUnit() { return "deg/sec"; }
        };
    }


    public static final Creator<MeasurementSeries> CREATOR = new Creator<MeasurementSeries>() {
        @Override
        public MeasurementSeries createFromParcel(Parcel in) {
            return new MeasurementSeries(in);
        }

        @Override
        public MeasurementSeries[] newArray(int size) {
            return new MeasurementSeries[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeTypedList(getEntries());
    }
}

