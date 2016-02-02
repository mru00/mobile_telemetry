package cc.teil.sisyphus.mru.mobiletelemetry.Visualization;

import android.os.Parcel;

import com.androidplot.xy.XYSeries;

/**
 * Created by mru on 10.01.16.
 */

public class RssiHistorySeries extends AbstractFifoSeries<RssiPlotDataAdapter> {

    public RssiHistorySeries() {
        super();
    }

    public RssiHistorySeries(Parcel in) {
        setEntries(in.createTypedArrayList(RssiPlotDataAdapter.CREATOR));
    }

    public XYSeries getRssi() {
        return new AbstractXYSeries() {
            @Override
            public Number getData(RssiPlotDataAdapter data) {
                return data.rssi;
            }

            @Override
            public String getUnit() {
                return null;
            }

            @Override
            public String getTitle() {
                return "Rssi";
            }
        };
    }

    public static final Creator<RssiHistorySeries> CREATOR = new Creator<RssiHistorySeries>() {
        @Override
        public RssiHistorySeries createFromParcel(Parcel in) {
            return new RssiHistorySeries(in);
        }

        @Override
        public RssiHistorySeries[] newArray(int size) {
            return new RssiHistorySeries[size];
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

