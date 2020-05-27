package com.yonglin.pdc.epaper_setting;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.data.DeviceType;
import com.yonglin.pdc.epaper_setting.data.GlobalData;
import com.yonglin.pdc.epaper_setting.tasks.ConfigDeviceAsyncTask;
import com.yonglin.pdc.epaper_setting.tasks.DeviceTaskCallback;
import com.yonglin.pdc.epaper_setting.tasks.OtaUpdateAsyncTask;
import com.yonglin.pdc.epaper_setting.utils.ConversionUtil;
import com.yonglin.pdc.epaper_setting.data.DeviceData;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.DeviceStatus;
import com.yonglin.pdc.epaper_setting.utils.NetworkUtil;
import com.yonglin.pdc.epaper_setting.data.SecurityType;
import com.yonglin.pdc.epaper_setting.utils.WifiNetworkUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class RunTaskActivity extends AppCompatActivity {
    private static final String TAG = "RunTaskActivity";
    private Context mContext;
    private Button mDoneButton;
    private TextView mLogTextView;

    private DeviceProfile mProfile;
    private Uri mFirmwareUri;
    private String mChecksum;

//    Timer logShowTimerOut;
    public boolean isSettingProcessing;

    boolean isOtaMode = false;
    boolean isReaderMode = false;

    ArrayList<DeviceData> mDeviceList = new ArrayList<>();

    Timer nextTaskTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_run_task);

        setResult(RESULT_CANCELED);

        // get task id from caller
        Intent intent = getIntent();
        final String action = intent.getAction();

        mLogTextView = findViewById(R.id.run_task_log);
        mContext = getBaseContext();
        isSettingProcessing = false;

        if (intent.getExtras() != null) {
            // take input
            mDeviceList = ConversionUtil.deviceListFromJson(intent.getStringExtra(Constants.INTENT_EXTRA_JSON_DEVICE_LIST));
            Log.d(TAG, "Device to work on:" + mDeviceList.size());
            Gson gson = new Gson();
            switch (action) {
                case Constants.INTENT_ACTION_CONFIG_DEVICE:  //Batch configurator
                    mProfile = gson.fromJson(intent.getStringExtra(Constants.INTENT_EXTRA_JSON_DEVICE_PROFILE), DeviceProfile.class);
                    Log.d(TAG, "Profile for Task:\n" + mProfile.toString());
                    break;
                case Constants.INTENT_ACTION_UPLOAD_FIRMWARE: //firmware configurator
                    String uri = intent.getStringExtra(Constants.INTENT_EXTRA_DEVICE_FIRMWARE_URI);
                    mFirmwareUri = Uri.parse(uri);
                    mChecksum = intent.getStringExtra(Constants.INTENT_EXTRA_JSON_DEVICE_FIRMWARE_CHECKSUM);
                    Log.d(TAG, "firmware checksum: " + mChecksum);
                    break;
                default:
            }

        } else {
            Log.d(TAG, "reader mode");
            isReaderMode = true;
        }

        mDoneButton = findViewById(R.id.run_task_done_button);
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isReaderMode) {
                    Intent intent = new Intent();
                    intent.putExtra(Constants.INTENT_EXTRA_JSON_DEVICE_LIST, ConversionUtil.deviceListToJson(mDeviceList));
                    setResult(RESULT_OK, intent);
                }
                finish();
            }
        });

        if (isReaderMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLogTextView.setText(Html.fromHtml(GlobalData.getInstance().getLog(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                mLogTextView.setText(Html.fromHtml(GlobalData.getInstance().getLog()));
            }
        } else {
            mDoneButton.setEnabled(false);
            GlobalData.getInstance().setLog("");
            Toast.makeText(RunTaskActivity.this,
                    RunTaskActivity.this.getString(R.string.work_in_progress_message), Toast.LENGTH_SHORT).show();
            switch (action) {
                case Constants.INTENT_ACTION_CONFIG_DEVICE:
                    // start config devices
                    startTask(mContext.getApplicationContext());
                    break;
                case Constants.INTENT_ACTION_UPLOAD_FIRMWARE:
                    isOtaMode = true;
                    startTask(mContext.getApplicationContext());
                    break;
                default:
                    printLog("No matching action to do.", Color.BLACK, false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (nextTaskTimer != null) {
            nextTaskTimer.cancel();
        }
        super.onDestroy();
    }

    // There could be APs with the same ssid but different security settings, but our devices will
    // be OPEN by default so using ssid alone here is enough for the key.
    public void updateDeviceStatus(final String ssid, final DeviceStatus status) {
        Log.e(TAG, "!! update device status for " + ssid);
        for (DeviceData device : mDeviceList) {

            // ssid names from system wifi configurations will have quotes
            String quotedSsid = "\"" + device.getSsid() + "\"";

            if (device.getSsid().equals(ssid) || quotedSsid.equals(ssid)) {
                Log.d(TAG, "ssid" + ssid + " set to new status: " + status);
                device.setStatus(status);
                break;
            }
        }
    }

    void connectToSSID(final Context context, final String ssid, final SecurityType securityType, final DeviceProfile profile) {
        WifiConfiguration configuration = null;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                printLog("Connecting to " + ssid + "...", Color.BLACK, false);
            }
        });
        WifiNetworkUtils.getInstance(RunTaskActivity.this).clearCallback();
        //20200525 增加firmwre不需要profile變數的判斷
        if (profile == null){
            configuration = NetworkUtil.getWifiConfigurationWithInfo(RunTaskActivity.this,
                    ssid, securityType, null);
        }else {
            configuration = NetworkUtil.getWifiConfigurationWithInfo(RunTaskActivity.this,
                    ssid, securityType, (profile.getDeviceType() == DeviceType.RTM) ? Constants.RTM_WIFI_PASSWORD : null);
        }
        WifiNetworkUtils.getInstance(RunTaskActivity.this).connectToWifi(configuration, RunTaskActivity.this, new WifiNetworkUtils.BitbiteNetworkUtilsCallback() {
            @Override
            public void successfullyConnectedToNetwork(String ssid) {
                printLog("Connection to " + ssid + " successfully.", Color.BLACK, true);
                // add new condition to check if we actually have normal RSSI
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                int info = wifiManager.getConnectionInfo().getRssi();
                Log.i(TAG, "RSSI is : " + info);
                Log.i(TAG, "True RSSI is : " + WifiManager.calculateSignalLevel(info, 5));
                int ipAddress = wifiManager.getDhcpInfo().gateway;
                // 利用位移運算和AND運算計算IP
                String deviceIp = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
                Log.i(TAG, "wifi ip : " + deviceIp);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (isOtaMode) {
                    new OtaUpdateAsyncTask(mOtaUpdateAsyncTaskCallback)
                            .executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mContext, ssid, mFirmwareUri, mChecksum, deviceIp);
                } else {
                    ConfigDeviceAsyncTask task = new ConfigDeviceAsyncTask(mDeviceTaskCallback);
                    task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mContext, ssid, profile, deviceIp);
                }
            }

            @Override
            public void failedToConnectToNetwork(WifiNetworkUtils.WifiConnectionFailure failure) {
                switch (failure) {
                    case Wrong_Password:
                        printLog("Incorrect password for " + ssid, Color.RED, false);
//                        showLoader(false, "");
                        updateDeviceStatus(ssid, DeviceStatus.FAILED);
                        break;
                    case Timeout:
                        printLog("Connection timeout for " + ssid, Color.RED, false);
//                        showLoader(false, "");
                        updateDeviceStatus(ssid, DeviceStatus.FAILED);
                        break;
                    case Connected_To_3G:
                    case Unknown:
                    default:
                        Log.e(TAG, "failedToConnectToNetwork");
                        printLog("Failed to connect to " + ssid, Color.RED, false);
//                        showLoader(false, "");
                        updateDeviceStatus(ssid, DeviceStatus.FAILED);
                        break;
                }
                isSettingProcessing = false;
            }
        });
    }


    private DeviceTaskCallback mDeviceTaskCallback = new DeviceTaskCallback() {
        @Override
        public void onCompleted(String ssid, DeviceProfile profile) {
            Log.d(TAG, "*AP* Wifi Config device completed successfully");
//            showLoader(false, "");
            printLog("Successfully configured device : \"" + ssid + "\"\n", Color.BLUE, false);
            WifiNetworkUtils.getInstance(RunTaskActivity.this).deleteSavedNetworks(ssid);
            isSettingProcessing = false;
            updateDeviceStatus(ssid, DeviceStatus.SUCCESS);
        }

        @Override
        public void onFailure(String ssid, DeviceProfile profile, String error) {
            Log.d(TAG, "*AP* Wifi Config device completed unsuccessfully");
//            showLoader(false, "");
            printLog("Failed to configure device : \"" + ssid + "\"\n", Color.RED, false);
            WifiNetworkUtils.getInstance(RunTaskActivity.this).deleteSavedNetworks(ssid);
            isSettingProcessing = false;
            updateDeviceStatus(ssid, DeviceStatus.FAILED);
        }

        @Override
        public void onMessage(String ssid, DeviceProfile profile, String message) {

        }
    };

    private DeviceTaskCallback mOtaUpdateAsyncTaskCallback = new DeviceTaskCallback() {

        @Override
        public void onCompleted(String ssid, DeviceProfile profile) {
            Log.d(TAG, "*AP* Wifi OTA completed successfully");
            updateDeviceStatus(ssid, DeviceStatus.SUCCESS);
//            showLoader(false, "");
            printLog("Successfully updated firmware for device : \"" + ssid + "\"\n", Color.BLUE, false);
            WifiNetworkUtils.getInstance(RunTaskActivity.this).deleteSavedNetworks(ssid);
            isSettingProcessing = false;
        }

        @Override
        public void onFailure(String ssid, DeviceProfile profile, String error) {
            Log.d(TAG, "*AP* Wifi OTA completed unsuccessfully");
            updateDeviceStatus(ssid, DeviceStatus.FAILED);
//            showLoader(false, "");
            printLog("Failed to update firmware for device : \"" + ssid + "\"\n", Color.RED, false);
            WifiNetworkUtils.getInstance(RunTaskActivity.this).deleteSavedNetworks(ssid);
            isSettingProcessing = false;
        }

        @Override
        public void onMessage(String ssid, DeviceProfile profile, String message) {
            Log.d(TAG, "*AP* Wifi OTA in progress, msg: " + message);
//            showLoader(false, "");
            printLog("message :" + message, Color.BLACK, false);
        }
    };

    @Override
    public void onBackPressed() {
        if (isSettingProcessing) {
            AlertDialog.Builder builder = new AlertDialog.Builder(RunTaskActivity.this);
            View customView = LayoutInflater.from(RunTaskActivity.this).inflate(R.layout.dialog_message, (ViewGroup) findViewById(android.R.id.content), false);
            final MaterialTextView msgTextView = customView.findViewById(R.id.dialog_text_view);
            msgTextView.setText(R.string.dialog_confirm_stop_running_task);
            builder.setTitle(R.string.dialog_confirm_title);
            builder.setView(customView);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        } else {
            super.onBackPressed();
        }
    }

    public void printLog(String str, int color, boolean isDetail) {
        String colorStr = Integer.toString(color, 16);
        SimpleDateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        String time = sDateFormat.format(new java.util.Date());

        final String formattedStr = String.format("[%s] <font color=\"#%s\">%s</font><br>", time, colorStr, str);
        final String newLog = GlobalData.getInstance().getLog() + formattedStr;
        GlobalData.getInstance().setLog(newLog);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView log = findViewById(R.id.run_task_log);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    log.setText(Html.fromHtml(newLog, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    log.setText(Html.fromHtml(newLog));
                }
            }
        });
    }

//    public void showLoader(boolean show, final String text) {
//        if (show) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
////                    ((RelativeLayout) findViewById(R.id.device_configuration_loader_layout)).setVisibility(View.VISIBLE);
////                    ((TextView) findViewById(R.id.device_configuration_loader_label)).setText(text);
//                    Log.e("showLoader", text);
//                }
//            });
//
//            //如果loader畫面顯示過久，則關閉loader畫面
//            if (logShowTimerOut != null)
//                logShowTimerOut.cancel();
//
//            int connectTimeOut;
//            if (isOtaMode)
//                connectTimeOut = 60000;
//            else
//                connectTimeOut = 5000;
//
//            logShowTimerOut = new Timer();
//            logShowTimerOut.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    showLoader(false, "");
//                }
//            }, WifiNetworkUtils.CONNECTION_TIMEOUT + connectTimeOut);
//        } else {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
////                    ((RelativeLayout) findViewById(R.id.device_configuration_loader_layout)).setVisibility(View.GONE);
////                    ((TextView) findViewById(R.id.device_configuration_loader_label)).setText(text);
//                    Log.e("showLoader", text);
//                }
//            });
//            if (logShowTimerOut != null)
//                logShowTimerOut.cancel();
//        }
//    }

    //  launch task one by one to get by the concurrent issue with getWifiConfigurationWithInfo
    public void startTask(final Context appContext) {
        Log.d(TAG, "Heartbeat: task busy state: " + isSettingProcessing);
        boolean isFinished = false;
        // do the work
        if (isSettingProcessing == false) {
            isFinished = true;
            for (DeviceData device : mDeviceList) {
                if (device.isSelected() && device.getStatus() == DeviceStatus.NONE) {
                    device.setStatus(DeviceStatus.QUEUED);
                    isSettingProcessing = true;
                    Log.d(TAG, "startTask profile: " + mProfile);
                    if(mProfile != null) {
                        connectToSSID(appContext, device.getSsid(), device.getSecurityType(), mProfile);
                    }
                    connectToSSID(appContext, device.getSsid(), device.getSecurityType(), null);
                    // process one item at a time
                    isFinished = false;
                    break;
                }
            }
            // if no more items to work on
            if (isFinished && nextTaskTimer != null) {
                nextTaskTimer.cancel();
            }
        }

        if (isFinished) {
            if (nextTaskTimer != null) {
                nextTaskTimer.cancel();
            }
            Log.e(TAG, "Finished, killing timer");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("All tasks finished.", Color.BLACK, false);
                    mDoneButton.setEnabled(true);
                }
            });
        } else {
            // schedule next timer
            nextTaskTimer = new Timer();
            nextTaskTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    startTask(appContext);
                }
            }, 5000);
        }
    }
}

