// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

package cc.teil.sisyphus.mru.mobiletelemetry.Visualization;

import android.os.Parcel;
import android.os.Parcelable;

import cc.teil.sisyphus.mru.mobiletelemetry.Parcels.MeasurementParcel;

/**
 * Created by mru on 15.01.16.
 */
public class MeasurementPlotDataAdapter extends AbstractPlotDataAdapter implements Parcelable {
    MeasurementParcel data;

    public MeasurementPlotDataAdapter(MeasurementParcel data) {
        super();
        this.data = data;
    }

    protected MeasurementPlotDataAdapter(Parcel in) {
        timeStampMilli = in.readDouble();
        data = in.readParcelable(MeasurementParcel.class.getClassLoader());
    }

    public static final Creator<MeasurementPlotDataAdapter> CREATOR = new Creator<MeasurementPlotDataAdapter>() {
        @Override
        public MeasurementPlotDataAdapter createFromParcel(Parcel in) {
            return new MeasurementPlotDataAdapter(in);
        }

        @Override
        public MeasurementPlotDataAdapter[] newArray(int size) {
            return new MeasurementPlotDataAdapter[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(timeStampMilli);
        dest.writeParcelable(data, flags);
    }
}
