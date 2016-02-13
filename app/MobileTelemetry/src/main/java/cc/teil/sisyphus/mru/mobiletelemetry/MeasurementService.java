// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.teil.sisyphus.mru.mobiletelemetry;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cc.teil.sisyphus.mru.mobiletelemetry.Activities.ViewDataActivity;
import cc.teil.sisyphus.mru.mobiletelemetry.Parcels.MeasurementParcel;
import cc.teil.sisyphus.mru.mobiletelemetry.Parcels.RawByteParcel;
import cc.teil.sisyphus.mru.mobiletelemetry.Protocol.DecodeCharacteristic;
import cc.teil.sisyphus.mru.mobiletelemetry.Visualization.MeasurementPlotDataAdapter;
import cc.teil.sisyphus.mru.mobiletelemetry.Visualization.MeasurementSeries;
import cc.teil.sisyphus.mru.mobiletelemetry.Visualization.RssiHistorySeries;
import cc.teil.sisyphus.mru.mobiletelemetry.Visualization.RssiPlotDataAdapter;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class MeasurementService extends Service {

    public final static String packagename = MeasurementService.class.getPackage().getName();
    public final static String ACTION_GATT_CONNECTED = packagename + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = packagename + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = packagename + ".ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = packagename + ".ACTION_DATA_AVAILABLE";
    public final static String ACTION_RSSI_UPDATE = packagename + ".ACTION_RSSI_UPDATE";
    public final static String EXTRA_DATA = packagename + ".EXTRA_DATA";
    public final static String EXTRA_MEASUREMENT_SERIES = packagename + ".EXTRA_MEASUREMENT_SERIES";
    public final static String EXTRA_RSSI_SERIES = packagename + ".EXTRA_RSSI_SERIES";
    public final static String EXTRA_UUID = packagename + ".EXTRA_UUID";

    private final static String TAG = MeasurementService.class.getSimpleName();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private final IBinder mBinder = new LocalBinder();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private boolean readCharacteristicInProgress = false;
    private int notificationId = 101;


    private MeasurementSeries measurementSeries = new MeasurementSeries();
    private RssiHistorySeries rssiHistorySeries = new RssiHistorySeries();

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;

                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");

                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:");
                if (!mBluetoothGatt.discoverServices()) {
                    Log.e(TAG, "failed to start discovery");
                }
                readCharacteristicInProgress = false;

                updateNotification("Connected");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;

                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);

                updateNotification("Disconnected");

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                final BluetoothGattService service = gatt.getService(UUIDs.RCMON_SERVICE_UUID);

                if (service == null) {
                    Toast.makeText(MeasurementService.this, "Failed to get service", Toast.LENGTH_SHORT).show();
                } else {
                    final BluetoothGattCharacteristic configCharacteristic = service.getCharacteristic(UUIDs.RCMON_CHAR_CONFIG_UUID);

                    if (configCharacteristic == null) {
                        Toast.makeText(MeasurementService.this, "Failed to read config characteristic", Toast.LENGTH_SHORT).show();
                    } else {
                        readCharacteristic(configCharacteristic);
                    }

                    dataCharacteristic = service.getCharacteristic(UUIDs.RCMON_CHAR_MEASUREMENT_UUID);

                    if (dataCharacteristic == null) {
                        Toast.makeText(MeasurementService.this, "Failed to read data characteristic", Toast.LENGTH_SHORT).show();
                    }
                }


                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else {
                Log.w(TAG, "readCharacteristic error");
            }
            readCharacteristicInProgress = false;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            readCharacteristicInProgress = false;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                rssiHistorySeries.add(new RssiPlotDataAdapter(rssi));
                final Intent intent = new Intent(ACTION_RSSI_UPDATE);
                intent.putExtra("rssi", rssi);
                intent.putExtra(EXTRA_RSSI_SERIES, rssiHistorySeries);
                sendBroadcast(intent);
            } else {
                Log.w(TAG, "failed to read rssi");
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());

        if (UUIDs.isConfigCharacteristic(characteristic)) {
            Log.w(TAG, "received config");
            intent.putExtra(EXTRA_DATA, DecodeCharacteristic.decodeConfig(characteristic));
        } else if (UUIDs.isMeasurementCharacteristic(characteristic)) {
            final MeasurementParcel data = DecodeCharacteristic.decodeMeasurement(characteristic);
            measurementSeries.add(new MeasurementPlotDataAdapter(data));
            intent.putExtra(EXTRA_DATA, data);
            intent.putExtra(EXTRA_MEASUREMENT_SERIES, measurementSeries);
        } else {
            intent.putExtra(EXTRA_DATA, new RawByteParcel(characteristic.getValue()));
        }
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }


    private final ScheduledExecutorService executor_ = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        this.executor_.scheduleWithFixedDelay(fetchDataTimer, 1500L, 500L, TimeUnit.MILLISECONDS);

        updateNotification("Started");
    }

    private void updateNotification(String text) {

        final Intent notificationIntent = new Intent(this, ViewDataActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        final NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentIntent(intent)
                        .setContentTitle("MobileTelemetry")
                        .setContentText(text);

        final Notification notification = mBuilder.build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL
                        | Notification.FLAG_ONGOING_EVENT
                        | Notification.FLAG_NO_CLEAR;

        getNotificationManager().notify(notificationId, notification);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    private static boolean dataOrRssi = true;
    private BluetoothGattCharacteristic dataCharacteristic;
    private final Runnable fetchDataTimer = new Runnable() {
        @Override
        public void run() {
            if (dataCharacteristic == null) return;
            if (readCharacteristicInProgress) return;

            if (dataOrRssi) {
                if (!readCharacteristic(dataCharacteristic)) {
                    Log.w(TAG, "failed to read characteristic");
                }
            } else {
                readCharacteristicInProgress = true;
                mBluetoothGatt.readRemoteRssi();
            }
            dataOrRssi = !dataOrRssi;
        }
    };

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        if (characteristic == null) {
            throw new RuntimeException("ch = null");
        }

        if (readCharacteristicInProgress) {
            Log.w(TAG, "readCharacteristic in progress; discarding request");
            return false;
        }


        readCharacteristicInProgress = true;
        return mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        /*
        if (UUIDs.RCMON_CHAR_V_CELL_ARR.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUIDs.CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getNotificationManager().cancel(notificationId);
    }

    public class LocalBinder extends Binder {
        public MeasurementService getService() {
            return MeasurementService.this;
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
