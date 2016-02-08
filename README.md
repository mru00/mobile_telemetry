# Mobile Telemetry - RC Car telemetry for your Android phone

Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.me/Muehlbauer)


This is a telemetry module targeting RC cars. 

* Uses Bluetooth Smart (Bluetooth Low Energy) as transport
* Uses your Android phone to display the data

It currently supports 

* Cell voltages (either through ADS1115 or nRF51-builtin ADCs)
* Total battery current (via http://www.dx.com/p/new-apm-osd-90a-voltage-current-sensor-connector-power-module-black-red-297400#.VrZpt99yuis, through ADS1115)
* Accelerometer (through ADXL345)

The hardware is currently just the modules hand-wired on a perfboard. No proper PCB is planned yet. Hardware is cheap stuff from dx.com.

![screenshot1](https://cloud.githubusercontent.com/assets/581904/12903013/e6d4bb40-cec4-11e5-8214-e05cf1bb4177.png)
![screenshot2](https://cloud.githubusercontent.com/assets/581904/12903014/e6e57c0a-cec4-11e5-8e2d-675b02c0b3fd.png)
![board1](https://cloud.githubusercontent.com/assets/581904/12903018/ebbab2b8-cec4-11e5-9fea-841592ad5187.jpg)



The telemetry module is built around the nRF51 SoC. I used a BLE Nano (http://redbearlab.com/blenano/), it nicely combines a DCDC and the nRF51.

## Firmware for the SoC

The firmware source can be found in the `firmware` directory, a pre-built binary is available in `release/target.hex`.

## The Android app

The app source is available in the `app` directory. A pre-built binary is available in `release/Application-release-unsigned.apk`


## Hardware Variants

It is possible to run the telemetry model with different hardware configurations.

* Only cell voltages
 * Requires only the BLE Nano module

* Accelerometer ADXL345

* Cell current
 * Add the ADS1115 ADC


## Used hardware modules

The module is just the following modules wired together.

* http://redbearlab.com/blenano/

* http://www.dx.com/p/geeetech-6dof-adxl345-and-itg3205-digital-combo-board-red-384317#.VrXOlN9yuis

* http://www.dx.com/p/ads1115-16-bit-i2c-adc-development-board-module-for-arduino-raspberry-pi-blue-384030#.VrXOyd9yuis

* http://www.dx.com/p/double-sided-glass-fiber-prototyping-pcb-universal-board-12-piece-pack-145675#.VrXPBN9yuis

* http://www.dx.com/p/new-apm-osd-90a-voltage-current-sensor-connector-power-module-black-red-297400#.VrZpt99yuis

## Next steps


* make the app better
 * almost no error handling, no UX

* draw own pcb, 
 * current sensor 
  * http://www.ti.com/product/INA220
  * INA219
  * http://www.infineon.com/cms/de/product/sensor/magnetic-current-sensor/channel.html?channel=db3a30433afc7e3e013b288381965ab4

 * find a better nRF51 module, https://www.nordicsemi.com/Products/3rd-Party-Bluetooth-Smart-Modules
  * http://www.hosiden.co.jp/en/news/product/hrm1017.html
  * or even Microchip RN2040


