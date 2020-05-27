package com.yonglin.pdc.epaper_setting;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textview.MaterialTextView;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.GlobalData;
import com.yonglin.pdc.epaper_setting.fragments.BatchConfigurationFragment;
import com.yonglin.pdc.epaper_setting.fragments.DeviceConfigurationFragment;
import com.yonglin.pdc.epaper_setting.fragments.FirmwareFragment;
import com.yonglin.pdc.epaper_setting.fragments.ProfileFragment;
import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.utils.FileUtils;
import com.yonglin.pdc.epaper_setting.utils.WifiNetworkUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    Fragment mFragment;
    Handler mHandler = new Handler();
    ArrayList<String> mFragmentHistory = new ArrayList<>(Arrays.asList(Constants.FRAGMENT_DEVICE_CONFIGURATION));

    private DeviceConfigurationFragment mDeviceConfigurationFragment;
    private FirmwareFragment mFirmwareFragment;
    private ProfileFragment mProfileFragment;
    private BatchConfigurationFragment mBatchConfigurationFragment;


    Timer logShowTimerOut;
    boolean isDetailLog = false;
    String logStr = "";
    String logDetailStr = "";
    long headlineLabelPressTime = 0;

    public boolean isSettingProcessing = false;

    boolean otaMode = false;


    private Context mContext;
    private Toolbar mToolbar;

    Menu mMenu;

    public Menu getMenu() {
        return mMenu;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        // request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //開啟位置權限才能看到搜尋的WiFi SSID
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }


        // Debug
        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                int i = getSupportFragmentManager().getBackStackEntryCount();
                Log.e(TAG, "BackStack count :" + i);
            }
        });

        mContext = getBaseContext();

        // toolbar
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.menu_device_configuration);
        setSupportActionBar(mToolbar);


        // load profiles
        GlobalData.getInstance().getProfileMap().clear();
        HashMap<String, DeviceProfile> profileMap = FileUtils.loadProfiles(mContext);
        GlobalData.getInstance().getProfileMap().putAll(profileMap);
        GlobalData.getInstance().getSavedProfileMap().putAll(profileMap);
//        mProfileMap = FileUtils.loadProfiles(mContext);
//        mProfileList = FileUtils.loadProfiles(mContext);

        if (savedInstanceState == null) {
            mDeviceConfigurationFragment = DeviceConfigurationFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container_main, mDeviceConfigurationFragment)
                    .commitNow();
        }


        findViewById(R.id.device_configuration_loader_layout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
//
//
//        ((TextView)findViewById(R.id.device_configuration_headline_label)).setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if(event.getAction() == MotionEvent.ACTION_DOWN){
//                    headlineLabelPressTime = System.currentTimeMillis();
//                    return true;
//                } else if(event.getAction() == MotionEvent.ACTION_UP){
//                    if((System.currentTimeMillis() - headlineLabelPressTime) > 10000){ //10秒
//                        if(isDetailLog) {
//                            Toast.makeText(ConfigDeviceActivity.this, "Log exit detail mode", Toast.LENGTH_SHORT).show();
//                            isDetailLog = false;
//                            TextView log = findViewById(R.id.device_configuration_log);
//                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                                log.setText(Html.fromHtml(logStr, Html.FROM_HTML_MODE_LEGACY));
//                            } else {
//                                log.setText(Html.fromHtml(logStr));
//                            }
//                        } else {
//                            Toast.makeText(ConfigDeviceActivity.this, "Log enter detail mode", Toast.LENGTH_SHORT).show();
//                            isDetailLog = true;
//                            TextView log = findViewById(R.id.device_configuration_log);
//                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                                log.setText(Html.fromHtml(logDetailStr, Html.FROM_HTML_MODE_LEGACY));
//                            } else {
//                                log.setText(Html.fromHtml(logDetailStr));
//                            }
//                        }
//                        return true;
//                    }
//                }
//                return false;
//            }
//        });
//

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
//                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {//没有授权权限
//                ActivityCompat.requestPermissions(MainActivity.this,
//                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
//            } else {//授权了权限
////                updateFwFileList();
//            }
//        } else {//6.0以下系统
////            updateFwFileList();
//        }


        if (savedInstanceState != null) {
            //Restore the fragment's instance
            // TODO last key fragment
//            Fragment mFragment = getSupportFragmentManager().getFragment(savedInstanceState, Constants.FRAGMENT_CONFIG_DEVICE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO Save the fragment's instance
//        getSupportFragmentManager().putFragment(outState, Constants.FRAGMENT_CONFIG_DEVICE, mFragment);
    }


//    private boolean settingContentPrepare() {
//        boolean result = true;
//
//        Spinner ssid = findViewById(R.id.device_configuration_wifi_ssid_name_spinner);
//        deviceSettingGatewaySsid = ssid.getSelectedItem().toString();
//        if (deviceSettingGatewaySsid.equals("")) {
//            result = false;
//            logShow("Wifi SSID input error", Color.RED, false);
//        }
//
//        Spinner encrypt = findViewById(R.id.device_configuration_encrypt_spinner);
//        deviceSettingEncrypt = encrypt.getSelectedItem().toString();
//
//        EditText EnterpriseName = findViewById(R.id.device_configuration_enterprise_name_edittext);
//        deviceSettingEnterpriseName = EnterpriseName.getText().toString();
//        if (deviceSettingEncrypt.equals(SecurityType.PSK.toString()) && deviceSettingEnterpriseName.equals("")) {
//            result = false;
//            logShow("Enterprise name is empty", Color.RED, false);
//        }
//
//        EditText password = findViewById(R.id.device_configuration_wifi_password_edittext);
//        deviceSettingPassword = password.getText().toString();
//        if (!(deviceSettingEncrypt.equals(SecurityType.OPEN.toString()))) {
//            if (deviceSettingPassword.equals("")) {
//                result = false;
//                logShow("Wifi Password is empty", Color.RED, false);
//            }
//        }
//
//        Spinner ipType = findViewById(R.id.device_configuration_ip_type_spinner);
//        deviceSettingIpType = ipType.getSelectedItem().toString();
//
//        EditText staticIp = findViewById(R.id.device_configuration_static_ip_edittext);
//        if (deviceSettingIpType.equals(IpType.Static_IP.toString()) && (ConversionUtil.ipStringToByte(staticIp.getText().toString()) == null)) {
//            result = false;
//            logShow("Static IP input error", Color.RED, false);
//        } else {
//            deviceSettingStaticIp = staticIp.getText().toString();
//        }
//
//        EditText gatewayIp = findViewById(R.id.device_configuration_gateway_ip_edittext);
//        if (deviceSettingIpType.equals(IpType.Static_IP.toString()) && (ConversionUtil.ipStringToByte(gatewayIp.getText().toString()) == null)) {
//            result = false;
//            logShow("Gateway IP input error", Color.RED, false);
//        } else {
//            deviceSettingGatewayIp = gatewayIp.getText().toString();
//        }
//
//        EditText subnetMask = findViewById(R.id.device_configuration_subnet_mask_edittext);
//        if (deviceSettingIpType.equals(IpType.Static_IP.toString()) && (ConversionUtil.ipStringToByte(subnetMask.getText().toString()) == null)) {
//            result = false;
//            logShow("subnet Mask input error", Color.RED, false);
//        } else {
//            deviceSettingSubnetMask = subnetMask.getText().toString();
//        }
//
//        EditText dnsService = findViewById(R.id.device_configuration_dns_service_edittext);
//        if (deviceSettingIpType.equals(IpType.Static_IP.toString()) && (ConversionUtil.ipStringToByte(dnsService.getText().toString()) == null)) {
//            result = false;
//            logShow("DNS service input error", Color.RED, false);
//        } else {
//            deviceSettingDnsServer = dnsService.getText().toString();
//        }
//
//        EditText serverIp = findViewById(R.id.device_configuration_server_ip_edittext);
//        if ((ConversionUtil.ipStringToByte(serverIp.getText().toString()) == null)) {
//            result = false;
//            logShow("Server IP input error", Color.RED, false);
//        } else {
//            deviceSettingServerIp = serverIp.getText().toString();
//        }
//
//        EditText serverPort = findViewById(R.id.device_configuration_server_port_edittext);
//        try {
//            deviceSettingServerPort = serverPort.getText().toString();
//        } catch (NumberFormatException e) {
//            result = false;
//            logShow("Server port input error", Color.RED, false);
//        }
//
//        EditText serialNumber = findViewById(R.id.device_configuration_device_serial_edittext);
//        deviceSettingSerialNumber = serialNumber.getText().toString();
//
////        Log.d(TAG, "########## settingContentPrepare: " + result);
////        Log.d(TAG, "GatewaySsid : " + deviceSettingGatewaySsid);
////        Log.d(TAG, "Encrypt : " + deviceSettingEncrypt);
////        Log.d(TAG, "EnterpriseName : " + deviceSettingEnterpriseName);
////        Log.d(TAG, "Password : " + deviceSettingPassword);
////        Log.d(TAG, "IpType : " + deviceSettingIpType);
////        if(deviceSettingStaticIp!=null)
////            Log.d(TAG, "StaticIp : " + deviceSettingStaticIp);
////        if(deviceSettingGatewayIp!=null)
////            Log.d(TAG, "GatewayIp : " + deviceSettingGatewayIp);
////        if(deviceSettingSubnetMask!=null)
////            Log.d(TAG, "SubnetMask : " + deviceSettingSubnetMask);
////        if(deviceSettingDnsServer!=null)
////            Log.d(TAG, "DnsServer : " + deviceSettingDnsServer);
////        if(deviceSettingServerIp!=null)
////            Log.d(TAG, "ServerIp : " + deviceSettingServerIp);
////        Log.d(TAG, "ServerPort : " + deviceSettingServerPort);
////        Log.d(TAG, "SerialNumber : " + deviceSettingSerialNumber);
//
//        return result;
//    }


    public void showLoader(boolean show, final String text) {
        if (show) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.device_configuration_loader_layout).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.device_configuration_loader_label)).setText(text);
                }
            });

            //如果loader畫面顯示過久，則關閉loader畫面
            if (logShowTimerOut != null)
                logShowTimerOut.cancel();

            int connectTimeOut;
            if (otaMode)
                connectTimeOut = 60000;
            else
                connectTimeOut = 5000;

            logShowTimerOut = new Timer();
            logShowTimerOut.schedule(new TimerTask() {
                @Override
                public void run() {
                    showLoader(false, "");
                }
            }, WifiNetworkUtils.CONNECTION_TIMEOUT + connectTimeOut);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.device_configuration_loader_layout).setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.device_configuration_loader_label)).setText(text);
                }
            });
            if (logShowTimerOut != null)
                logShowTimerOut.cancel();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


        final int id = item.getItemId();

        if (id == R.id.action_about) {
            showAbout();
            return true;
        }

        // check if we were at profile
        if (mProfileFragment != null
                && (mFragmentHistory.size() > 1)
                && mFragmentHistory.get(mFragmentHistory.size() - 1).equals(Constants.FRAGMENT_PROFILE)
                // lastly, check the content
                && mProfileFragment.isProfileChanged()) {
            mProfileFragment.confirmSaveProfiles();
            return true;
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        switch (id) {
            case R.id.action_device_configuration:
                mToolbar.setTitle(R.string.menu_device_configuration);
                // create
                if (mDeviceConfigurationFragment == null) {
                    mDeviceConfigurationFragment = DeviceConfigurationFragment.newInstance();
                }
                // check is current
                if (mDeviceConfigurationFragment.isVisible()) {
                    return true;
                }
//                // check others
//                if (mFirmwareFragment != null) {
//                    ft.hide(mFirmwareFragment);
//                }
//                if (mProfileFragment != null) {
//                    ft.hide(mProfileFragment);
//                }
//                if (mBatchConfigurationFragment != null) {
//                    ft.hide(mBatchConfigurationFragment);
//                }
//                // check is added
//                if (!mDeviceConfigurationFragment.isAdded()) {
//                    ft.add(R.id.container_main, mDeviceConfigurationFragment, Constants.FRAGMENT_DEVICE_CONFIGURATION);
//                }
//                ft.show(mDeviceConfigurationFragment);
                mFragmentHistory.add(Constants.FRAGMENT_DEVICE_CONFIGURATION);
//                ft.addToBackStack(Constants.FRAGMENT_DEVICE_CONFIGURATION).commit();
                ft.replace(R.id.container_main, mDeviceConfigurationFragment, Constants.FRAGMENT_DEVICE_CONFIGURATION).commit();
                break;
            case R.id.action_firmware:
                mToolbar.setTitle(R.string.menu_firmware);
                // create
                if (mFirmwareFragment == null) {
                    mFirmwareFragment = FirmwareFragment.newInstance();
                }
                // check is current
                if (mFirmwareFragment.isVisible()) {
                    return true;
                }
//                // check others
//                if (mDeviceConfigurationFragment != null) {
//                    ft.hide(mDeviceConfigurationFragment);
//                }
//                if (mProfileFragment != null) {
//                    ft.hide(mProfileFragment);
//                }
//                if (mBatchConfigurationFragment != null) {
//                    ft.hide(mBatchConfigurationFragment);
//                }
//                // check is added
//                if (!mFirmwareFragment.isAdded()) {
//                    ft.add(R.id.container_main, mFirmwareFragment, Constants.FRAGMENT_FIRMWARE);
//                }
//                ft.show(mFirmwareFragment);
                mFragmentHistory.add(Constants.FRAGMENT_FIRMWARE);
//                ft.addToBackStack(Constants.FRAGMENT_FIRMWARE).commit();
                ft.replace(R.id.container_main, mFirmwareFragment, Constants.FRAGMENT_FIRMWARE).commit();
                break;
            case R.id.action_profiles:
                mToolbar.setTitle(R.string.menu_profiles);
                // create
                if (mProfileFragment == null) {
                    mProfileFragment = ProfileFragment.newInstance();
                }
                // check is current
                if (mProfileFragment.isVisible()) {
                    return true;
                }
//                // check others
//                if (mDeviceConfigurationFragment != null) {
//                    ft.hide(mDeviceConfigurationFragment);
//                }
//                if (mFirmwareFragment != null) {
//                    ft.hide(mFirmwareFragment);
//                }
//                if (mBatchConfigurationFragment != null) {
//                    ft.hide(mBatchConfigurationFragment);
//                }
//                // check is added
//                if (!mProfileFragment.isAdded()) {
//                    ft.add(R.id.container_main, mProfileFragment, Constants.FRAGMENT_PROFILE);
//                }
//                ft.show(mProfileFragment);
                mFragmentHistory.add(Constants.FRAGMENT_PROFILE);
//                ft.addToBackStack(Constants.FRAGMENT_PROFILE).commit();
                ft.replace(R.id.container_main, mProfileFragment, Constants.FRAGMENT_PROFILE).commit();

//                // if there is an edit request, consume it, but wait until view is ready
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (!TextUtils.isEmpty(GlobalData.getInstance().getCurrentEditProfile())) {
//                            if (mProfileFragment != null) {
//                                mProfileFragment.loadProfile(GlobalData.getInstance().getCurrentEditProfile());
//                            }
//                        }
//                    }
//                });
                break;
            case R.id.action_device_batch_configuration:
                mToolbar.setTitle(R.string.menu_device_batch_configuration);
                // create
                if (mBatchConfigurationFragment == null) {
                    mBatchConfigurationFragment = BatchConfigurationFragment.newInstance();
                }
                // check is current
                if (mBatchConfigurationFragment.isVisible()) {
                    return true;
                }
//                // check others
//                if (mDeviceConfigurationFragment != null) {
//                    ft.hide(mDeviceConfigurationFragment);
//                }
//                if (mFirmwareFragment != null) {
//                    ft.hide(mFirmwareFragment);
//                }
//                if (mProfileFragment != null) {
//                    ft.hide(mProfileFragment);
//                }
//                // check is added
//                if (!mBatchConfigurationFragment.isAdded()) {
//            “
//                }
//                ft.show(mBatchConfigurationFragment);
                mFragmentHistory.add(Constants.FRAGMENT_BATCH_CONFIGURATION);
//                ft.addToBackStack(Constants.FRAGMENT_ABOUT).commit();
                ft.replace(R.id.container_main, mBatchConfigurationFragment, Constants.FRAGMENT_BATCH_CONFIGURATION).commit();
//                // update profile list
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mBatchConfigurationFragment.updateProfileSpinner(null);
                    }
                });
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // check if we were at profile
        if (mProfileFragment != null
                && (mFragmentHistory.size() > 1)
                && mFragmentHistory.get(mFragmentHistory.size() - 1).equals(Constants.FRAGMENT_PROFILE)
                // lastly, check the content
                && mProfileFragment.isProfileChanged()) {
            mProfileFragment.confirmSaveProfiles();
            return;
        }
////        if (getSupportFragmentManager().getBackStackEntryCount() <= 0) {
//        if (mFragmentHistory.size() > 1
//                && mFragmentHistory.get(mFragmentHistory.size() - 2).equals(Constants.FRAGMENT_DEVICE_CONFIGURATION)) {
//            // if the last page was device config, go back to it, mostly after we edited the profiles.
//            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//            mToolbar.setTitle(R.string.menu_device_configuration);
//            // TODO null after update one profile and save it, then press back.
//            ft.replace(R.id.container_main, mDeviceConfigurationFragment, Constants.FRAGMENT_DEVICE_CONFIGURATION).commit();
//            // update profile list
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    mDeviceConfigurationFragment.updateProfileSpinner(null);
//                }
//            });
//            mFragmentHistory.clear();
//            mFragmentHistory.add(Constants.FRAGMENT_DEVICE_CONFIGURATION);
//        } else {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View customView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_message, (ViewGroup) findViewById(android.R.id.content), false);
        final MaterialTextView msgTextView = customView.findViewById(R.id.dialog_text_view);
        msgTextView.setText(R.string.dialog_confirm_exit);
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
//        }
//        } else {
//            super.onBackPressed();
//            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//            List<Fragment> fragments = getSupportFragmentManager().getFragments();
//            Fragment f = null;
//            for (Fragment fragment : fragments) {
//                if (fragment != null && fragment.isVisible())
//                    f = fragment;
//            }
//
//            if (f instanceof DeviceConfigurationFragment) {
//                mToolbar.setTitle(R.string.menu_device_configuration);
//            } else if (f instanceof FirmwareFragment) {
//                mToolbar.setTitle(R.string.menu_firmware);
//            } else if (f instanceof ProfileFragment) {
//                mToolbar.setTitle(R.string.menu_profiles);
//            } else if (f instanceof BatchConfigurationFragment) {
//                mToolbar.setTitle(R.string.menu_about);
//            }
//            ft.show(f).commit();
//        }
    }

    private void showAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View customView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_message, (ViewGroup) findViewById(android.R.id.content), false);
        final MaterialTextView msgTextView = customView.findViewById(R.id.dialog_text_view);

        String version = "1.0";
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            version = versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String text = String.format(getString(R.string.dialog_about_message), version);

        msgTextView.setText(text);
        builder.setTitle(R.string.dialog_about_title);
        builder.setView(customView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
}
