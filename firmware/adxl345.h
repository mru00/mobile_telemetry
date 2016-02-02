#ifndef ADXL345_H
#define ADXL345_H


#include <stdint.h>
#include "app_util.h"
#include "nrf_drv_twi.h"


// http://www.analog.com/media/en/technical-documentation/data-sheets/ADXL345.PDF

// 7 bit addresses
#define ADXL345_ADDR_ALT1 0x1D
#define ADXL345_ADDR_ALT0 0x53

// register addresses
#define ADXL345_REG_DEVID 0x00
#define ADXL345_REG_THRESH_TAP 0x1D
#define ADXL345_REG_OFSX 0x1E
#define ADXL345_REG_OFSY 0x1F
#define ADXL345_REG_OFSZ 0x20
#define ADXL345_REG_DUR 0x21
#define ADXL345_REG_LATENT 0x22
#define ADXL345_REG_WINDOW 0x23
#define ADXL345_REG_THRESH_ACT 0x24
#define ADXL345_REG_THRESH_INACT 0x25
#define ADXL345_REG_TIME_INACT 0x26
#define ADXL345_REG_ACT_INACT_CTL 0x27
#define ADXL345_REG_THRESH_FF 0x28
#define ADXL345_REG_TIME_FF 0x29
#define ADXL345_REG_TAP_AXES 0x2A
#define ADXL345_REG_ACT_TAP_STATUS 0x2B
#define ADXL345_REG_BW_RATE 0x2C
#define ADXL345_REG_POWER_CTL 0x2D
#define ADXL345_REG_INT_ENABLE 0x2E
#define ADXL345_REG_INT_MAP 0x2F
#define ADXL345_REG_INT_SOURCE 0x30
#define ADXL345_REG_DATA_FORMAT 0x31
#define ADXL345_REG_DATAX0 0x32
#define ADXL345_REG_DATAX1 0x33
#define ADXL345_REG_DATAY0 0x34
#define ADXL345_REG_DATAY1 0x35
#define ADXL345_REG_DATAZ0 0x36
#define ADXL345_REG_DATAZ1 0x37
#define ADXL345_REG_FIFO_CTL 0x38
#define ADXL345_REG_FIFO_STATUS 0x39

typedef enum {
    ADXL345_DATA_FORMAT_JUSTIFY_RIGHT = 0,
    ADXL345_DATA_FORMAT_JUSTIFY_LEFT = 1
} adxl_data_format_justify_t;

typedef enum {
    ADXL345_DATA_FORMAT_RANGE_2 = 0,
    ADXL345_DATA_FORMAT_RANGE_4 = 1,
    ADXL345_DATA_FORMAT_RANGE_8 = 2,
    ADXL345_DATA_FORMAT_RANGE_16 = 3
} adxl_data_format_range;

typedef enum {
    ADXL345_DATA_FORMAT_INT_ACTIVE_HIGH = 0,
    ADXL345_DATA_FORMAT_INT_ACTIVE_LOW = 1
} adxl_data_format_int_active;

typedef enum {
    ADXL345_DATA_FORMAT_RES_10BIT = 0,
    ADXL345_DATA_FORMAT_RES_FULL = 1
} adxl_data_format_res_t;

typedef enum {
    ADXL345_DATA_FORMAT_SPI_4WIRE = 0,
    ADXL345_DATA_FORMAT_SPI_3WIRE = 1
} adxl_data_format_spi_t;

typedef enum {
    ADXL345_DATA_FORMAT_SELFTEST_DISABLE = 0,
    ADXL345_DATA_FORMAT_SELFTEST_ENABLE = 1
} adxl_data_format_selftest_t;

typedef union {

    struct __attribute__((__packed__)) {
        adxl_data_format_range range : 2;
        adxl_data_format_justify_t justify : 1;
        adxl_data_format_res_t full_res : 1;
        uint8_t:        1;
        adxl_data_format_int_active int_invert : 1;
        adxl_data_format_spi_t spi : 1;
        adxl_data_format_selftest_t self_test : 1;
    };
    uint8_t value;
} adxl345_data_format;

typedef enum {
    ADXL345_INT_MAP_INT0 = 0,
    ADXL345_INT_MAP_INT1 = 1
} adxl345_int_map_t;

typedef union {

    struct {
        adxl345_int_map_t data_ready : 1;
        adxl345_int_map_t single_tap : 1;
        adxl345_int_map_t double_tap : 1;
        adxl345_int_map_t activity : 1;
        adxl345_int_map_t inactivity : 1;
        adxl345_int_map_t free_fall : 1;
        adxl345_int_map_t watermark : 1;
        adxl345_int_map_t overrun : 1;
    };
    uint8_t value;
} adxl345_int_map;

typedef enum {
    ADXL345_FIFO_MODE_BYPASS = 0,
    ADXL345_FIFO_MODE_FIFO = 1,
    ADXL345_FIFO_MODE_STREAM = 2,
    ADXL345_FIFO_MODE_TRIGGER = 3,
} adxl345_fifo_mode_t;

typedef enum {
    ADXL345_FIFO_TRIGGER_INT1 = 0,
    ADXL345_FIFO_TRIGGER_INT2 = 1
} adxl345_fifo_trigger_t;

typedef union {
    struct {
        uint8_t samples : 5;
        adxl345_fifo_trigger_t trigger : 1;
        adxl345_fifo_mode_t mode : 2;
    };
    uint8_t value;
} adxl345_fifo_ctl;




typedef enum {
    ADXL345_BW_LOW_POWER_NORMAL = 0,
    ADXL345_BW_LOW_POWER_REDUCED = 1
} adxl345_bw_mode_low_power_t;

typedef enum {
    ADXL345_ODR_400 = 12,
    ADXL345_ODR_200 = 11,
    ADXL345_ODR_100 = 10,
    ADXL345_ODR_50 = 9,
    ADXL345_ODR_25 = 8,
    ADXL345_ODR_12_5 = 7
} adxl345_odr_t;


typedef union {
    struct {
        adxl345_odr_t rate : 4;
        adxl345_bw_mode_low_power_t power : 1;
uint8_t:
        3;
    };
    uint8_t value;
} adxl345_bw_mode_t;




typedef enum {
    ADXL345_LINK_0 = 0,
    ADXL345_LINK_1 = 1
} adxl345_link_t;

typedef enum {
    ADXL345_AUTO_SLEEP_DISABLE = 0,
    ADXL345_AUTO_SLEEP_ENABLE = 1,
} adxl345_auto_sleep_t;

typedef enum {
    ADXL345_MEASURE_STANDBY = 0,
    ADXL345_MEASURE_MEASURE = 1,
} adxl345_measure_t;

typedef enum {
    ADXL345_SLEEP_NOSLEEP = 0,
    ADXL345_SLEEP_SLEEP = 1,
} adxl345_sleep_t;

typedef enum {
    ADXL345_WAKEUP_8 = 0,
    ADXL345_WAKEUP_4 = 1,
    ADXL345_WAKEUP_2 = 2,
    ADXL345_WAKEUP_1 = 3,
} adxl345_wakeup_t;

typedef union {
    struct {
        adxl345_wakeup_t wakeup : 2;
        adxl345_sleep_t sleep : 1;
        adxl345_measure_t measure : 1;
        adxl345_auto_sleep_t auto_sleep : 1;
        adxl345_link_t link : 1;
uint8_t:
        2;
    };
    uint8_t value;
} adxl345_power_ctl_t;




STATIC_ASSERT(sizeof (adxl345_bw_mode_t) == 1);
STATIC_ASSERT(sizeof (adxl345_int_map) == 1);
STATIC_ASSERT(sizeof (adxl345_fifo_ctl) == 1);
STATIC_ASSERT(sizeof (adxl345_power_ctl_t) == 1);

ret_code_t adxl345_init(nrf_drv_twi_t const * const twi);
ret_code_t adxl345_read_values(nrf_drv_twi_t const * const twi, int16_t* values);


#endif /* ADXL345_H */

