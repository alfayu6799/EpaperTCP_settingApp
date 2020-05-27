package com.yonglin.pdc.epaper_setting.utils;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.IpType;
import com.yonglin.pdc.epaper_setting.data.SecurityType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;


public class NetworkUtil {
    private static final String TAG = "NetworkUtil";

    public NetworkUtil() {
    }

    public static void startScan(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
    }

    public static WifiManager getWifiManager(Context context) {
        WifiManager wifiManager = null;
        try {
            wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return wifiManager;
    }

    public static ArrayList<ScanResult> removeMultipleSSIDsWithRSSI(ArrayList<ScanResult> list) {
        ArrayList<ScanResult> newList = new ArrayList<>();
        boolean contains;
        for (ScanResult ap : list) {
            contains = false;
            for (ScanResult mp : newList) {
                if ((mp.SSID).equals(ap.SSID)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                newList.add(ap);
            }
        }
        Collections.sort(newList, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                return (lhs.level < rhs.level ? 1 : (lhs.level == rhs.level ? 0 : -1));
            }

        });
        return newList;
    }

    public static List<ScanResult> getWifiScanResults(Boolean sorted, Context context) {
        WifiManager wifiManager = NetworkUtil.getWifiManager(context);
        List<ScanResult> wifiList = wifiManager.getScanResults();
        //Remove results with empty ssid
        ArrayList<ScanResult> wifiListNew = new ArrayList<>();
        for (ScanResult scanResult : wifiList) {
            if (!scanResult.SSID.equals(""))
                wifiListNew.add(scanResult);
        }

        if (sorted) {
            return removeMultipleSSIDsWithRSSI(wifiListNew);
        } else {
            return wifiListNew;
        }
    }

    public static SecurityType getScanResultSecurity(ScanResult scanResult) {
        String cap = scanResult != null ? scanResult.capabilities : "";
        SecurityType newState = SecurityType.OPEN;

        if (cap.contains("WEP"))
            newState = SecurityType.WEP;
        else if (cap.contains("PSK"))
            newState = SecurityType.PSK;
        else if (cap.contains("EAP"))
            newState = SecurityType.EAP;
        return newState;
    }

    public static WifiConfiguration getWifiConfigurationWithInfo(Context context, String ssid, SecurityType securityType, String password) {
        List<WifiConfiguration> configuredWifiList = null;
        WifiManager wifiManager = getWifiManager(context);
        if (wifiManager != null) {
            configuredWifiList = wifiManager.getConfiguredNetworks();
        }
        if (configuredWifiList == null) {
            return null;
        } else {
            for (WifiConfiguration i : configuredWifiList) {
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    Log.i(TAG, "Wifi configuration for " + ssid + " already exist, so we will use it");
                    return i;
                }
            }

            Log.i(TAG, "Wifi configuration for " + ssid + " doesn't exist, so we will create new one");
            Log.i(TAG, "SSID: " + ssid);
            Log.i(TAG, "Security: " + securityType);
            WifiConfiguration wc = new WifiConfiguration();

            wc.SSID = "\"" + ssid + "\"";
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.hiddenSSID = false;

            // For supported devices we hae PID (Open) and RTM (WPA)
            switch (securityType) {
                case OPEN:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    break;
                case WEP:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                    wc.preSharedKey = "\"" + password + "\"";
                    break;
                case PSK:
                    wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    // WPA
                    wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    // WPA2
                    wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    wc.preSharedKey = "\"" + password + "\"";
                    break;
                case EAP:
                    // EAP not supported
                    Log.e(TAG, "EAP network is not supported.");
                    return null;
                default:
                    break;
            }

            Log.i(TAG, "New wifi configuration with id " + wifiManager.addNetwork(wc));
            Log.i(TAG, "Saving configuration " + wifiManager.saveConfiguration());
            Log.i(TAG, "wc.networkId " + wc.networkId);

            configuredWifiList = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : configuredWifiList) {
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    Log.i(TAG, "Returning wifiConfiguration with id " + i.networkId);
                    return i;
                }
            }
        }
        return null;
    }

    /**
     * Upload cert/key files.
     *
     * @param context
     * @param profile
     * @param deviceIp
     * @param command  command bytes to determine which file to send to.
     * @return
     */
    public static boolean uploadCert(Context context, DeviceProfile profile, String deviceIp, Byte command) {

        UUID uuid = profile.getUuid();
        String filename = DeviceProfile.CA_CERT_FILE;
        switch (command) {
            case Constants.CMD_UPLOAD_CLIENT_CERT:
                filename = DeviceProfile.CLIENT_CERT_FILE;
                break;
            case Constants.CMD_UPLOAD_PRIVATE_KEY:
                filename = DeviceProfile.PRIVATE_KEY_FILE;
                break;
        }

        byte[] certData = FileUtils.loadExternalFile(context, uuid, filename);

        if (certData == null || certData.length == 0) {
            return false;
        }
        Log.d(TAG, String.format("Loaded %d bytes from %s.", certData.length, filename));

        byte[] configData = new byte[4 + certData.length];

        // 準備config 資料符合 W-CD-2A~4A
        Arrays.fill(configData, (byte) 0);
        // cmd
        configData[0] = command;
        // data size
        configData[1] = (byte) (certData.length / 0x100);
        configData[2] = (byte) (certData.length % 0x100);
        // data
        int sum = 0;
        for (int i = 0; i < certData.length; i++) {
            configData[i + 4] = certData[i];
            sum += certData[i];
        }
        // check sum
        configData[3] = (byte) (sum % 0x100);

        try {
            Socket socket = new Socket(deviceIp, Constants.TCP_SERVER_PORT);
            if (!socket.isConnected())
                return false;
            OutputStream os = socket.getOutputStream();
            os.write(configData);
            os.flush();

            byte[] data = new byte[50];
            InputStream is = socket.getInputStream();
            int receivedLength = is.read(data);
            if ((receivedLength != 10) || (data[0] != (byte) 0xAA)) {
                return false;
            }

            // check byte 1 ~ 4 should be 1
            for (int i = 1; i < 5; i++) {
                if ((data[i] != (byte) 0x01)) {
                    return false;
                }
            }

            if (is != null) {
                is.close();
                is = null;
            }
            if (os != null) {
                os.close();
                os = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Log.d(TAG, String.format("%s uploaded.", filename));
        return true;
    }

    public static boolean configDevice(Context context, String deviceSSID, DeviceProfile profile, String deviceIp) {
        // cert files
        if (profile.isEapCaCertSaved()) {
            sleep();
            if (!uploadCert(context, profile, deviceIp, Constants.CMD_UPLOAD_CA_CERT)) {
                return false;
            }
        }
        if (profile.isEapClientCertSaved()) {
            sleep();
            if (!uploadCert(context, profile, deviceIp, Constants.CMD_UPLOAD_CLIENT_CERT)) {
                return false;
            }
        }
        if (profile.isEapPrivateKeySaved()) {
            sleep();
            if (!uploadCert(context, profile, deviceIp, Constants.CMD_UPLOAD_PRIVATE_KEY)) {
                return false;
            }
        }

        sleep();

        // set config
        String serverIp = profile.getServerIp();
        String serverPort = profile.getServerPort();

        byte[] configData = new byte[151];
        Log.d(TAG, "TCP Set Server ip =  " + serverIp + ":" + serverPort);
        // 準備config 資料符合 W-CD-1A
        Arrays.fill(configData, (byte) 0);
        // cmd
        configData[0] = 0x04; // User IP

        String[] parts;
        // IP Type
        if (profile.getIpType() == IpType.Static_IP) {
            // Device static ip
            if (profile.getStaticIp() == null || profile.getGatewayIp() == null
                    || profile.getSubnetMask() == null || profile.getDnsServer() == null) {
                return false;
            }
            parts = profile.getStaticIp().split("\\.");
            if (parts.length != 4)
                return false;
            configData[1] = (byte) Integer.parseInt(parts[0]);
            configData[2] = (byte) Integer.parseInt(parts[1]);
            configData[3] = (byte) Integer.parseInt(parts[2]);
            configData[4] = (byte) Integer.parseInt(parts[3]);
            // Gateway ip
            parts = profile.getGatewayIp().split("\\.");
            if (parts.length != 4)
                return false;
            configData[5] = (byte) Integer.parseInt(parts[0]);
            configData[6] = (byte) Integer.parseInt(parts[1]);
            configData[7] = (byte) Integer.parseInt(parts[2]);
            configData[8] = (byte) Integer.parseInt(parts[3]);
            // Subnet Mask
            parts = profile.getSubnetMask().split("\\.");
            if (parts.length != 4)
                return false;
            configData[124] = (byte) Integer.parseInt(parts[0]);
            configData[125] = (byte) Integer.parseInt(parts[1]);
            configData[126] = (byte) Integer.parseInt(parts[2]);
            configData[127] = (byte) Integer.parseInt(parts[3]);
            // DNS Server
            parts = profile.getDnsServer().split("\\.");
            if (parts.length != 4)
                return false;
            configData[128] = (byte) Integer.parseInt(parts[0]);
            configData[129] = (byte) Integer.parseInt(parts[1]);
            configData[130] = (byte) Integer.parseInt(parts[2]);
            configData[131] = (byte) Integer.parseInt(parts[3]);
            configData[123] = 0x00;
        } else {
            configData[123] = 0x01; // dhcp
        }

        // Gateway SSID
        if (profile.getServerSsid() == null) {
            return false;
        }
        try {
            byte[] bytes = profile.getServerSsid().getBytes("UTF-8");
            if (bytes.length > 24)
                return false;
            for (int i = 0; i < bytes.length; i++)
                configData[9 + i] = bytes[i];
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
        // encrypt(security type)
        switch (profile.getSecurityType()) {
            case OPEN:
                configData[33] = 0x01;
                break;
            case WEP:
                configData[33] = 0x02;
                break;
            case PSK:
                configData[33] = 0x03;
                break;
            case EAP:
                // the EapMethod value is designed to match the order from protocol
                configData[33] = (byte) (4 + profile.getEapMethod().getIntValue());
                break;
            default:
                return false;
        }
        // password
        // As TLS may have or not have password, check has been removed and
        // we write whatever frontend sends here.
        // if (profile.getSecurityType() != SecurityType.OPEN) {
        if (profile.getPassword() == null) {
            Log.d(TAG, "profile has no password");
            // return false;
        } else {
            try {
                byte[] bytes = profile.getPassword().getBytes("UTF-8");
                if (bytes.length > 63)
                    return false;
                for (int i = 0; i < bytes.length; i++)
                    configData[34 + i] = bytes[i];
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }
        }
        //        }

        // Server ip
        if (profile.getServerIp() == null || profile.getServerPort() == null) {
            return false;
        }
        parts = profile.getServerIp().split("\\.");
        if (parts.length != 4)
            return false;
        configData[97] = (byte) Integer.parseInt(parts[0]);
        configData[98] = (byte) Integer.parseInt(parts[1]);
        configData[99] = (byte) Integer.parseInt(parts[2]);
        configData[100] = (byte) Integer.parseInt(parts[3]);
        // server Port
        int IntegerVal = Integer.parseInt(profile.getServerPort());
        configData[101] = (byte) (IntegerVal / 0x100);
        configData[102] = (byte) (IntegerVal % 0x100);

        // Device Serial number is the same as ssid for now
        if(deviceSSID!=null) {
            try {
                byte[] bytes = deviceSSID.getBytes("UTF-8");
                if (bytes.length > 20)
                    return false;
                for (int i = 0; i < bytes.length; i++)
                    configData[103 + i] = bytes[i];
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (profile.getSecurityType() == SecurityType.EAP) {
            // Enterprise User Name
            if(profile.getEapIdentity()!=null) {
                try {
                    byte[] bytes = profile.getEapIdentity().getBytes("UTF-8");
                    if (bytes.length > 8)
                        return false;
                    for (int i = 0; i < bytes.length; i++)
                        configData[132 + i] = bytes[i];
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            // Enterprise anonymous name
            if(profile.getEapAnonymousIdentity()!=null) {
                try {
                    byte[] bytes = profile.getEapAnonymousIdentity().getBytes("UTF-8");
                    if (bytes.length > 8)
                        return false;
                    for (int i = 0; i < bytes.length; i++)
                        configData[140 + i] = bytes[i];
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            // Enterprise server authentication
            // TODO this is a cert related field but still need to test its behavior,
            //  we are setting it on when certs are used now.
            boolean enabled = profile.isEapCaCertSaved() || profile.isEapClientCertSaved() || profile.isEapPrivateKeySaved();
            if (enabled) {
                configData[148] = 0x01;
            } else {
                configData[148] = 0; // disable
            }
        }
        Log.d(TAG, "TCP Set device ip =  " + deviceIp + ":" + Constants.TCP_SERVER_PORT);
        try {
            Socket socket = new Socket(deviceIp, Constants.TCP_SERVER_PORT);
            if (!socket.isConnected())
                return false;
            OutputStream os = socket.getOutputStream();
            os.write(configData);
            os.flush();

            // The set config does not reply now, we will read config later.
//			byte[] data = new byte[50];
//			InputStream is = socket.getInputStream();
//			int receivedLength = is.read(data);
//			if((receivedLength != 10) || (data[0] != (byte)0xAA))
//				return false;

//			if(is!= null) {
//				is.close();
//				is = null;
//			}
            if (os != null) {
                os.close();
                os = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

//        // attempt to read back the config and print DEBUG info
//        sleep();
//        Log.d(TAG, "checking FW version");
//        String getConfig = getDeviceConfig(deviceIp);
//        try {
//            JSONObject jsonObj = new JSONObject(getConfig);
//            // TODO keys are using old code here, would be better to use data object later if it needs go to UI
//            Log.d(TAG, "Server IP:" + jsonObj.getString("deviceSettingServerIp"));
//            Log.d(TAG, "FW:" + jsonObj.getString("deviceSettingFwVersion"));
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return false;
//        }
        return true;
    }


    public static boolean configRtmDevice(DeviceProfile profile, String deviceIp) {
        sleep();

        String serverIp = profile.getServerIp();

        // prepare command
        String command = String.format(
                "SetConfig 6,STA_SSID=%s,STA_Password=%s,"
                        + "MQTTServer=%s,MQTTTopic=%s,MQTTUserName=%s,MQTTPassword=%s\n"
                , profile.getServerSsid()
                , profile.getPassword()
                , profile.getServerIp()
                , profile.getRtmMqttTopic()
                , profile.getRtmMqttUsername()
                , profile.getRtmMqttPassword());

        Log.d(TAG, "RTM command:\n" + command);

        byte[] configData = null;
        try {
            configData = command.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }

        Log.d(TAG, "TCP Set Server ip =  " + serverIp);

        // Server ip
        if (profile.getServerIp() == null) {
            return false;
        }

        try {
            Socket socket = new Socket(deviceIp, Constants.RTM_TCP_SERVER_PORT);
            if (!socket.isConnected())
                return false;
            OutputStream os = socket.getOutputStream();
            os.write(configData);
            os.flush();

            byte[] data = new byte[11];
            InputStream is = socket.getInputStream();

            int receivedLength = is.read(data);
            // expecting "SetConfig 0" - OK or "SetConfig 1" - Failed
            if (receivedLength != 11)
                return false;
            String reply = new String(data);
            Log.d(TAG, "RTM response: " + reply);
            if (!Constants.RTM_SET_CONFIG_OK.equals(reply)) {
                Log.d(TAG, "RTM set config failed.");
                return false;
            }

            if (is != null) {
                is.close();
                is = null;
            }

            if (os != null) {
                os.close();
                os = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean doOtaUpdate(Context context, Uri fileUri, String checksum, String deviceIp) {
        sleep();

        int packetSize = 30720;
        byte[] OtaData = new byte[50];
        Log.d(TAG, "TCP OTA Device ip =  " + deviceIp + ":" + Constants.TCP_SERVER_PORT);
        Log.d(TAG, "TCP OTA File uri  =  " + fileUri.toString());

        int userChecksum;
        try {
            userChecksum = Integer.parseInt(checksum, 16);
        } catch (NumberFormatException e) {
            Log.d(TAG, "User checksum error: " + checksum);
            return false;
        }

        try {
            // open
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                return false;
            }

            byte[] fileBytes = ByteStreams.toByteArray(inputStream);
            inputStream.close();
            int fileLength = fileBytes.length;

            int cs = 0;

            for (byte b : fileBytes) {
                cs = (cs + (b & 0xFF)) & 0xFFFF;
            }

            if (cs != userChecksum) {
                Log.e(TAG, String.format("Checksums are different, expected: %d, provided: %d", cs, userChecksum));
            }

            Log.d(TAG, "File Size =  " + fileLength);
            Log.d(TAG, "Bytes Size =  " + fileBytes.length);
            Log.d(TAG, "Packet Size  =  " + packetSize);
            Log.d(TAG, "Checksum = " + cs);

            //準備config
            Arrays.fill(OtaData, (byte) 0);
            // cmd
            OtaData[0] = (byte) 0xF2; // User IP
            // File size
            OtaData[1] = (byte) (fileLength / 0x1000000 & 0xFF);
            OtaData[2] = (byte) (fileLength / 0x10000 & 0xFF);
            OtaData[3] = (byte) (fileLength / 0x100 & 0xFF);
            OtaData[4] = (byte) (fileLength & 0xFF);
            // Checksum
            OtaData[5] = (byte) (cs / 0x100 & 0xFF);
            OtaData[6] = (byte) (cs & 0xFF);
            // packet size
            OtaData[7] = (byte) (packetSize / 0x1000000 & 0xFF);
            OtaData[8] = (byte) (packetSize / 0x10000 & 0xFF);
            OtaData[9] = (byte) (packetSize / 0x100 & 0xFF);
            OtaData[10] = (byte) (packetSize & 0xFF);

            // send cmd and file info
            Socket socket = new Socket(deviceIp, Constants.TCP_SERVER_PORT);
            if (!socket.isConnected())
                return false;
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            os.write(OtaData);
            os.flush();

            int sendSize = 0;
            while (fileBytes.length > sendSize) {
                byte[] sendData;
                byte[] data = new byte[100];

                if (fileBytes.length > (sendSize + packetSize)) {
                    sendData = new byte[packetSize];
                    for (int i = 0; i < packetSize; i++) {
                        sendData[i] = fileBytes[i + sendSize];
                    }
                    sendSize += packetSize;
                } else {
                    sendData = new byte[fileBytes.length - sendSize];
                    for (int i = 0; i < sendData.length; i++) {
                        sendData[i] = fileBytes[i + sendSize];
                    }
                    sendSize += sendData.length;
                }


                long time = System.currentTimeMillis();
                int receiveLength = is.read(data);
                while ((receiveLength < 0) && (System.currentTimeMillis() > (time + 20000))) {
                    receiveLength = is.read(data);
                }

                Log.d(TAG, "receiveLength = " + receiveLength);
                Log.d(TAG, "data[0] = " + data[0]);
                if ((receiveLength != 100) || (data[0] < 0x41))
                    return false;

                os.write(sendData);
                os.flush();
            }

            if (is != null) {
                is.close();
                is = null;
            }
            if (os != null) {
                os.close();
                os = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static String getDeviceConfig(String serverIp) {
        byte[] sendData = new byte[]{(byte) 0xB4, 0x55, 0x55, 0x55, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] data = new byte[200];
        Log.d(TAG, "Reading device configuration from ip: " + serverIp + ":" + Constants.TCP_SERVER_PORT);

        try {
            Socket socket = new Socket(serverIp, Constants.TCP_SERVER_PORT);
            if (!socket.isConnected())
                return "";
            OutputStream os = socket.getOutputStream();
            os.write(sendData);
            os.flush();

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            while (dis.available() == 0) {
                ; // could hang here if we call get config right after set config.
            }
            int receiveLength = dis.read(data);

            if (dis != null) {
                dis.close();
                dis = null;
            }
            if (os != null) {
                os.close();
                os = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
            if ((receiveLength != 200) || (data[0] != 0x04)) {
                Log.d(TAG, "TCPGetDeviceConfig fail (receiveLength =  " + receiveLength + "; cmd = " + data[0]);
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject jsonObject = new JSONObject();
        try {
            byte[] bytes;
            jsonObject.put("deviceSettingStaticIp", String.valueOf(data[1]) + "." +
                    String.valueOf(data[2]) + "." + String.valueOf(data[3]) + "." +
                    String.valueOf(data[4]));

            jsonObject.put("deviceSettingGatewayIp", String.valueOf(data[5]) + "." +
                    String.valueOf(data[6]) + "." + String.valueOf(data[7]) + "." +
                    String.valueOf(data[8]));

            bytes = new byte[24];
            for (int i = 0; i < 24; i++)
                bytes[i] = data[9 + i];
            jsonObject.put("deviceSettingGatewaySsid", new String(bytes));

            if (data[33] == 0x01)
                jsonObject.put("deviceSettingEncrypt", "OPEN");
            else if (data[33] == 0x02)
                jsonObject.put("deviceSettingEncrypt", "WEP");
            else if (data[33] == 0x03)
                jsonObject.put("deviceSettingEncrypt", "WPA");
            else if (data[33] == 0x04)
                jsonObject.put("deviceSettingEncrypt", "WPA2");

            bytes = new byte[63];
            for (int i = 0; i < 63; i++)
                bytes[i] = data[34 + i];
            jsonObject.put("deviceSettingPassword", new String(bytes));

            jsonObject.put("deviceSettingServerIp", String.valueOf(data[97]) + "." +
                    String.valueOf(data[98]) + "." + String.valueOf(data[99]) + "." +
                    String.valueOf(data[100]));

            int port = data[101] * 0x100 + data[102];
            jsonObject.put("deviceSettingServerPort", String.valueOf(port));

            bytes = new byte[20];
            for (int i = 0; i < 20; i++)
                bytes[i] = data[103 + i];
            jsonObject.put("deviceSettingSerialNumber", new String(bytes));

            if (data[123] == 0x00)
                jsonObject.put("deviceSettingIpType", "Static IP");
            else if (data[123] == 0x01)
                jsonObject.put("deviceSettingIpType", "DHCP");

            jsonObject.put("deviceSettingSubnetMask", String.valueOf(data[124]) + "." +
                    String.valueOf(data[125]) + "." + String.valueOf(data[126]) + "." +
                    String.valueOf(data[127]));

            jsonObject.put("deviceSettingDnsServer", String.valueOf(data[128]) + "." +
                    String.valueOf(data[129]) + "." + String.valueOf(data[130]) + "." +
                    String.valueOf(data[131]));

            bytes = new byte[8];
            for (int i = 0; i < 8; i++)
                bytes[i] = data[132 + i];
            jsonObject.put("deviceSettingEnterpriseName", new String(bytes));

            bytes = new byte[10];
            for (int i = 0; i < 10; i++)
                bytes[i] = data[140 + i];
            jsonObject.put("deviceSettingFwVersion", new String(bytes));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }

    private static void sleep() {
        try {
            // wait a bit for network refresh and device to be ready for next request.
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}