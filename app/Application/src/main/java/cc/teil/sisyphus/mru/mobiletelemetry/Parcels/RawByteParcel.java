// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

package cc.teil.sisyphus.mru.mobiletelemetry.Parcels;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mru on 21.12.15.
 */
public class RawByteParcel implements Parcelable {

    public static final Creator<RawByteParcel> CREATOR = new Creator<RawByteParcel>() {
        @Override
        public RawByteParcel createFromParcel(Parcel in) {
            return new RawByteParcel(in);
        }

        @Override
        public RawByteParcel[] newArray(int size) {
            return new RawByteParcel[size];
        }
    };
    private byte[] data;

    public RawByteParcel(byte[] data) {
        this.data = data;
    }

    protected RawByteParcel(Parcel in) {
        data = in.createByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(data);
    }
}
