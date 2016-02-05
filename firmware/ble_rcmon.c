// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

/* Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 *
 * Use of this source code is governed by a BSD-style license that can be
 * found in the license.txt file.
 */

#include <string.h>
#include "ble_rcmon.h"
#include "nordic_common.h"
#include "ble_srv_common.h"
#include "app_util.h"
#include "app_trace.h"
#include "app_error.h"

static void on_connect(ble_rcmon_t * p_rcmon, ble_evt_t * p_ble_evt) {
    p_rcmon->conn_handle = p_ble_evt->evt.gap_evt.conn_handle;
    app_trace_log("on_connect\r\n");
}

static void on_disconnect(ble_rcmon_t * p_rcmon, ble_evt_t * p_ble_evt) {
    UNUSED_PARAMETER(p_ble_evt);
    p_rcmon->conn_handle = BLE_CONN_HANDLE_INVALID;
    app_trace_log("on_disconnect\r\n");
}

static void on_write(ble_rcmon_t * p_rcmon, ble_evt_t * p_ble_evt) {
    //ble_gatts_evt_write_t * p_evt_write = &p_ble_evt->evt.gatts_evt.params.write;
}

void ble_rcmon_on_ble_evt(ble_rcmon_t * p_rcmon, ble_evt_t * p_ble_evt) {
    switch (p_ble_evt->header.evt_id) {
        case BLE_GAP_EVT_CONNECTED:
            on_connect(p_rcmon, p_ble_evt);
            break;

        case BLE_GAP_EVT_DISCONNECTED:
            on_disconnect(p_rcmon, p_ble_evt);
            break;

        case BLE_GATTS_EVT_WRITE:
            on_write(p_rcmon, p_ble_evt);
            break;

        default:
            // No implementation needed.
            break;
    }
}

static uint32_t char_add_user(ble_rcmon_t * p_rcmon, uint16_t uuid, uint8_t* p_value, size_t size, const char* title, ble_gatts_char_handles_t* handle) {
    ble_gatts_char_md_t char_md;
    ble_gatts_attr_md_t cccd_md;
    ble_gatts_attr_t attr_char_value;
    ble_uuid_t ble_uuid;
    ble_gatts_attr_md_t attr_md;

    memset(&cccd_md, 0, sizeof (cccd_md));

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&cccd_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&cccd_md.write_perm);
    cccd_md.vloc = BLE_GATTS_VLOC_STACK;

    memset(&char_md, 0, sizeof (char_md));

    char_md.char_props.read = 1;
    char_md.char_props.notify = 1;
    char_md.p_char_user_desc = (uint8_t*) title;
    char_md.char_user_desc_size = strlen(title);
    char_md.char_user_desc_max_size = char_md.char_user_desc_size;
    char_md.p_char_pf = NULL;
    char_md.p_user_desc_md = NULL;
    char_md.p_cccd_md = &cccd_md;
    char_md.p_sccd_md = NULL;

    ble_uuid.type = p_rcmon->uuid_type;
    ble_uuid.uuid = uuid;

    memset(&attr_md, 0, sizeof (attr_md));

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&attr_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_NO_ACCESS(&attr_md.write_perm);
    attr_md.vloc = BLE_GATTS_VLOC_USER;
    attr_md.rd_auth = 0;
    attr_md.wr_auth = 0;
    attr_md.vlen = 0;

    memset(&attr_char_value, 0, sizeof (attr_char_value));

    attr_char_value.p_uuid = &ble_uuid;
    attr_char_value.p_attr_md = &attr_md;
    attr_char_value.init_len = size;
    attr_char_value.init_offs = 0;
    attr_char_value.max_len = size;
    attr_char_value.p_value = p_value;

    return sd_ble_gatts_characteristic_add(p_rcmon->service_handle, &char_md, &attr_char_value, handle);
}

static uint32_t char_add_stack(ble_rcmon_t * p_rcmon, uint16_t uuid, size_t size, const char* title, ble_gatts_char_handles_t* handle) {
    ble_gatts_char_md_t char_md;
    ble_gatts_attr_md_t cccd_md;
    ble_gatts_attr_t attr_char_value;
    ble_uuid_t ble_uuid;
    ble_gatts_attr_md_t attr_md;

    memset(&cccd_md, 0, sizeof (cccd_md));

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&cccd_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&cccd_md.write_perm);
    cccd_md.vloc = BLE_GATTS_VLOC_STACK;

    memset(&char_md, 0, sizeof (char_md));

    char_md.char_props.read = 1;
    char_md.char_props.notify = 1;
    char_md.p_char_user_desc = (uint8_t*) title;
    char_md.char_user_desc_size = strlen(title);
    char_md.char_user_desc_max_size = char_md.char_user_desc_size;
    char_md.p_char_pf = NULL;
    char_md.p_user_desc_md = NULL;
    char_md.p_cccd_md = &cccd_md;
    char_md.p_sccd_md = NULL;

    ble_uuid.type = p_rcmon->uuid_type;
    ble_uuid.uuid = uuid;

    memset(&attr_md, 0, sizeof (attr_md));

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&attr_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_NO_ACCESS(&attr_md.write_perm);
    attr_md.vloc = BLE_GATTS_VLOC_STACK;
    attr_md.rd_auth = 0;
    attr_md.wr_auth = 0;
    attr_md.vlen = 0;

    memset(&attr_char_value, 0, sizeof (attr_char_value));

    attr_char_value.p_uuid = &ble_uuid;
    attr_char_value.p_attr_md = &attr_md;
    attr_char_value.init_len = size;
    attr_char_value.init_offs = 0;
    attr_char_value.max_len = size;
    attr_char_value.p_value = NULL;

    return sd_ble_gatts_characteristic_add(p_rcmon->service_handle, &char_md, &attr_char_value, handle);
}

static uint32_t data_char_add(ble_rcmon_t * p_rcmon, const ble_rcmon_init_t * p_rcmon_init) {
    return char_add_user(p_rcmon, RCMON_UUID_DATA_CHAR, (uint8_t*) p_rcmon_init->pdata, sizeof (ble_rcmon_data_t), "data", &p_rcmon->data_char_handles);
}

static uint32_t config_char_add(ble_rcmon_t * p_rcmon, const ble_rcmon_init_t * p_rcmon_init) {
    return char_add_user(p_rcmon, RCMON_UUID_CONFIG_CHAR, (uint8_t*) p_rcmon_init->pconfig, sizeof (ble_rcmon_config_t), "config", &p_rcmon->config_char_handles);
}

uint32_t ble_rcmon_init(ble_rcmon_t * p_rcmon, const ble_rcmon_init_t * p_rcmon_init) {
    app_trace_log("\r\nrcmon_init\r\n");

    uint32_t err_code;
    ble_uuid_t ble_uuid;

    // Initialize service structure.
    p_rcmon->conn_handle = BLE_CONN_HANDLE_INVALID;

    // Add service.
    ble_uuid128_t base_uuid = {RCMON_UUID_BASE};
    err_code = sd_ble_uuid_vs_add(&base_uuid, &p_rcmon->uuid_type);
    APP_ERROR_CHECK(err_code);

    ble_uuid.type = p_rcmon->uuid_type;
    ble_uuid.uuid = RCMON_UUID_SERVICE;

    err_code = sd_ble_gatts_service_add(BLE_GATTS_SRVC_TYPE_PRIMARY, &ble_uuid, &p_rcmon->service_handle);
    APP_ERROR_CHECK(err_code);

    p_rcmon_init->pdata->acc_x = 1000;
    p_rcmon_init->pdata->acc_y = 2000;
    p_rcmon_init->pdata->acc_z = 3000;

    // Add characteristics.
    err_code = data_char_add(p_rcmon, p_rcmon_init);
    APP_ERROR_CHECK(err_code);

    err_code = config_char_add(p_rcmon, p_rcmon_init);
    APP_ERROR_CHECK(err_code);

    app_trace_log("added chars\r\n");

    return NRF_SUCCESS;
}




