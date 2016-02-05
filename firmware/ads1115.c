// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


#include "ads1115.h"

#include "app_error.h"
#include "app_trace.h"
#include "nrf_delay.h"
#include "nrf_gpio.h"


#define LOG(msg, ...) do { app_trace_log("[ADS1115] " msg "\r\n", ## __VA_ARGS__); nrf_delay_ms(1); } while(0)


ret_code_t ads1115_write_config(ads1115_config_t* config);
ret_code_t ads1115_read_conversion(ads1115_config_t* config, int16_t* conversion);
ret_code_t ads1115_read_config(ads1115_config_t* config, ads1115_config_reg_t* status);

#ifdef ADS1115_IQR_DEV

static ads1115_config_t* current_config = NULL;

static void in_pin_handler(nrf_drv_gpiote_pin_t pin, nrf_gpiote_polarity_t action) {
    ret_code_t err_code = NRF_SUCCESS;

    int16_t value;

    if (current_config == NULL) {
        LOG("current_config not set");
        APP_ERROR_CHECK(1);
    }
    if (current_config->on_conversion_done == NULL) {
        LOG("on_conversion_done not set");
        APP_ERROR_CHECK(1);
    }


    err_code = ads1115_read_conversion(current_config, &value);
    APP_ERROR_CHECK(err_code);

    current_config->on_conversion_done(current_config->config_reg.mux, value);
}

#endif

ret_code_t ads1115_init(ads1115_config_t* config) {
    ret_code_t err_code = NRF_SUCCESS;

    err_code = ads1115_write_config(config);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to set configuration");
        return err_code;
    }

    if (config->alert_rdy_mode == ADS1115_ALERT_RDY_RDY && config->alert_rdy_pin >= 0) {
        nrf_gpio_cfg_sense_input(config->alert_rdy_pin, NRF_GPIO_PIN_NOPULL, NRF_GPIO_PIN_SENSE_HIGH);
    }

    return NRF_SUCCESS;
}

ret_code_t ads1115_write_config(ads1115_config_t* config) {
    ret_code_t err_code = NRF_SUCCESS;
    uint8_t trx[3];
    trx[0] = ADS1115_REG_CONFIGURATION;
    trx[1] = config->config_reg.value_h;
    trx[2] = config->config_reg.value_l;
    
    err_code = nrf_drv_twi_tx(config->twi, ADS1115_ADDR_BASE + config->addr_pin, trx, 3, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to write config register (ads1115_write_config)");
        return err_code;
    }

    if (config->alert_rdy_mode == ADS1115_ALERT_RDY_RDY) {
        trx[0] = ADS1115_REG_HI_THRESH;
        trx[1] = 0x80;
        trx[2] = 0x00;
        err_code = nrf_drv_twi_tx(config->twi, ADS1115_ADDR_BASE + config->addr_pin, trx, 3, false);
        APP_ERROR_CHECK(err_code);

        trx[0] = ADS1115_REG_LO_THRESH;
        trx[1] = 0x00;
        trx[2] = 0x00;
        err_code = nrf_drv_twi_tx(config->twi, ADS1115_ADDR_BASE + config->addr_pin, trx, 3, false);
        APP_ERROR_CHECK(err_code);
    }

    return NRF_SUCCESS;
}

ret_code_t ads1115_read_config(ads1115_config_t* config, ads1115_config_reg_t* status) {
    ret_code_t err_code = NRF_SUCCESS;
    uint8_t trx[3];
    trx[0] = ADS1115_REG_CONFIGURATION;

    err_code = nrf_drv_twi_tx(config->twi, ADS1115_ADDR_BASE + config->addr_pin, trx, 1, false);
    APP_ERROR_CHECK(err_code);

    err_code = nrf_drv_twi_rx(config->twi, ADS1115_ADDR_BASE + config->addr_pin, trx, 2, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to read config register");
        APP_ERROR_CHECK(err_code);
        return err_code;
    }

    status->value_h = trx[0];
    status->value_l = trx[1];
    app_trace_dump(trx, 2);
    return NRF_SUCCESS;
}

ret_code_t ads1115_read_conversion(ads1115_config_t* config, int16_t* conversion) {
    ret_code_t err_code = NRF_SUCCESS;
    uint8_t trx[3];
    trx[0] = ADS1115_REG_CONVERSION;

    err_code = nrf_drv_twi_tx(config->twi, ADS1115_ADDR_BASE + config->addr_pin, trx, 1, false);
    APP_ERROR_CHECK(err_code);

    err_code = nrf_drv_twi_rx(config->twi, ADS1115_ADDR_BASE + config->addr_pin, trx, 2, false);
    if (err_code != NRF_SUCCESS) {
        LOG("failed to read config register");
        APP_ERROR_CHECK(err_code);
        return err_code;
    }

    *conversion = (trx[0] << 8) | (trx[1]);

    return NRF_SUCCESS;
}

ret_code_t ads1115_read_channel_sync(ads1115_config_t* config, ads1115_pga_t pga, ads1115_mux_t channel, int16_t* value) {
    ret_code_t err_code = NRF_SUCCESS;

    ads1115_config_reg_t status;

    uint16_t test_counter = 0;
    for (test_counter = 0;; test_counter++) {
        err_code = ads1115_read_config(config, &status);
        APP_ERROR_CHECK(err_code);

        if (status.os == ADS1115_OS_READ_RUNNING) {
            LOG("ADC currently converting");
            nrf_delay_us(1);
        } else {
            break;
        }
        if (test_counter > 10) {
            LOG("timeout waiting to get ready");
            break;
        }
    }
    while (status.os == ADS1115_OS_READ_RUNNING);


    config->config_reg.mux = channel;
    config->config_reg.os = ADS1115_OS_WRITE_START;
    config->config_reg.pga = pga;
    
    err_code = ads1115_write_config(config);
    APP_ERROR_CHECK(err_code);

    test_counter = 0;

    do {
        nrf_delay_ms(1);
        err_code = ads1115_read_config(config, &status);
        APP_ERROR_CHECK(err_code);
        if (test_counter++ > 10) {
            LOG("timeout while waiting for ADC");
            break;
        }
    } while (status.os == ADS1115_OS_READ_RUNNING);

    //LOG("ran %d iterations", test_counter);

    err_code = ads1115_read_conversion(config, value);
    APP_ERROR_CHECK(err_code);

    return NRF_SUCCESS;
}

#ifdef ADS1115_IQR_DEV

ret_code_t ads1115_read_channel_async(ads1115_config_t* config, ads1115_mux_t channel) {
    ret_code_t err_code = NRF_SUCCESS;
    config->config_reg.mux = channel;
    config->config_reg.os = ADS1115_OS_START;

    err_code = ads1115_write_config(config);
    APP_ERROR_CHECK(err_code);

    return err_code;

}
#endif
