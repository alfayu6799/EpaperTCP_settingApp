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

package com.yonglin.pdc.epaper_setting.utils;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WifiNetworkUtils {

    private static final String TAG = "WifiNetworkUtils";
    private static final WifiNetworkUtils instance = new WifiNetworkUtils();

    private WifiConfiguration mConfigurationToConnectAfterDisconnecting = null;
    private Context mContext = null;
    private BitbiteNetworkUtilsCallback mBitbiteNetworkUtilsCallback;
    private Handler mWifiHandler = new Handler();
    private Boolean mConnectAfterDisconnected = false;
    private WifiManager wifiManager;
    private BitbiteNetworkUtilsCallback mTempBitbiteNetworkUtilsCallback;
    private ConnectivityManager mConnectivityManager;
    private Boolean isInitial3GEnabled;

    private Timer connectionCheckTimer = new Timer();
    private Timer getIpCheckTimer = new Timer();
    private int wifiConnectionTime = 0;
    private int getIpTime = 0;
    public static final long CONNECTION_CHECK_TIME = 1000;
    public static final long CONNECTION_TIMEOUT = 15000;

    public static WifiNetworkUtils getInstance(Context context) {
        if (instance.mContext == null) {
            instance.mContext = context;
            instance.wifiManager = (WifiManager) instance.mContext.getSystemService(Context.WIFI_SERVICE);
            Log.e(TAG,"TEST=====" + (instance.wifiManager  == null) );
            instance.mConnectivityManager = (ConnectivityManager) instance.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            instance.isInitial3GEnabled = isLollipopAndUp() ? instance.isMobileDataEnabledLollipop() : instance.isMobileDataEnabled();
        }

        return instance;
    }

    public void onResume() {
        setMobileDataEnabled(false);
        mContext.registerReceiver(instance.mWifiConnectionReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        mContext.registerReceiver(instance.mSupplicantStateReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
    }

    public void onPaused() {
        setMobileDataEnabled(isInitial3GEnabled);
        try {
            mContext.unregisterReceiver(mWifiConnectionReceiver);
            mContext.unregisterReceiver(mSupplicantStateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getConnectedSSID() {
        String networkName = null;
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                networkName = wifiInfo.getSSID().replaceAll("\"", "");
            }
            if (networkName == null || networkName.equals("<unknown ssid>") || networkName.equals("0x") || networkName.equals("")) {
                networkName = null;
            }
        }
        return networkName;
    }

    public List<WifiConfiguration> getSavedNetworks(Context context) {
        return wifiManager.getConfiguredNetworks();
    }

    public boolean deleteSavedNetworks(String ssid) {
        boolean isDeleted = false;
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null
                    && (i.SSID.equals(ssid) || ConversionUtil.formatSSID(i.SSID).equals(ssid))) {
                Log.i(TAG, "Found " + i.SSID + " from configured network. Attempt to clean up.");
                isDeleted = wifiManager.removeNetwork(i.networkId);
                Log.i(TAG, "delete Network result: " + isDeleted);
                break;
            }
        }
        return isDeleted;
    }

    public void connectToWifi(WifiConfiguration configuration, Context context, BitbiteNetworkUtilsCallback callback) {
        try {
            mBitbiteNetworkUtilsCallback = callback;
            mConfigurationToConnectAfterDisconnecting = configuration;

            if (configuration == null) {
                Log.e(TAG, "WifiConfiguration was null.");
                mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Unknown);
                return;
            }

            if (isLollipopAndUp()) {
                Log.d(TAG, "Connecting (Lollipop and up) to " + configuration.SSID);
                mConnectAfterDisconnected = true;
            }

            Log.i(TAG, "Disconnect: " + wifiManager.disconnect());
            boolean connectFromList = false;
            // check for existing configuration
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (!connectFromList) {
                    if (i.SSID != null && i.SSID.equals(configuration.SSID)) {
                        Log.i(TAG, "Found " + configuration.SSID + " from configured network.");
                        boolean isEnabled = wifiManager.enableNetwork(i.networkId, true);
                        Log.i(TAG, "enable Network: " + isEnabled);
                        wifiManager.reconnect();
                        connectFromList = true;
                        break;
                    }
                }
            }

            if (!connectFromList) {
                Log.i(TAG, "Creating new configuration for: " + configuration.SSID);
                wifiManager.addNetwork(configuration);
                wifiManager.saveConfiguration(); // deprecated in api 26
                List<WifiConfiguration> configsList = wifiManager.getConfiguredNetworks();
                boolean found = false;
                for (WifiConfiguration i : configsList) {
                    if (!found) {
                        if (i.SSID != null && i.SSID.equals(configuration.SSID)) {
                            Log.i(TAG, "Connecting to just created \"" + configuration.SSID + "\" from list");
                            wifiManager.enableNetwork(i.networkId, true);
                            wifiManager.reconnect();
                            found = true;
                        }
                    }
                }
            }

            wifiConnectionTime = 0;
            connectionCheckTimer = new Timer();
            connectionCheckTimer.schedule(wifiConnectionTimerTask(), CONNECTION_CHECK_TIME, CONNECTION_CHECK_TIME);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception with input: " + configuration + " " + context + " " + callback);
            if (mBitbiteNetworkUtilsCallback != null)
                mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Unknown);
        }
    }

    public static Boolean isLollipopAndUp() {
        int currentApiVersion = Build.VERSION.SDK_INT;
        return currentApiVersion >= Build.VERSION_CODES.LOLLIPOP;
    }

    public List<ScanResult> getWifiScanResults(Context context) {
        return wifiManager.getScanResults();
    }

    public WifiConfiguration getConfigurationForScanResult(ScanResult result, Context context) {
        for (WifiConfiguration configuration : getSavedNetworks(context)) {
            if (configuration.SSID.replaceAll("\"", "").equals(result.SSID))
                return configuration;
        }
        return null;
    }

    private BroadcastReceiver mWifiConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo info = (NetworkInfo) intent.getExtras().get("networkInfo");
            State state = info != null ? info.getState() : null;
            String network = info != null ? info.getExtraInfo() : null;
            String networkType = info != null ? info.getTypeName() : null;
            if (networkType != null && networkType.contains("mobile")) {
                return;
            }
            if (mBitbiteNetworkUtilsCallback == null)
                return;
            Log.i(TAG, "State: " + state + " Network:\"" + network + "\"");
            if (state != null) {
                switch (state) {
                    case CONNECTED:
                        if (network == null) {
                            network = getConnectedSSID();
                        }
                        if (network.contains("sphone")) {
                            return;
                        }
                        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
                        if (network.equals(mConfigurationToConnectAfterDisconnecting.SSID) && !mConfigurationToConnectAfterDisconnecting.SSID.equals("")) {
                            Log.i(TAG, "Connected to desired network: \"" + network + "\"");
                            successfullyConnectToWifi(network, mBitbiteNetworkUtilsCallback);
                            disconnectCallback();
                        } else if (mConfigurationToConnectAfterDisconnecting.SSID.replaceAll("\"", "").equals(network)) {
                            Log.i(TAG, "Connected to desired network: \"" + network + "\"");
                            successfullyConnectToWifi(network, mBitbiteNetworkUtilsCallback);
                            disconnectCallback();
                        }
                        break;
                    case CONNECTING:
                        break;
                    case DISCONNECTED:

                        if (mConnectAfterDisconnected) {
                            mConnectAfterDisconnected = false;
                            Log.i(TAG, "Connecting after disconnecting: " + wifiManager.enableNetwork(mConfigurationToConnectAfterDisconnecting.networkId, true));
                        }
                        break;
                    case DISCONNECTING:
                        break;
                    case SUSPENDED:
                        break;
                    case UNKNOWN:
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private void disconnectCallback() {
        mWifiHandler.removeCallbacks(mWifiConnectionTimeout);
        mConfigurationToConnectAfterDisconnecting = null;
        mBitbiteNetworkUtilsCallback = null;
    }

    private Runnable mWifiConnectionTimeout = new Runnable() {
        @Override
        public void run() {
            String connectedNetwork = getConnectedSSID();
            Log.i(TAG, "Connected to \"" + connectedNetwork + "\" now, was supposed to connect to: " + mConfigurationToConnectAfterDisconnecting.SSID);
            mTempBitbiteNetworkUtilsCallback = mBitbiteNetworkUtilsCallback;
            mBitbiteNetworkUtilsCallback = null;
            if ((mConfigurationToConnectAfterDisconnecting.SSID).equals(connectedNetwork)) {
                successfullyConnectToWifi(mConfigurationToConnectAfterDisconnecting.SSID, mTempBitbiteNetworkUtilsCallback);
            } else if ((mConfigurationToConnectAfterDisconnecting.SSID).replaceAll("\"", "").equals(connectedNetwork)) {
                successfullyConnectToWifi(mConfigurationToConnectAfterDisconnecting.SSID, mTempBitbiteNetworkUtilsCallback);
            } else {
                Log.e(TAG, "Wifi Connection Runnable Timeout");
                mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Timeout);
            }
        }
    };

    private TimerTask wifiConnectionTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                String connectedNetwork = getConnectedSSID();
                final String targetNetwork = mConfigurationToConnectAfterDisconnecting.SSID;
                Log.i(TAG, "Connected to \"" + connectedNetwork + "\" now, was supposed to connect to: \"" + targetNetwork + "\"");

                if (ConversionUtil.formatSSID(targetNetwork).equals(connectedNetwork)) {
                    mTempBitbiteNetworkUtilsCallback = mBitbiteNetworkUtilsCallback;
                    mBitbiteNetworkUtilsCallback = null;
                    getIpCheckTimer = new Timer();
                    getIpTime = 0;
                    getIpCheckTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            int ipAddress = wifi.getConnectionInfo().getIpAddress();
                            if (ipAddress != 0) {
                                successfullyConnectToWifi(targetNetwork, mTempBitbiteNetworkUtilsCallback);
                                getIpCheckTimer.cancel();
                            } else if ((getIpTime++) > (CONNECTION_TIMEOUT / CONNECTION_CHECK_TIME)) {
                                Log.e(TAG, "wifi connection timer check: NO IP");
                                mTempBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Timeout);
                                getIpCheckTimer.cancel();
                            }
                        }
                    }, CONNECTION_CHECK_TIME, CONNECTION_CHECK_TIME);
                    connectionCheckTimer.cancel();
                } else if ((wifiConnectionTime++) > (CONNECTION_TIMEOUT / CONNECTION_CHECK_TIME)) {
                    Log.e(TAG, "wifi connection timer check: TIMEOUT");
                    mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Timeout);
                    connectionCheckTimer.cancel();
                }
            }
        };
    }

    private void successfullyConnectToWifi(String ssid, BitbiteNetworkUtilsCallback callback) {
        try {
            // wait a bit for network state to refresh on slow phones.
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (callback == null)
            return;
        try {
            if (isActiveNetworkWifi()) {
                callback.successfullyConnectedToNetwork(ssid);
            } else {
                callback.failedToConnectToNetwork(WifiConnectionFailure.Connected_To_3G);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to activate the callback");
        }
    }

    private BroadcastReceiver mSupplicantStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mBitbiteNetworkUtilsCallback == null) {
                return;
            }
            if (intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR)) {
                int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);
                Log.e(TAG, "Supplicant State Receiver, error: " + error);
                if (error == WifiManager.ERROR_AUTHENTICATING)
                    mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Wrong_Password);
                else
                    mBitbiteNetworkUtilsCallback.failedToConnectToNetwork(WifiConnectionFailure.Unknown);
                disconnectCallback();
            } else if (intent.hasExtra(WifiManager.EXTRA_NEW_STATE)) {
            } else {
                Log.i(TAG, "**** Got Supplicant state with unknown extra ****");
                for (String key : intent.getExtras().keySet()) {
                    Object value = intent.getExtras().get(key);
                    Log.i(TAG, String.format("%s %s (%s)", key, value != null ? value.toString() : null, value != null ? value.getClass().getName() : null));
                }
                Log.i(TAG, "************************************************");
            }
        }
    };

    public enum WifiConnectionFailure {
        Connected_To_3G,
        Wrong_Password,
        Timeout,
        Unknown
    }

    public void clearCallback() {
        Log.e(TAG, "Callback was cleared");
        mBitbiteNetworkUtilsCallback = null;
    }

    public interface BitbiteNetworkUtilsCallback {
        void successfullyConnectedToNetwork(String ssid);

        void failedToConnectToNetwork(WifiConnectionFailure failure);
    }

    private Boolean isActiveNetworkWifi() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null)
            return true;
        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_WIMAX) {
            Log.e(TAG, "We are in WIFI");
            return true;
        } else {
            Log.e(TAG, "We are not in WIFI");
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void lollipopChangeDefaultNetwork(final ConnectivityManager cm, Context context) {
        Network[] array = cm.getAllNetworks();
        for (Network network : array) {
            NetworkInfo info = cm.getNetworkInfo(network);
            Log.i(TAG, "Network: \"" + network + "\"\nInfo: " + info);

            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.i(TAG, "Setting the network: \"" + network + "\" as default (" + ConnectivityManager.setProcessDefaultNetwork(network) + ")");
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setMobileDataEnabled(boolean enabled) {
        try {
            final Class conmanClass = Class.forName(mConnectivityManager.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(mConnectivityManager);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Boolean isMobileDataEnabledLollipop() {
        TelephonyManager telephonyService = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        switch (telephonyService.getDataState()) {
            case TelephonyManager.DATA_DISCONNECTED:
                Log.i(TAG, "DATA_DISCONNECTED");
                break;
            case TelephonyManager.DATA_CONNECTING:
                Log.i(TAG, "DATA_CONNECTING");
                break;
            case TelephonyManager.DATA_SUSPENDED:
                Log.i(TAG, "DATA_SUSPENDED");
                break;
            case TelephonyManager.DATA_CONNECTED:
                Log.i(TAG, "DATA_CONNECTED");
                return true;
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Boolean isMobileDataEnabled() {
        try {
            final Class conmanClass = Class.forName(mConnectivityManager.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(mConnectivityManager);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());

            final Method getMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("getMobileDataEnabled");
            getMobileDataEnabledMethod.setAccessible(true);
            Boolean flag = (Boolean) getMobileDataEnabledMethod.invoke(iConnectivityManager);
            Log.i(TAG, "3G data was initialised " + flag);
            return flag;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to know if 3G was enabled");
        }
        return false;
    }
}