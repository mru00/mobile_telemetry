package cc.teil.sisyphus.mru.mobiletelemetry.Parcels;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mru on 09.01.16.
 */
public class ConfigParcel implements Parcelable {

    public static final Creator<ConfigParcel> CREATOR = new Creator<ConfigParcel>() {
        @Override
        public ConfigParcel createFromParcel(Parcel in) {
            return new ConfigParcel(in);
        }

        @Override
        public ConfigParcel[] newArray(int size) {
            return new ConfigParcel[size];
        }
    };

    private int cellCount;
    private int version;

    public boolean getHasAccelerometer() {
        return hasAccelerometer;
    }

    public void setHasAccelerometer(boolean hasAccelerometer) {
        this.hasAccelerometer = hasAccelerometer;
    }

    private boolean hasAccelerometer;

    public boolean getHasGyroscope() {
        return hasGyroscope;
    }

    public void setHasGyroscope(boolean hasGyroscope) {
        this.hasGyroscope = hasGyroscope;
    }

    private boolean hasGyroscope;

    protected ConfigParcel(Parcel in) {
        version = in.readInt();
        cellCount = in.readInt();
        hasAccelerometer = in.readInt() == 1;
        hasGyroscope = in.readInt() == 1;
    }

    public ConfigParcel() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(version);
        dest.writeInt(cellCount);
        dest.writeInt(hasAccelerometer?1:0);
        dest.writeInt(hasGyroscope?1:0);
    }

    public int getCellCount() {
        return cellCount;
    }

    public void setCellCount(int nCells) {
        this.cellCount = nCells;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
