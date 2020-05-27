/*
 * Copyright (C) 2019 Texas Instruments Incorporated - http://www.ti.com/
 *
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *    Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 *    Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.yonglin.pdc.epaper_setting.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.utils.ConversionUtil;
import com.yonglin.pdc.epaper_setting.utils.NetworkUtil;

import java.util.ArrayList;

public class GetDeviceConfigAsyncTask extends AsyncTask<ArrayList<Object>, Void, Boolean> {
    private static final String TAG = "GetDeviceConfigTask"; // max tag length is 23

    private DeviceTaskCallback mTaskCallback;

    public GetDeviceConfigAsyncTask(DeviceTaskCallback callBack) {
        mTaskCallback = callBack;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Log.d(TAG, "onPost started");
        super.onPostExecute(result);
    }

    @Override
    protected Boolean doInBackground(ArrayList<Object>... params){
        Log.d(TAG, "doInBackground started");
        ArrayList<Object> list = params[0];
//		Context context= (Context)list.get(0);
        String ssid = ConversionUtil.formatSSID((String) list.get(1));
        DeviceProfile profile = (DeviceProfile) list.get(2);
        String deviceIp = (String) list.get(3);

        String deviceConfig = "";
        try {
//            if (Constants.CONFIG_TYPE.equals("TCP")) {
                deviceConfig = NetworkUtil.getDeviceConfig(deviceIp);

                Log.e(TAG, deviceConfig);
//                if (deviceConfig.equals(""))
//					mTaskCallback.onFailure(ssid,profile,"failed to get data");
//                else
//					mTaskCallback.onCompleted(ssid,profile);


//            } else if (Constants.CONFIG_TYPE.equals("HTTP")) {
//                deviceConfig = NetworkUtil.httpGetDeviceConfig(Constants.BASE_URL_NO_HTTP);
//                mTaskCallback.Completed(deviceConfig);
//            }
        } catch (Exception e) {
            e.printStackTrace();
            mTaskCallback.onFailure(ssid,profile,e.getLocalizedMessage());
        }
        return true;
    }

}
