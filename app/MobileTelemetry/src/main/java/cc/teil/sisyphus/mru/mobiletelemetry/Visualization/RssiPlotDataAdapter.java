// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

package cc.teil.sisyphus.mru.mobiletelemetry.Visualization;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mru on 15.01.16.
 */

public class RssiPlotDataAdapter extends AbstractPlotDataAdapter implements Parcelable {
    public int rssi;

    public RssiPlotDataAdapter(int rssi) {
        super();
        this.rssi = rssi;
    }

    protected RssiPlotDataAdapter(Parcel in) {
        timeStampMilli = in.readDouble();
        rssi = in.readInt();
    }

    public static final Creator<RssiPlotDataAdapter> CREATOR = new Creator<RssiPlotDataAdapter>() {
        @Override
        public RssiPlotDataAdapter createFromParcel(Parcel in) {
            return new RssiPlotDataAdapter(in);
        }

        @Override
        public RssiPlotDataAdapter[] newArray(int size) {
            return new RssiPlotDataAdapter[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(timeStampMilli);
        dest.writeInt(rssi);
    }
}
