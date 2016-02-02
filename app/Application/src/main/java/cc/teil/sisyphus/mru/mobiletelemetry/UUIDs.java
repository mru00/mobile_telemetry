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

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class UUIDs {
    //private static HashMap<UUID, String> attributes = new HashMap<>();

//    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //public static final UUID MANUFACTURER_NAME_UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    //public static final UUID DEVICE_INFORMATION_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    public static final UUID RCMON_SERVICE_UUID = UUID.fromString("00002523-1212-efde-1523-785feabcfeca");

    public static final UUID RCMON_CHAR_MEASUREMENT_UUID = UUID.fromString("00002530-1212-efde-1523-785feabcfeca");
    public static final UUID RCMON_CHAR_CONFIG_UUID = UUID.fromString("00002531-1212-efde-1523-785feabcfeca");


    public static boolean isConfigCharacteristic(BluetoothGattCharacteristic characteristic) {
        return characteristic.getUuid().equals(RCMON_CHAR_CONFIG_UUID);
    }

    public static boolean isMeasurementCharacteristic(BluetoothGattCharacteristic characteristic) {
        return characteristic.getUuid().equals(RCMON_CHAR_MEASUREMENT_UUID);
    }


/*
    static {
        attributes.put(DEVICE_INFORMATION_SERVICE_UUID, "Device Information Service");
        attributes.put(MANUFACTURER_NAME_UUID, "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(UUID.fromString(uuid));
        return name == null ? defaultName : name;
    }

    public static String lookup(UUID uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
*/
}
