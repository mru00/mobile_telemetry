// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   ADS1115.h
 * Author: mru
 *
 * Created on January 25, 2016, 10:55 PM
 * 
 * http://www.ti.com/lit/ds/symlink/ads1115.pdf
 */

#ifndef ADS1115_H
#define ADS1115_H

#include <stdint.h>
#include "app_util.h"
#include "nrf_drv_twi.h"

//#define ADS1115_IQR_DEV


#define ADS1115_ADDR_BASE 0x48

typedef enum {
    ADS1115_ADDR_PIN_GND = 0x0,
    ADS1115_ADDR_PIN_VDD = 0x1,
    ADS1115_ADDR_PIN_SDA = 0x2,
    ADS1115_ADDR_PIN_SCL = 0x3
} ads1115_addr_pin_t;

typedef enum {
    ADS1115_REG_CONVERSION = 0x00,
    ADS1115_REG_CONFIGURATION = 0x01,
    ADS1115_REG_LO_THRESH = 0x02,
    ADS1115_REG_HI_THRESH = 0x03
} ads1115_reg_addr_t;

/*
 * OS: Operational status/single-shot conversion start
 * 
 * This bit determines the operational status of the device.
 * This bit can only be written when in power-down mode.
 * 
 * For a write status:
 *   0 : No effect
 *   1 : Begin a single conversion (when in power-down mode)
 * 
 * For a read status:
 *   0 : Device is currently performing a conversion
 *   1 : Device is not currently performing a conversion
 */

typedef enum {
    ADS1115_OS_READ_RUNNING = 0,
    ADS1115_OS_READ_STOPPED = 1,
    ADS1115_OS_WRITE_START = 1,
    ADS1115_OS_WRITE_NO_EFFECT = 0,
    ADS1115_OS_DEFAULT = ADS1115_OS_WRITE_NO_EFFECT
} ads1115_operational_status_t;

/*
 MUX[2:0]: Input multiplexer configuration (ADS1115 only)
 * These bits configure the input multiplexer. They serve no function on the ADS1113/4.
 * 000 : AINP = AIN0 and AINN = AIN1 (default) 
 * 001 : AINP = AIN0 and AINN = AIN3 
 * 010 : AINP = AIN1 and AINN = AIN3 
 * 011 : AINP = AIN2 and AINN = AIN3 
 * 100 : AINP = AIN0 and AINN = GND
 * 101 : AINP = AIN1 and AINN = GND
 * 110 : AINP = AIN2 and AINN = GND
 * 111 : AINP = AIN3 and AINN = GND
 */
typedef enum {
    ADS1115_MUX_A0_A1 = 0,
    ADS1115_MUX_A0_A3 = 1,
    ADS1115_MUX_A1_A3 = 2,
    ADS1115_MUX_A2_A3 = 3,
    ADS1115_MUX_A0 = 4,
    ADS1115_MUX_A1 = 5,
    ADS1115_MUX_A2 = 6,
    ADS1115_MUX_A3 = 7,
    ADS1115_MUX_DEFAULT = ADS1115_MUX_A0_A1
} ads1115_mux_t;

/*
 * PGA[2:0]: Programmable gain amplifier configuration (ADS1114 and ADS1115 only)
 * These bits configure the programmable gain amplifier. They serve no function on the ADS1113.
 * 000 : FS = ±6.144V(1) 
 * 001 : FS = ±4.096V(1) 
 * 010 : FS = ±2.048V (default) 
 * 011 : FS = ±1.024V 
 * 100 : FS = ±0.512V
 * 101 : FS = ±0.256V
 * 110 : FS = ±0.256V
 * 111 : FS = ±0.256V
 */
typedef enum {
    ADS1115_PGA_2_3 = 0,
    ADS1115_PGA_1 = 1,
    ADS1115_PGA_2 = 2,
    ADS1115_PGA_4 = 3,
    ADS1115_PGA_8 = 4,
    ADS1115_PGA_16 = 5,
    ADS1115_PGA_DEFAULT = ADS1115_PGA_2
} ads1115_pga_t;

/*
 * MODE: Device operating mode
 * This bit controls the current operational mode of the ADS1113/4/5.
 * 0 : Continuous conversion mode
 * 1 : Power-down single-shot mode (default)
 */
typedef enum {
    ADS1115_MODE_CONTINUOUS = 0,
    ADS1115_MODE_SINGLE = 1,
    ADS1115_MODE_DEFAULT = ADS1115_MODE_SINGLE
} ads1115_mode_t;

/*
 * DR[2:0]: Data rate
 * These bits control the data rate setting.
 * 000 : 8SPS 
 * 001 : 16SPS 
 * 010 : 32SPS 
 * 011 : 64SPS 
 * 100 : 128SPS (default)
 * 101 : 250SPS
 * 110 : 475SPS
 * 111 : 860SPS

 */

typedef enum {
    ADS1115_DATA_RATE_8 = 0,
    ADS1115_DATA_RATE_16 = 1,
    ADS1115_DATA_RATE_32 = 2,
    ADS1115_DATA_RATE_64 = 3,
    ADS1115_DATA_RATE_128 = 4,
    ADS1115_DATA_RATE_256 = 5,
    ADS1115_DATA_RATE_475 = 6,
    ADS1115_DATA_RATE_860 = 7,
    ADS1115_DATA_RATE_DEFAULT = ADS1115_DATA_RATE_128
} ads1115_data_rate_t;

/*
 * COMP_MODE: Comparator mode (ADS1114 and ADS1115 only)
 * This bit controls the comparator mode of operation. It changes whether the comparator is implemented as a
 * traditional comparator (COMP_MODE = '0') or as a window comparator (COMP_MODE = '1'). It serves no
 * function on the ADS1113.
 * 0 : Traditional comparator with hysteresis (default)
 * 1 : Window comparator
 */
typedef enum {
    ADS1115_COMP_MODE_TRADITIONAL = 0,
    ADS1115_COMP_MODE_WINDOW = 1,
    ADS1115_COMP_MODE_DEFAULT = ADS1115_COMP_MODE_TRADITIONAL
} ads1115_comp_mode_t;

/*
 * COMP_POL: Comparator polarity (ADS1114 and ADS1115 only)
 * This bit controls the polarity of the ALERT/RDY pin. When COMP_POL = '0' the comparator output is active
 * low. When COMP_POL='1' the ALERT/RDY pin is active high. It serves no function on the ADS1113.
 * 0 : Active low (default)
 * 1 : Active high
 */

typedef enum {
    ADS1115_COMP_POL_AL = 0,
    ADS1115_COMP_POL_AH = 1,
    ADS1115_COMP_POL_DEFAULT = ADS1115_COMP_POL_AL
} ads1115_comp_pol_t;

/*
 * COMP_LAT: Latching comparator (ADS1114 and ADS1115 only)
 * This bit controls whether the ALERT/RDY pin latches once asserted or clears once conversions are within the
 * margin of the upper and lower threshold values. When COMP_LAT = '0', the ALERT/RDY pin does not latch
 * when asserted. When COMP_LAT = '1', the asserted ALERT/RDY pin remains latched until conversion data
 * are read by the master or an appropriate SMBus alert response is sent by the master, the device responds with
 * its address, and it is the lowest address currently asserting the ALERT/RDY bus line. This bit serves no
 * function on the ADS1113.
 * 0 : Non-latching comparator (default)
 * 1 : Latching comparator
 */
typedef enum {
    ADS1115_COMP_LAT_NONLATCHING = 0,
    ADS1115_COMP_LAT_LATCHING = 1,
    ADS1115_COMP_LAT_DEFAULT = ADS1115_COMP_LAT_NONLATCHING
} ads1115_comp_lat_t;

/*
 * COMP_QUE: Comparator queue and disable (ADS1114 and ADS1115 only)
 * These bits perform two functions. When set to '11', they disable the comparator function and put the
 * ALERT/RDY pin into a high state. When set to any other value, they control the number of successive
 * conversions exceeding the upper or lower thresholds required before asserting the ALERT/RDY pin. They
 * serve no function on the ADS1113.
 * 00 : Assert after one conversion
 * 01 : Assert after two conversions
 * 10 : Assert after four conversions
 * 11 : Disable comparator (default)
 */

typedef enum {
    ADS1115_COMP_QUE_ONE = 0,
    ADS1115_COMP_QUE_TWO = 1,
    ADS1115_COMP_QUE_FOUR = 2,
    ADS1115_COMP_QUE_DISABLE = 3,
    ADS1115_COMP_QUE_DEFAULT = ADS1115_COMP_QUE_DISABLE
} ads1115_comp_queue_t;

typedef union {

    struct {
        ads1115_mode_t mode : 1;
        ads1115_pga_t pga : 3;
        ads1115_mux_t mux : 3;
        ads1115_operational_status_t os : 1;

        ads1115_comp_queue_t comparator_que : 2;
        ads1115_comp_lat_t comparator_lat : 1;
        ads1115_comp_pol_t comparator_pol : 1;
        ads1115_comp_mode_t comparator_mode : 1;
        ads1115_data_rate_t data_rate : 3;
    };

    struct {
        uint8_t value_h;
        uint8_t value_l;
    };

} ads1115_config_reg_t;

typedef enum {
    ADS1115_ALERT_RDY_ALERT = 0,
    ADS1115_ALERT_RDY_RDY = 1
} ads1115_alert_rdy_t;


STATIC_ASSERT(sizeof (ads1115_config_reg_t) == 2);

typedef struct {
    nrf_drv_twi_t const * const twi;
    uint8_t addr_pin;
    ads1115_config_reg_t config_reg;
    ads1115_addr_pin_t alert_rdy_pin;
    ads1115_alert_rdy_t alert_rdy_mode;

#ifdef ADS1115_IQR_DEV
    enum ads1115_mux_t current_channel;
    void (*on_conversion_done)(enum ads1115_mux_t channel, int16_t value);
#endif
} ads1115_config_t;


ret_code_t ads1115_init(ads1115_config_t* config);
ret_code_t ads1115_read_channel_sync(ads1115_config_t* config, ads1115_pga_t pga, ads1115_mux_t channel, int16_t* value);


#ifdef ADS1115_IQR_DEV
ret_code_t ads1115_read_channel_async(struct ads1115_config_t* config, enum ads1115_mux_t channel);
#endif



#endif /* ADS1115_H */

