/* Copyright (c) 2014 Nordic Semiconductor. All Rights Reserved.
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
/** @example examples/ble_peripheral/ble_app_hrs/main.c
 *
 * @brief Heart Rate Service Sample Application main file.
 *
 * This file contains the source code for a sample application using the Heart Rate service
 * (and also Battery and Device Information services). This application uses the
 * @ref srvlib_conn_params module.
 */

#include "boards.h"

#define LOG(msg, ...) do { app_trace_log("[MAIN] " msg "\r\n", ## __VA_ARGS__); nrf_delay_ms(1); } while(0)

#include <stdint.h>
#include <string.h>
#include "nordic_common.h"
#include "nrf.h"
#include "app_error.h"
#include "ble.h"
#include "ble_hci.h"
#include "ble_srv_common.h"
#include "ble_advdata.h"
#include "ble_advertising.h"
#include "ble_dis.h"
#include "ble_rcmon.h"

#ifdef BLE_DFU_APP_SUPPORT
#include "ble_dfu.h"
#include "dfu_app_handler.h"
#endif // BLE_DFU_APP_SUPPORT

#include "ble_conn_params.h"
#include "softdevice_handler.h"
#include "device_manager.h"


#include "app_timer.h"
#include "app_trace.h"

#include "bsp.h"
#include "bsp_btn_ble.h"

#include "pstorage.h"

#include "nrf_drv_twi.h"
#include "nrf_delay.h"
#include "nrf_gpio.h"
#include "nrf_adc.h"

#include "adxl345.h"
#include "ads1115.h"



#define IS_SRVC_CHANGED_CHARACT_PRESENT  1                                          /**< Include or not the service_changed characteristic. if not enabled, the server's database cannot be changed for the lifetime of the device*/

#define DEVICE_NAME                      "Mobile Telemetry"                            /**< Name of device. Will be included in the advertising data. */
#define MANUFACTURER_NAME                "MRU"                                      /**< Manufacturer. Will be passed to Device Information Service. */
#define APP_ADV_INTERVAL                 300                                        /**< The advertising interval (in units of 0.625 ms. This value corresponds to 25 ms). */
#define APP_ADV_TIMEOUT_IN_SECONDS       1800                                        /**< The advertising timeout in units of seconds. */

#define APP_TIMER_PRESCALER              0                                          /**< Value of the RTC1 PRESCALER register. */
#define APP_TIMER_OP_QUEUE_SIZE          4                                          /**< Size of timer operation queues. */

#define BATTERY_LEVEL_MEAS_INTERVAL      APP_TIMER_TICKS(500, APP_TIMER_PRESCALER) /**< Battery level measurement interval (ticks). */
#define ADS1115_MEAS_INTERVAL            APP_TIMER_TICKS(250, APP_TIMER_PRESCALER) /**< Battery level measurement interval (ticks). */
#define ADXL345_MEAS_INTERVAL            APP_TIMER_TICKS(500, APP_TIMER_PRESCALER) /**< Battery level measurement interval (ticks). */

#define MIN_CONN_INTERVAL                MSEC_TO_UNITS(400, UNIT_1_25_MS)           /**< Minimum acceptable connection interval (0.4 seconds). */
#define MAX_CONN_INTERVAL                MSEC_TO_UNITS(650, UNIT_1_25_MS)           /**< Maximum acceptable connection interval (0.65 second). */
#define SLAVE_LATENCY                    0                                          /**< Slave latency. */
#define CONN_SUP_TIMEOUT                 MSEC_TO_UNITS(4000, UNIT_10_MS)            /**< Connection supervisory timeout (4 seconds). */

#define FIRST_CONN_PARAMS_UPDATE_DELAY   APP_TIMER_TICKS(5000, APP_TIMER_PRESCALER) /**< Time from initiating event (connect or start of notification) to first time sd_ble_gap_conn_param_update is called (5 seconds). */
#define NEXT_CONN_PARAMS_UPDATE_DELAY    APP_TIMER_TICKS(30000, APP_TIMER_PRESCALER)/**< Time between each call to sd_ble_gap_conn_param_update after the first call (30 seconds). */
#define MAX_CONN_PARAMS_UPDATE_COUNT     3                                          /**< Number of attempts before giving up the connection parameter negotiation. */

#define SEC_PARAM_BOND                   1                                          /**< Perform bonding. */
#define SEC_PARAM_MITM                   0                                          /**< Man In The Middle protection not required. */
#define SEC_PARAM_IO_CAPABILITIES        BLE_GAP_IO_CAPS_NONE                       /**< No I/O capabilities. */
#define SEC_PARAM_OOB                    0                                          /**< Out Of Band data not available. */
#define SEC_PARAM_MIN_KEY_SIZE           7                                          /**< Minimum encryption key size. */
#define SEC_PARAM_MAX_KEY_SIZE           16                                         /**< Maximum encryption key size. */

#define DEAD_BEEF                        0xDEADBEEF                                 /**< Value used as error code on stack dump, can be used to identify stack location on stack unwind. */

#ifdef BLE_DFU_APP_SUPPORT
#define DFU_REV_MAJOR                    0x00                                       /** DFU Major revision number to be exposed. */
#define DFU_REV_MINOR                    0x01                                       /** DFU Minor revision number to be exposed. */
#define DFU_REVISION                     ((DFU_REV_MAJOR << 8) | DFU_REV_MINOR)     /** DFU Revision number to be exposed. Combined of major and minor versions. */
#define APP_SERVICE_HANDLE_START         0x000C                                     /**< Handle of first application specific service when when service changed characteristic is present. */
#define BLE_HANDLE_MAX                   0xFFFF                                     /**< Max handle value in BLE. */

STATIC_ASSERT(IS_SRVC_CHANGED_CHARACT_PRESENT); /** When having DFU Service support in application the Service Changed Characteristic should always be present. */
#endif // BLE_DFU_APP_SUPPORT


static uint16_t m_conn_handle = BLE_CONN_HANDLE_INVALID; /**< Handle of the current connection. */
static volatile ble_rcmon_data_t m_rcmon_data;

static volatile ble_rcmon_config_t m_rcmon_config = {
    .has_accelerometer = true,
    .num_cells = 2,
    .version = 1
};

static ble_rcmon_t m_rcmon;

APP_TIMER_DEF(m_battery_timer_id);
APP_TIMER_DEF(ads1115_timer_id);
APP_TIMER_DEF(adxl345_timer_id);

static nrf_drv_twi_t m_twi = NRF_DRV_TWI_INSTANCE(0);
static dm_application_instance_t m_app_handle; /**< Application identifier allocated by device manager */

static ads1115_config_t ads_config = {
    .twi = &m_twi,
    .addr_pin = ADS1115_ADDR_PIN_GND,
    .config_reg =
    {
        .os = ADS1115_OS_DEFAULT,
        .mux = ADS1115_MUX_A0,
        .pga = ADS1115_PGA_1,
        .mode = ADS1115_MODE_SINGLE,
        .data_rate = ADS1115_DATA_RATE_DEFAULT,
        .comparator_mode = ADS1115_COMP_MODE_DEFAULT,
        .comparator_pol = ADS1115_COMP_POL_DEFAULT,
        .comparator_lat = ADS1115_COMP_LAT_DEFAULT,
        .comparator_que = ADS1115_COMP_QUE_DISABLE
    },
    .alert_rdy_mode = ADS1115_ALERT_RDY_RDY,
    .alert_rdy_pin = 4,
};


static ble_uuid_t m_adv_uuids[] = {
    {RCMON_UUID_SERVICE, BLE_UUID_TYPE_BLE},
    {BLE_UUID_BATTERY_SERVICE, BLE_UUID_TYPE_BLE},
    {BLE_UUID_DEVICE_INFORMATION_SERVICE, BLE_UUID_TYPE_BLE}
}; /**< Universally unique service identifiers. */
#ifdef BLE_DFU_APP_SUPPORT    
static ble_dfu_t m_dfus; /**< Structure used to identify the DFU service. */
#endif // BLE_DFU_APP_SUPPORT    

/**@brief Callback function for asserts in the SoftDevice.
 *
 * @details This function will be called in case of an assert in the SoftDevice.
 *
 * @warning This handler is an example only and does not fit a final product. You need to analyze
 *          how your product is supposed to react in case of Assert.
 * @warning On assert from the SoftDevice, the system can only recover on reset.
 *
 * @param[in] line_num   Line number of the failing ASSERT call.
 * @param[in] file_name  File name of the failing ASSERT call.
 */
void assert_nrf_callback(uint16_t line_num, const uint8_t * p_file_name) {
    app_error_handler(DEAD_BEEF, line_num, p_file_name);
}

void adxl345_timer_schedule_func(void * p_context) {
    uint32_t err_code = NRF_SUCCESS;

    //LOG("[ADXL345]     reading adxl  1");

    int16_t data[3];
    err_code = adxl345_read_values(&m_twi, data);
    APP_ERROR_CHECK(err_code);

    m_rcmon_data.acc_x = data[0];
    m_rcmon_data.acc_y = data[1];
    m_rcmon_data.acc_z = data[2];

    LOG("received adxl %d %d %d", data[0], data[1], data[2]);
}

void ads1115_timer_schedule_func(void * p_context) {
    uint32_t err_code = NRF_SUCCESS;
    static uint8_t current_channel = 0;
    //LOG("[ADS1115]    reading adc");

    int16_t value;
    //LOG("[ADS1115]    value from adc channel %d value %d", current_channel, value);


    switch (current_channel) {
        case 0:
            err_code = ads1115_read_channel_sync(&ads_config, ADS1115_PGA_1, ADS1115_MUX_A0_A1, &value);
            m_rcmon_data.vcell[0] = value;
            current_channel = 1;
            break;
        case 1:
            err_code = ads1115_read_channel_sync(&ads_config, ADS1115_PGA_1, ADS1115_MUX_A1, &value);
            m_rcmon_data.vcell[1] = value;
            current_channel = 2;
            break;
        case 2:
            err_code = ads1115_read_channel_sync(&ads_config, ADS1115_PGA_16, ADS1115_MUX_A2_A3, &value);
            m_rcmon_data.current = value;
            current_channel = 0;
            break;
        default:
            err_code = NRF_ERROR_INVALID_STATE;
            LOG("[ADS1115] unhandled current channel");
    }
    APP_ERROR_CHECK(err_code);

}

static void timers_init(void) {
    uint32_t err_code = NRF_SUCCESS;
    APP_TIMER_INIT(APP_TIMER_PRESCALER, APP_TIMER_OP_QUEUE_SIZE, false);
    // TODO: remove this timer
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for the GAP initialization.
 *
 * @details This function sets up all the necessary GAP (Generic Access Profile) parameters of the
 *          device including the device name, appearance, and the preferred connection parameters.
 */
static void gap_params_init(void) {
    uint32_t err_code;
    ble_gap_conn_params_t gap_conn_params;
    ble_gap_conn_sec_mode_t sec_mode;

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&sec_mode);

    err_code = sd_ble_gap_device_name_set(&sec_mode,
            (const uint8_t *) DEVICE_NAME,
            strlen(DEVICE_NAME));
    APP_ERROR_CHECK(err_code);

    err_code = sd_ble_gap_appearance_set(BLE_APPEARANCE_HEART_RATE_SENSOR_HEART_RATE_BELT);
    APP_ERROR_CHECK(err_code);

    memset(&gap_conn_params, 0, sizeof (gap_conn_params));

    gap_conn_params.min_conn_interval = MIN_CONN_INTERVAL;
    gap_conn_params.max_conn_interval = MAX_CONN_INTERVAL;
    gap_conn_params.slave_latency = SLAVE_LATENCY;
    gap_conn_params.conn_sup_timeout = CONN_SUP_TIMEOUT;

    err_code = sd_ble_gap_ppcp_set(&gap_conn_params);
    APP_ERROR_CHECK(err_code);
}


#ifdef BLE_DFU_APP_SUPPORT

static void advertising_stop(void) {
    uint32_t err_code;

    err_code = sd_ble_gap_adv_stop();
    APP_ERROR_CHECK(err_code);

    err_code = bsp_indication_set(BSP_INDICATE_IDLE);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for loading application-specific context after establishing a secure connection.
 *
 * @details This function will load the application context and check if the ATT table is marked as 
 *          changed. If the ATT table is marked as changed, a Service Changed Indication
 *          is sent to the peer if the Service Changed CCCD is set to indicate.
 *
 * @param[in] p_handle The Device Manager handle that identifies the connection for which the context 
 *                     should be loaded.
 */
static void app_context_load(dm_handle_t const * p_handle) {
    uint32_t err_code;
    static uint32_t context_data;
    dm_application_context_t context;

    context.len = sizeof (context_data);
    context.p_data = (uint8_t *) & context_data;

    err_code = dm_application_context_get(p_handle, &context);
    if (err_code == NRF_SUCCESS) {
        // Send Service Changed Indication if ATT table has changed.
        if ((context_data & (DFU_APP_ATT_TABLE_CHANGED << DFU_APP_ATT_TABLE_POS)) != 0) {
            err_code = sd_ble_gatts_service_changed(m_conn_handle, APP_SERVICE_HANDLE_START, BLE_HANDLE_MAX);
            if ((err_code != NRF_SUCCESS) &&
                    (err_code != BLE_ERROR_INVALID_CONN_HANDLE) &&
                    (err_code != NRF_ERROR_INVALID_STATE) &&
                    (err_code != BLE_ERROR_NO_TX_BUFFERS) &&
                    (err_code != NRF_ERROR_BUSY) &&
                    (err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING)) {
                APP_ERROR_HANDLER(err_code);
            }
        }

        err_code = dm_application_context_delete(p_handle);
        APP_ERROR_CHECK(err_code);
    } else if (err_code == DM_NO_APP_CONTEXT) {
        // No context available. Ignore.
    } else {
        APP_ERROR_HANDLER(err_code);
    }
}


/** @snippet [DFU BLE Reset prepare] */

/**@brief Function for preparing for system reset.
 *
 * @details This function implements @ref dfu_app_reset_prepare_t. It will be called by 
 *          @ref dfu_app_handler.c before entering the bootloader/DFU.
 *          This allows the current running application to shut down gracefully.
 */
static void reset_prepare(void) {
    uint32_t err_code;

    if (m_conn_handle != BLE_CONN_HANDLE_INVALID) {
        // Disconnect from peer.
        err_code = sd_ble_gap_disconnect(m_conn_handle, BLE_HCI_REMOTE_USER_TERMINATED_CONNECTION);
        APP_ERROR_CHECK(err_code);
        err_code = bsp_indication_set(BSP_INDICATE_IDLE);
        APP_ERROR_CHECK(err_code);
    } else {
        // If not connected, the device will be advertising. Hence stop the advertising.
        advertising_stop();
    }

    err_code = ble_conn_params_stop();
    APP_ERROR_CHECK(err_code);

    nrf_delay_ms(500);
}
/** @snippet [DFU BLE Reset prepare] */
#endif // BLE_DFU_APP_SUPPORT

/**@brief Function for initializing services that will be used by the application.
 *
 * @details Initialize the Heart Rate, Battery and Device Information services.
 */
static void services_init(void) {
    uint32_t err_code;
    ble_dis_init_t dis_init;

    // Initialize Device Information Service.
    memset(&dis_init, 0, sizeof (dis_init));

    ble_srv_ascii_to_utf8(&dis_init.manufact_name_str, (char *) MANUFACTURER_NAME);

    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&dis_init.dis_attr_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_NO_ACCESS(&dis_init.dis_attr_md.write_perm);

    err_code = ble_dis_init(&dis_init);
    APP_ERROR_CHECK(err_code);

#ifdef BLE_DFU_APP_SUPPORT
    /** @snippet [DFU BLE Service initialization] */
    ble_dfu_init_t dfus_init;

    // Initialize the Device Firmware Update Service.
    memset(&dfus_init, 0, sizeof (dfus_init));

    dfus_init.evt_handler = dfu_app_on_dfu_evt;
    dfus_init.error_handler = NULL;
    dfus_init.evt_handler = dfu_app_on_dfu_evt;
    dfus_init.revision = DFU_REVISION;

    err_code = ble_dfu_init(&m_dfus, &dfus_init);
    APP_ERROR_CHECK(err_code);

    dfu_app_reset_prepare_set(reset_prepare);
    dfu_app_dm_appl_instance_set(m_app_handle);
    /** @snippet [DFU BLE Service initialization] */
#endif // BLE_DFU_APP_SUPPORT

    LOG("initializing rcmon");

    ble_rcmon_init_t rcmon_init;
    memset(&rcmon_init, 0, sizeof (rcmon_init));

    rcmon_init.pdata = &m_rcmon_data;
    err_code = ble_rcmon_init(&m_rcmon, &rcmon_init);
    APP_ERROR_CHECK(err_code);
}

static void create_app_timers(void) {
    uint32_t err_code;
    err_code = app_timer_create(&ads1115_timer_id, APP_TIMER_MODE_REPEATED, ads1115_timer_schedule_func);
    APP_ERROR_CHECK(err_code);

    err_code = app_timer_create(&adxl345_timer_id, APP_TIMER_MODE_REPEATED, adxl345_timer_schedule_func);
    APP_ERROR_CHECK(err_code);

}

/**@brief Function for starting application timers.
 */
static void application_timers_start(void) {
    uint32_t err_code;

    // Start application timers.
    err_code = app_timer_create(&ads1115_timer_id, APP_TIMER_MODE_REPEATED, ads1115_timer_schedule_func);
    //app_timer_start(ads1115_timer_id, ADS1115_MEAS_INTERVAL, NULL);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for handling the Connection Parameters Module.
 *
 * @details This function will be called for all events in the Connection Parameters Module which
 *          are passed to the application.
 *          @note All this function does is to disconnect. This could have been done by simply
 *                setting the disconnect_on_fail config parameter, but instead we use the event
 *                handler mechanism to demonstrate its use.
 *
 * @param[in] p_evt  Event received from the Connection Parameters Module.
 */
static void on_conn_params_evt(ble_conn_params_evt_t * p_evt) {
    uint32_t err_code;

    if (p_evt->evt_type == BLE_CONN_PARAMS_EVT_FAILED) {
        err_code = sd_ble_gap_disconnect(m_conn_handle, BLE_HCI_CONN_INTERVAL_UNACCEPTABLE);
        APP_ERROR_CHECK(err_code);
    }
}

/**@brief Function for handling a Connection Parameters error.
 *
 * @param[in] nrf_error  Error code containing information about what went wrong.
 */
static void conn_params_error_handler(uint32_t nrf_error) {
    APP_ERROR_HANDLER(nrf_error);
}

/**@brief Function for initializing the Connection Parameters module.
 */
static void conn_params_init(void) {
    uint32_t err_code;
    ble_conn_params_init_t cp_init;

    memset(&cp_init, 0, sizeof (cp_init));

    cp_init.p_conn_params = NULL;
    cp_init.first_conn_params_update_delay = FIRST_CONN_PARAMS_UPDATE_DELAY;
    cp_init.next_conn_params_update_delay = NEXT_CONN_PARAMS_UPDATE_DELAY;
    cp_init.max_conn_params_update_count = MAX_CONN_PARAMS_UPDATE_COUNT;
    //cp_init.start_on_notify_cccd_handle    = m_hrs.hrm_handles.cccd_handle;
    cp_init.start_on_notify_cccd_handle = false;
    cp_init.disconnect_on_fail = false;
    cp_init.evt_handler = on_conn_params_evt;
    cp_init.error_handler = conn_params_error_handler;

    LOG("conn init");
    err_code = ble_conn_params_init(&cp_init);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for putting the chip into sleep mode.
 *
 * @note This function will not return.
 */
static void sleep_mode_enter(void) {
    uint32_t err_code = bsp_indication_set(BSP_INDICATE_IDLE);
    APP_ERROR_CHECK(err_code);

    // Prepare wakeup buttons.
    err_code = bsp_btn_ble_sleep_mode_prepare();
    APP_ERROR_CHECK(err_code);

    // Go to system-off mode (this function will not return; wakeup will cause a reset).
    err_code = sd_power_system_off();
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for handling advertising events.
 *
 * @details This function will be called for advertising events which are passed to the application.
 *
 * @param[in] ble_adv_evt  Advertising event.
 */
static void on_adv_evt(ble_adv_evt_t ble_adv_evt) {
    uint32_t err_code;

    switch (ble_adv_evt) {
        case BLE_ADV_EVT_FAST:
            err_code = bsp_indication_set(BSP_INDICATE_ADVERTISING);
            APP_ERROR_CHECK(err_code);
            break;
        case BLE_ADV_EVT_IDLE:
            sleep_mode_enter();
            break;
        default:
            break;
    }
}

/**@brief Function for handling the Application's BLE Stack events.
 *
 * @param[in] p_ble_evt  Bluetooth stack event.
 */
static void on_ble_evt(ble_evt_t * p_ble_evt) {
    uint32_t err_code;

    switch (p_ble_evt->header.evt_id) {
        case BLE_GAP_EVT_CONNECTED:
            err_code = bsp_indication_set(BSP_INDICATE_CONNECTED);
            APP_ERROR_CHECK(err_code);
            m_conn_handle = p_ble_evt->evt.gap_evt.conn_handle;
            break;

        case BLE_GAP_EVT_DISCONNECTED:
            m_conn_handle = BLE_CONN_HANDLE_INVALID;
            break;

        default:
            // No implementation needed.
            break;
    }
}

/**@brief Function for dispatching a BLE stack event to all modules with a BLE stack event handler.
 *
 * @details This function is called from the BLE Stack event interrupt handler after a BLE stack
 *          event has been received.
 *
 * @param[in] p_ble_evt  Bluetooth stack event.
 */
static void ble_evt_dispatch(ble_evt_t * p_ble_evt) {
    dm_ble_evt_handler(p_ble_evt);
    ble_rcmon_on_ble_evt(&m_rcmon, p_ble_evt);

    ble_conn_params_on_ble_evt(p_ble_evt);
    bsp_btn_ble_on_ble_evt(p_ble_evt);
#ifdef BLE_DFU_APP_SUPPORT
    /** @snippet [Propagating BLE Stack events to DFU Service] */
    ble_dfu_on_ble_evt(&m_dfus, p_ble_evt);
    /** @snippet [Propagating BLE Stack events to DFU Service] */
#endif // BLE_DFU_APP_SUPPORT

    on_ble_evt(p_ble_evt);
    ble_advertising_on_ble_evt(p_ble_evt);
}

/**@brief Function for dispatching a system event to interested modules.
 *
 * @details This function is called from the System event interrupt handler after a system
 *          event has been received.
 *
 * @param[in] sys_evt  System stack event.
 */
static void sys_evt_dispatch(uint32_t sys_evt) {
    pstorage_sys_event_handler(sys_evt);
    ble_advertising_on_sys_evt(sys_evt);
}

/**@brief Function for initializing the BLE stack.
 *
 * @details Initializes the SoftDevice and the BLE event interrupt.
 */
static void ble_stack_init(void) {
    uint32_t err_code;

    // Initialize the SoftDevice handler module.
    SOFTDEVICE_HANDLER_INIT(NRF_CLOCK_LFCLKSRC_XTAL_20_PPM, NULL);

#if defined(S110) || defined(S130) || defined(S310)  || defined(S132)
    // Enable BLE stack.
    ble_enable_params_t ble_enable_params;
    memset(&ble_enable_params, 0, sizeof (ble_enable_params));
#if defined(S130) || defined(S310) || defined(S132)
    ble_enable_params.gatts_enable_params.attr_tab_size = BLE_GATTS_ATTR_TAB_SIZE_DEFAULT;
#endif
    ble_enable_params.gatts_enable_params.service_changed = IS_SRVC_CHANGED_CHARACT_PRESENT;
    err_code = sd_ble_enable(&ble_enable_params);
    APP_ERROR_CHECK(err_code);
#endif

    // Register with the SoftDevice handler module for BLE events.
    err_code = softdevice_ble_evt_handler_set(ble_evt_dispatch);
    APP_ERROR_CHECK(err_code);

    // Register with the SoftDevice handler module for BLE events.
    err_code = softdevice_sys_evt_handler_set(sys_evt_dispatch);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for handling events from the BSP module.
 *
 * @param[in]   event   Event generated by button press.
 */
void bsp_event_handler(bsp_event_t event) {
    uint32_t err_code;
    switch (event) {
        case BSP_EVENT_SLEEP:
            sleep_mode_enter();
            break;

        case BSP_EVENT_DISCONNECT:
            err_code = sd_ble_gap_disconnect(m_conn_handle, BLE_HCI_REMOTE_USER_TERMINATED_CONNECTION);
            if (err_code != NRF_ERROR_INVALID_STATE) {
                APP_ERROR_CHECK(err_code);
            }
            break;

        case BSP_EVENT_WHITELIST_OFF:
            err_code = ble_advertising_restart_without_whitelist();
            if (err_code != NRF_ERROR_INVALID_STATE) {
                APP_ERROR_CHECK(err_code);
            }
            break;

        default:
            break;
    }
}

/**@brief Function for handling the Device Manager events.
 *
 * @param[in] p_evt  Data associated to the device manager event.
 */
static uint32_t device_manager_evt_handler(dm_handle_t const * p_handle,
        dm_event_t const * p_event,
        ret_code_t event_result) {
    APP_ERROR_CHECK(event_result);

#ifdef BLE_DFU_APP_SUPPORT
    if (p_event->event_id == DM_EVT_LINK_SECURED) {
        app_context_load(p_handle);
    }
#endif // BLE_DFU_APP_SUPPORT

    return NRF_SUCCESS;
}

/**@brief Function for the Device Manager initialization.
 *
 * @param[in] erase_bonds  Indicates whether bonding information should be cleared from
 *                         persistent storage during initialization of the Device Manager.
 */
static void device_manager_init(bool erase_bonds) {
    uint32_t err_code;
    dm_init_param_t init_param = {.clear_persistent_data = erase_bonds};
    dm_application_param_t register_param;

    // Initialize persistent storage module.
    err_code = pstorage_init();
    APP_ERROR_CHECK(err_code);

    err_code = dm_init(&init_param);
    APP_ERROR_CHECK(err_code);

    memset(&register_param.sec_param, 0, sizeof (ble_gap_sec_params_t));

    register_param.sec_param.bond = SEC_PARAM_BOND;
    register_param.sec_param.mitm = SEC_PARAM_MITM;
    register_param.sec_param.io_caps = SEC_PARAM_IO_CAPABILITIES;
    register_param.sec_param.oob = SEC_PARAM_OOB;
    register_param.sec_param.min_key_size = SEC_PARAM_MIN_KEY_SIZE;
    register_param.sec_param.max_key_size = SEC_PARAM_MAX_KEY_SIZE;
    register_param.evt_handler = device_manager_evt_handler;
    register_param.service_type = DM_PROTOCOL_CNTXT_GATT_SRVR_ID;

    err_code = dm_register(&m_app_handle, &register_param);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for initializing the Advertising functionality.
 */
static void advertising_init(void) {
    uint32_t err_code;
    ble_advdata_t advdata;

    // Build advertising data struct to pass into @ref ble_advertising_init.
    memset(&advdata, 0, sizeof (advdata));

    advdata.name_type = BLE_ADVDATA_FULL_NAME;
    advdata.include_appearance = true;
    advdata.flags = BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE;
    advdata.uuids_complete.uuid_cnt = sizeof (m_adv_uuids) / sizeof (m_adv_uuids[0]);
    advdata.uuids_complete.p_uuids = m_adv_uuids;

    ble_adv_modes_config_t options = {0};
    options.ble_adv_fast_enabled = BLE_ADV_FAST_ENABLED;
    options.ble_adv_fast_interval = APP_ADV_INTERVAL;
    options.ble_adv_fast_timeout = APP_ADV_TIMEOUT_IN_SECONDS;

    err_code = ble_advertising_init(&advdata, NULL, &options, on_adv_evt, NULL);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for initializing buttons and leds.
 *
 * @param[out] p_erase_bonds  Will be true if the clear bonding button was pressed to wake the application up.
 */
static void buttons_leds_init(bool * p_erase_bonds) {
    bsp_event_t startup_event;

    uint32_t err_code = bsp_init(BSP_INIT_LED | BSP_INIT_BUTTONS,
            APP_TIMER_TICKS(100, APP_TIMER_PRESCALER),
            bsp_event_handler);
    APP_ERROR_CHECK(err_code);

    err_code = bsp_btn_ble_init(NULL, &startup_event);
    APP_ERROR_CHECK(err_code);

    *p_erase_bonds = (startup_event == BSP_EVENT_CLEAR_BONDING_DATA);
}

/**@brief Function for the Power manager.
 */
static void power_manage(void) {
    uint32_t err_code = sd_app_evt_wait();
    APP_ERROR_CHECK(err_code);
}

/**
 * @brief ADC interrupt handler.
 */
void ADC_IRQHandler(void) {
    nrf_adc_conversion_event_clean();
    //    int16_t adc_sample = nrf_adc_result_get();
    //    nrf_adc_start();
}

/**
 * @brief ADC initialization.
 */
void adc_config(void) {
    const nrf_adc_config_t nrf_adc_config = {
        .resolution = NRF_ADC_CONFIG_RES_10BIT,
        .scaling = NRF_ADC_CONFIG_SCALING_INPUT_ONE_THIRD,
        .reference = NRF_ADC_CONFIG_REF_VBG
    };

    // Initialize and configure ADC
    nrf_adc_configure((nrf_adc_config_t *) & nrf_adc_config);
    nrf_adc_input_select(NRF_ADC_CONFIG_INPUT_6);
    nrf_adc_int_enable(ADC_INTENSET_END_Enabled << ADC_INTENSET_END_Pos);

    NVIC_SetPriority(ADC_IRQn, NRF_APP_PRIORITY_HIGH);
    NVIC_EnableIRQ(ADC_IRQn);
}

#ifdef ADS1115_IQR_DEV

static void on_conversion_done(enum ads1115_mux_t channel, int16_t value) {
    LOG("conversion done, value=%f", value);
    switch (channel) {
        case ADS1115_MUX_A0:
            ads1115_read_channel_async(&ads_config, ADS1115_MUX_A1);
            break;
        case ADS1115_MUX_A1:
            ads1115_read_channel_async(&ads_config, ADS1115_MUX_A2);
            break;
        case ADS1115_MUX_A2:
            ads1115_read_channel_async(&ads_config, ADS1115_MUX_A0);
            break;
        default:
            break;
    }
}
#endif

static ret_code_t twi_config() {
    ret_code_t err_code;

    LOG("initializing twi");
    err_code = nrf_drv_twi_init(&m_twi, NULL, NULL, NULL);
    APP_ERROR_CHECK(err_code);
    nrf_drv_twi_enable(&m_twi);
    LOG("initialized twi");

    err_code = adxl345_init(&m_twi);
    if (err_code == NRF_SUCCESS) {
        m_rcmon_config.has_accelerometer = 1;
        LOG("ADXL345 connection successful");
        err_code = app_timer_start(adxl345_timer_id, ADXL345_MEAS_INTERVAL, NULL);
        APP_ERROR_CHECK(err_code);
    } else {
        LOG("ADXL345 connection failed");
        m_rcmon_config.has_accelerometer = 0;
    }


    err_code = ads1115_init(&ads_config);
    if (err_code == NRF_SUCCESS) {
        LOG("ADS1115 connection successful");
        err_code = app_timer_start(ads1115_timer_id, ADS1115_MEAS_INTERVAL, NULL);
        APP_ERROR_CHECK(err_code);
    } else {
        LOG("ADS1115 connection failed");
    }

    return NRF_SUCCESS;
}

void app_error_handler(uint32_t error_code, uint32_t line_num, const uint8_t * p_file_name) {
    nrf_delay_ms(50);
    LOG("APP ERROR: error code %d line %d file %s",
            error_code,
            line_num,
            p_file_name ? p_file_name : (const uint8_t *) "n/a");

    for (int i = 0; i < 10; i++) {
        nrf_delay_ms(50);
    }
    LOG("RESET!");
    nrf_delay_ms(50);

    NVIC_SystemReset();
}

/**@brief Function for application main entry.
 */
int main(void) {
    uint32_t err_code = NRF_SUCCESS;
    bool erase_bonds;

    // Initialize.

    app_trace_init();

    // wait for pc to open modem to get full debug output
    for (int i = 0; i < 10; i++) nrf_delay_ms(100);



    LOG("tracing inited");


    timers_init();
    create_app_timers();
    buttons_leds_init(&erase_bonds);
    ble_stack_init();
    device_manager_init(erase_bonds);

    gap_params_init();
    advertising_init();
    services_init();
    conn_params_init();

    //adc_config();
    twi_config();


    LOG("before timers");
    // Start execution.
    //application_timers_start();

    LOG("before adv");
    err_code = ble_advertising_start(BLE_ADV_MODE_FAST);
    APP_ERROR_CHECK(err_code);

    LOG("before adc");
    nrf_adc_start();

    LOG("starting");

    // Enter main loop.
    for (;;) {
        power_manage();
    }
}
