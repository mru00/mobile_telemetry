// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

package cc.teil.sisyphus.mru.mobiletelemetry.Protocol;

import android.bluetooth.BluetoothGattCharacteristic;

import cc.teil.sisyphus.mru.mobiletelemetry.Parcels.ConfigParcel;
import cc.teil.sisyphus.mru.mobiletelemetry.Parcels.MeasurementParcel;


/*
// from the ble source:

typedef struct __attribute__((__packed__)){
    uint16_t vcell[MAX_NUMBER_OF_CELLS];
    uint16_t current;
    uint16_t acc_x;
    uint16_t acc_y;
    uint16_t acc_z;
    uint16_t gyr_x;
    uint16_t gyr_y;
    uint16_t gyr_z;
} ble_rcmon_data_t;

typedef struct __attribute__((__packed__)){
    uint8_t version;
    uint8_t num_cells;
    uint8_t has_accelerometer;
    uint8_t has_gyroscope;
} ble_rcmon_config_t;

*/


/**
 * Created by mru on 16.01.16.
 */
public class DecodeCharacteristic {

    private static double scaleAcc(int in) {
        return (double) in / (double) ((2 << 12) - 1);
    }

    private static double scaleAdc(int in, double pga) {
        return 4.096 * (double) in / ((double) ((2 << 14) - 1) * pga);
    }

    private static double voltToAmpere(double shuntVoltage) {

        // http://www.ti.com/lit/ds/sbos181d/sbos181d.pdf
        double rl = 110e3;
        double rs = 0.25e-3;

        //VOUT = (IS) (RS) (1000µA/V) (RL)
        //IS = (VOUT) / (RS) (1mA/V) (RL)
        //IS (1mA/V) = (VOUT) / (RS) (RL)
        return shuntVoltage / (rs * rl * 1e-3);
    }

    public static MeasurementParcel decodeMeasurement(BluetoothGattCharacteristic characteristic) {
        MeasurementParcel data = new MeasurementParcel();

        final int FORMAT_SINT16 =  BluetoothGattCharacteristic.FORMAT_SINT16;

        final int cell1 = characteristic.getIntValue(FORMAT_SINT16, 0);
        final int cell2 = characteristic.getIntValue(FORMAT_SINT16, 2);
        final int cell3 = characteristic.getIntValue(FORMAT_SINT16, 4);
        final int current = characteristic.getIntValue(FORMAT_SINT16, 6);
        final int acc_x = characteristic.getIntValue(FORMAT_SINT16, 8);
        final int acc_y = characteristic.getIntValue(FORMAT_SINT16, 10);
        final int acc_z = characteristic.getIntValue(FORMAT_SINT16, 12);

        double voltageDivider = 3.;
        data.setVoltage(new double[]{voltageDivider * scaleAdc(cell1, 1), voltageDivider * scaleAdc(cell2, 1)});
        data.setCurrent(voltToAmpere(scaleAdc(current, 16)));
        data.setAcc_x(scaleAcc(acc_x));
        data.setAcc_y(scaleAcc(acc_y));
        data.setAcc_z(scaleAcc(acc_z));

        return data;
    }

    public static ConfigParcel decodeConfig(BluetoothGattCharacteristic characteristic) {

        final int version = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        final int cellCount = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
        final int has_accelerometer = 1 + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2);

        ConfigParcel data = new ConfigParcel();
        data.setVersion(version);
        data.setCellCount(cellCount);
        data.setHasAccelerometer(has_accelerometer != 0);

        return data;
    }
}
