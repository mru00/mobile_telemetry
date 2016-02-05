// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

/* Copyright (c) 2015 Nordic Semiconductor. All Rights Reserved.
 *
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC
 * SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 *
 * Licensees are granted free, non-transferable use of the information. NO
 * WARRANTY of ANY KIND is provided. This heading must NOT be removed from
 * the file.
 *
 */

#ifndef BLE_RCMON_H__
#define BLE_RCMON_H__

#include <stdint.h>
#include <stdbool.h>
#include "ble.h"
#include "ble_bps.h"
#include "ble_srv_common.h"

#define RCMON_UUID_BASE        {0xCA, 0xFE, 0xBC, 0xEA, 0x5F, 0x78, 0x23, 0x15, \
                              0xDE, 0xEF, 0x12, 0x12, 0x00, 0x00, 0x00, 0x00}
#define RCMON_UUID_SERVICE     0x2523

#define RCMON_UUID_DATA_CHAR   0x2530
#define RCMON_UUID_CONFIG_CHAR 0x2531

typedef struct ble_rcmon_s ble_rcmon_t;

#define MAX_NUMBER_OF_CELLS 3

typedef struct __attribute__((__packed__)){
    uint16_t vcell[MAX_NUMBER_OF_CELLS];
    uint16_t current;
    uint16_t acc_x;
    uint16_t acc_y;
    uint16_t acc_z;
} ble_rcmon_data_t;

typedef struct __attribute__((__packed__)){
    uint8_t version;
    uint8_t num_cells;
    uint8_t has_accelerometer;
} ble_rcmon_config_t;


typedef struct
{
    ble_rcmon_data_t volatile *  pdata;
    ble_rcmon_config_t volatile * pconfig;
} ble_rcmon_init_t;


struct ble_rcmon_s
{
    uint16_t                    service_handle;      /**< Handle of LED Button Service (as provided by the BLE stack). */
    ble_gatts_char_handles_t    data_char_handles; /**< Handles related to the Button Characteristic. */
    ble_gatts_char_handles_t    config_char_handles; /**< Handles related to the Button Characteristic. */
    uint8_t                     uuid_type;           /**< UUID type for the LED Button Service. */
    uint16_t                    conn_handle;         /**< Handle of the current connection (as provided by the BLE stack). BLE_CONN_HANDLE_INVALID if not in a connection. */
};

uint32_t ble_rcmon_init(ble_rcmon_t * p_rcmon, const ble_rcmon_init_t * p_rcmon_init);
void ble_rcmon_on_ble_evt(ble_rcmon_t * p_rcmon, ble_evt_t * p_ble_evt);

#endif // BLE_RCMON_H__

/** @} */
