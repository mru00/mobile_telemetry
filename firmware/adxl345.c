
#include "adxl345.h"
#include "app_error.h"
#include "app_trace.h"

#define ADXL345_SELECTED_ADDR ADXL345_ADDR_ALT0

#define LOG(msg) do { app_trace_log("[ADXL345] " msg "\r\n"); } while(0)

ret_code_t adxl345_init(nrf_drv_twi_t const * const twi) {
    ret_code_t err_code = 0;

    uint8_t trx[10];

    const adxl345_data_format df = {
        .self_test = ADXL345_DATA_FORMAT_SELFTEST_DISABLE,
        .spi = ADXL345_DATA_FORMAT_SPI_3WIRE,
        .int_invert = ADXL345_DATA_FORMAT_INT_ACTIVE_HIGH,
        .full_res = ADXL345_DATA_FORMAT_RES_FULL,
        .justify = ADXL345_DATA_FORMAT_JUSTIFY_LEFT,
//                .justify = ADXL345_DATA_FORMAT_JUSTIFY_RIGHT,
        .range = ADXL345_DATA_FORMAT_RANGE_4
    };

    const adxl345_int_map im = {
        .data_ready = ADXL345_INT_MAP_INT0,
        .single_tap = ADXL345_INT_MAP_INT0,
        .double_tap = ADXL345_INT_MAP_INT0,
        .activity = ADXL345_INT_MAP_INT0,
        .inactivity = ADXL345_INT_MAP_INT0,
        .free_fall = ADXL345_INT_MAP_INT0,
        .watermark = ADXL345_INT_MAP_INT0,
        .overrun = ADXL345_INT_MAP_INT0
    };

    const adxl345_fifo_ctl fc = {
        .trigger = ADXL345_FIFO_TRIGGER_INT1,
        .samples = 12,
        .mode = ADXL345_FIFO_MODE_BYPASS
    };

    const adxl345_bw_mode_t bw = {
        .power = ADXL345_BW_LOW_POWER_NORMAL,
        .rate = ADXL345_ODR_400
    };

    const adxl345_power_ctl_t pc = {
        .link = ADXL345_LINK_1,
        .auto_sleep = ADXL345_AUTO_SLEEP_DISABLE,
        .measure = ADXL345_MEASURE_MEASURE,
        .sleep = ADXL345_SLEEP_NOSLEEP,
        .wakeup = ADXL345_WAKEUP_1,
    };


    uint8_t devid;
    trx[0] = ADXL345_REG_DEVID;
    err_code = nrf_drv_twi_tx(twi, ADXL345_SELECTED_ADDR, trx, 1, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to set register address for devid");
        return err_code;
    }

    err_code = nrf_drv_twi_rx(twi, ADXL345_SELECTED_ADDR, &devid, 1, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed read ADXL345_REG_DEVID");
        return err_code;
    }

    if (devid != 0xe5) {
        LOG("failed to get correct device id");
        return NRF_ERROR_NOT_FOUND;
    }


    trx[0] = ADXL345_REG_INT_MAP;
    trx[1] = im.value;
    err_code = nrf_drv_twi_tx(twi, ADXL345_SELECTED_ADDR, trx, 2, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to set ADXL345_REG_INT_MAP");
        return err_code;
    }

    trx[0] = ADXL345_REG_DATA_FORMAT;
    trx[1] = df.value;
    err_code = nrf_drv_twi_tx(twi, ADXL345_SELECTED_ADDR, trx, 2, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to set ADXL345_REG_DATA_FORMAT");
        return err_code;
    }

    trx[0] = ADXL345_REG_FIFO_CTL;
    trx[1] = fc.value;
    err_code = nrf_drv_twi_tx(twi, ADXL345_SELECTED_ADDR, trx, 2, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to set ADXL345_REG_FIFO_CTL");
        return err_code;
    }

    trx[0] = ADXL345_REG_BW_RATE;
    trx[1] = bw.value;
    err_code = nrf_drv_twi_tx(twi, ADXL345_SELECTED_ADDR, trx, 2, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to set ADXL345_REG_BW_RATE");
        return err_code;
    }

    trx[0] = ADXL345_REG_POWER_CTL;
    trx[1] = pc.value;
    app_trace_dump(trx, 2);
    err_code = nrf_drv_twi_tx(twi, ADXL345_SELECTED_ADDR, trx, 2, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to set ADXL345_REG_POWER_CTL");
        return err_code;
    }


    return NRF_SUCCESS;
}

ret_code_t adxl345_read_values(nrf_drv_twi_t const * const twi, int16_t* values) {
    ret_code_t err_code = NRF_SUCCESS;
    uint8_t trx[6];
    trx[5] = 0xa5;
    
    trx[0] = ADXL345_REG_DATAX0;
    err_code = nrf_drv_twi_tx(twi, ADXL345_SELECTED_ADDR, trx, 1, false);
    APP_ERROR_CHECK(err_code);

    err_code = nrf_drv_twi_rx(twi, ADXL345_SELECTED_ADDR, trx, 6, false);
    APP_ERROR_CHECK(err_code);
    
    //app_trace_dump(trx, 6);

    values[0] = trx[0] | (trx[1] << 8);
    values[1] = trx[2] | (trx[3] << 8);
    values[2] = trx[4] | (trx[5] << 8);

    return err_code;
}
