package com.yonglin.pdc.epaper_setting.tasks;

import com.yonglin.pdc.epaper_setting.data.DeviceProfile;

public interface DeviceTaskCallback {
    void onCompleted(String ssid, DeviceProfile profile);
    void onFailure(String ssid, DeviceProfile profile, String error);
    void onMessage(String ssid, DeviceProfile profile, String message);
}
