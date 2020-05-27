package com.yonglin.pdc.epaper_setting.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.DeviceType;
import com.yonglin.pdc.epaper_setting.utils.ConversionUtil;
import com.yonglin.pdc.epaper_setting.utils.NetworkUtil;

public class ConfigDeviceAsyncTask extends AsyncTask<Object, Void, Boolean> {

    private static final String TAG = "ConfigDeviceAsyncTask";
    private DeviceTaskCallback mCallback;

    public ConfigDeviceAsyncTask(DeviceTaskCallback callBack) {
        mCallback = callBack;
    }


    @Override
    protected Boolean doInBackground(Object... params) {
        Log.d(TAG, "doInBackground started");
        Context context = (Context) params[0];
        String ssid = ConversionUtil.formatSSID((String) params[1]);
        DeviceProfile profile = (DeviceProfile) params[2];
        String deviceIp = (String) params[3];

        if(profile.getDeviceType()== DeviceType.RTM){
            if (NetworkUtil.configRtmDevice( profile, deviceIp)) {
                mCallback.onCompleted(ssid, profile);
                return true;
            } else {
                mCallback.onFailure(ssid, profile, null);
                return false;
            }
        }
        else { // EPD/PID
            if (NetworkUtil.configDevice(context, ssid, profile, deviceIp)) {
                mCallback.onCompleted(ssid, profile);
                return true;
            } else {
                mCallback.onFailure(ssid, profile, null);
                return false;
            }
        }
    }
}
