
# Mobile Telemetry - RC Car telemetry for your Android phone

Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

[![Donate](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.me/Muehlbauer)


This is a telemetry model targeting RC cars. 

* Uses Bluetooth Smart as transport
* Uses your mobile phone to display the data

It currently supports 

* Cell voltages
* Total battery current
* Accelerometer

The hardware is currently just the modules hand-wired on a perfboard. No proper PCB is planned yet. 





The telemetry module is built around the nRF51 Soc. I used a BLE Nano (http://redbearlab.com/blenano/), it nicely combines a DCDC and the nRF51.

## Firmware for the SoC

The firmware source can be found in the `firmware` directory, a pre-built binary is available in `release/target.hex`.

## The Android app

The app source is available in the `app` directory. A pre-built binary is available in `release/Application-release-unsigned.apk`


## Hardware Variants

It is possible to run the telemetry model with different hardware configurations.

* Only cell voltages
** Requires only the BLE Nano module

* Accelerometer ADXL345

* Cell current
** Add the ADS1115 ADC


