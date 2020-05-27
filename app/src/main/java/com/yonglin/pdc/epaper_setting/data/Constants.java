package com.yonglin.pdc.epaper_setting.data;

public interface Constants {
    int TCP_SERVER_PORT = 9000;
    int RTM_TCP_SERVER_PORT = 8888;
    String RTM_WIFI_PASSWORD = "12345678";
    String RTM_MQTT_TOPIC = "EQM_TEST";
    String RTM_MQTT_USERNAME = "EQM166";
    String RTM_MQTT_PASSWORD = "eqm166";
    String RTM_SET_CONFIG_OK = "SetConfig 0";
    int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    int PERMISSION_REQUEST_STORAGE = 2;
    // reuse the same profile folder for single device run
//    String ONE_TIME_PROFILE_ID = "4f9a8f02-cbaf-487a-b145-3e7ddd9e6b08";
    String ONE_TIME_PROFILE_ID =   "11111111-2222-3333-4444-555555555555";

    // intent
    int INTENT_REQUEST_CODE_PICK_CA_CERT_FILE = 101;
    int INTENT_REQUEST_CODE_PICK_CLIENT_CERT_FILE = 102;
    int INTENT_REQUEST_CODE_PICK_PRIVATE_KEY_FILE = 103;
    int INTENT_REQUEST_CODE_PICK_FIRMWARE_FILE = 104;
    int INTENT_REQUEST_CODE_PICK_SSID = 200;
    int INTENT_REQUEST_CODE_PICK_DEVICE_SSID = 201;
    String INTENT_EXTRA_SSID = "SSID";
    String INTENT_EXTRA_DEVICE_PREFIX = "DEVICE_PREFIX";

    // intent for tasks
    int INTENT_REQUEST_CODE_TASK_BATCH_CONFIG_DEVICE = 202;
    int INTENT_REQUEST_CODE_TASK_FIRMWARE_DEVICE = 203;
    String INTENT_ACTION_CONFIG_DEVICE = "ACTION_CONFIG_DEVICE";
    String INTENT_ACTION_UPLOAD_FIRMWARE = "ACTION_UPLOAD_FIRMWARE";
    String INTENT_EXTRA_JSON_DEVICE_LIST = "DeviceList";
    String INTENT_EXTRA_JSON_DEVICE_PROFILE = "DeviceProfile";
    String INTENT_EXTRA_DEVICE_FIRMWARE_URI = "DeviceFirmwareFile";
    String INTENT_EXTRA_JSON_DEVICE_FIRMWARE_CHECKSUM = "DeviceFirmwareChecksum";

    // fragments
    String FRAGMENT_DEVICE_CONFIGURATION = "fragmentDeviceConfiguration";
    String FRAGMENT_FIRMWARE = "fragmentFirmware";
    String FRAGMENT_PROFILE = "fragmentProfile";
    String FRAGMENT_BATCH_CONFIGURATION = "fragmentConfiguration";
//    String FRAGMENT_ABOUT = "fragmentAbout";

    // Max file size 40K for Certs
    long MAX_FILE_SIZE = 40 * 1024;

    // file commands
    byte CMD_UPLOAD_CA_CERT = (byte) 0xB0;
    byte CMD_UPLOAD_CLIENT_CERT = (byte) 0xB1;
    byte CMD_UPLOAD_PRIVATE_KEY = (byte) 0xB2;

    // TODO the prefix for dev device is PID but it should be EPD78_/EPD97_/WT_ later.
    // FIXME For demo, we added one more "EPD" here to support more than one prefixes per type.
    String[] prefixArray = new String[]{"EPD", "EPD", "RTM", "PID"}; //20200522 砍掉一個EPD程式會閃退
}
