package cc.teil.sisyphus.mru.mobiletelemetry.Parcels;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mru on 09.01.16.
 */
public class MeasurementParcel implements Parcelable {


    public static final Creator<MeasurementParcel> CREATOR = new Creator<MeasurementParcel>() {
        @Override
        public MeasurementParcel createFromParcel(Parcel in) {
            return new MeasurementParcel(in);
        }

        @Override
        public MeasurementParcel[] newArray(int size) {
            return new MeasurementParcel[size];
        }
    };
    private double[] voltage;
    private double current;
    private double acc_x;
    private double acc_y;
    private double acc_z;
    private double gyr_x;
    private double gyr_y;
    private double gyr_z;

    protected MeasurementParcel(Parcel in) {
        voltage = in.createDoubleArray();
        current = in.readDouble();
        acc_x = in.readDouble();
        acc_y = in.readDouble();
        acc_z = in.readDouble();
        gyr_y = in.readDouble();
        gyr_x = in.readDouble();
        gyr_z = in.readDouble();
    }

    public MeasurementParcel() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDoubleArray(voltage);
        dest.writeDouble(current);
        dest.writeDouble(acc_x);
        dest.writeDouble(acc_y);
        dest.writeDouble(acc_z);
        dest.writeDouble(gyr_x);
        dest.writeDouble(gyr_y);
        dest.writeDouble(gyr_z);
    }

    public double getVoltage(int index) {
        return voltage[index];
    }

//    public void setVoltage(double[] voltage) {
    //      this.voltage = voltage;
    //}

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getAcc(int index) {
        switch(index){
            case 0: return acc_x;
            case 1: return acc_y;
            case 2: return acc_z;
        }
        return Double.NaN;
    }

    public double getAcc_x() {
        return acc_x;
    }

    public void setAcc_x(double acc_x) {
        this.acc_x = acc_x;
    }

    public double getAcc_y() {
        return acc_y;
    }

    public void setAcc_y(double acc_y) {
        this.acc_y = acc_y;
    }

    public double getAcc_z() {
        return acc_z;
    }

    public void setAcc_z(double acc_z) {
        this.acc_z = acc_z;
    }

    public void setVoltage(double[] voltage) {
        this.voltage = voltage;
    }

    public Number getGyro(int index) {
        switch(index){
            case 0: return gyr_x;
            case 1: return gyr_y;
            case 2: return gyr_z;
        }
        return Double.NaN;
    }
}
