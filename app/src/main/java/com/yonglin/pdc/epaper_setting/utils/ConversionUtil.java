package com.yonglin.pdc.epaper_setting.utils;

import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yonglin.pdc.epaper_setting.R;
import com.yonglin.pdc.epaper_setting.data.DeviceData;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.DeviceType;
import com.yonglin.pdc.epaper_setting.data.EapMethod;
import com.yonglin.pdc.epaper_setting.data.SecurityType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.yonglin.pdc.epaper_setting.data.IpType.Static_IP;

public class ConversionUtil {
    /**
     * check if the ssid has extra quotes and remove them
     *
     * @param ssid input ssid
     * @return ssid without quotes
     */
    public static String formatSSID(String ssid) {
        if (ssid != null && ssid.length() > 3
                && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    public static boolean isIpAddress(String ipAddress) {
        String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

    public static boolean isTextViewEmpty(TextView view) {
        return (view == null || view.getText() == null || TextUtils.isEmpty(view.getText()));
    }

    public static byte[] ipStringToByte(String ipStr) {
        if (!isIpAddress(ipStr))
            return null;

        try {
            InetAddress ip = null;
            ip = InetAddress.getByName(ipStr);
            return ip.getAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String deviceListToJson(ArrayList<DeviceData> list) {
        Gson gson = new Gson();
        return gson.toJson(list, new TypeToken<List<DeviceData>>() {
        }.getType());

    }

    public static ArrayList<DeviceData> deviceListFromJson(String jsonString) {
        ArrayList<DeviceData> outputList = new ArrayList<>();
        Gson gson = new Gson();
        List<DeviceData> deviceStatusList = gson.fromJson(jsonString, new TypeToken<List<DeviceData>>() {
        }.getType());
        if (deviceStatusList != null) {
            outputList.addAll(deviceStatusList);
        }
        return outputList;
    }


    /**
     * validate a device profile and prompt errors
     *
     * @param profile
     * @return error string id, or 0 if everything is fine
     */
    public static int validateInput(DeviceProfile profile) {
        // server ip
        if (TextUtils.isEmpty(profile.getServerIp()) || !ConversionUtil.isIpAddress(profile.getServerIp())) {
            return R.string.device_profile_error_server_ip;
        }
        // server port
        if (profile.getDeviceType() != DeviceType.RTM) {
            if (TextUtils.isEmpty(profile.getServerPort())) {
                return R.string.device_profile_error_server_port;
            }
            try {
                Integer.parseInt(profile.getServerPort());
            } catch (NumberFormatException ne) {
                return R.string.device_profile_error_server_port;
            }
        } else { // check MQTT fields
            // topic
            if (TextUtils.isEmpty(profile.getRtmMqttTopic())) {
                return R.string.device_profile_error_mqtt_topic;
            }
            // username
            if (TextUtils.isEmpty(profile.getRtmMqttUsername())) {
                return R.string.device_profile_error_mqtt_username;
            }
            // password
            if (TextUtils.isEmpty(profile.getRtmMqttPassword())) {
                return R.string.device_profile_error_mqtt_password;
            }
        }
        // ssid
        if (TextUtils.isEmpty(profile.getServerSsid())) {
            return R.string.device_profile_error_ssid;
        }
        // password
        if (profile.getSecurityType() != SecurityType.OPEN
                && profile.getEapMethod() != EapMethod.TLS
                && TextUtils.isEmpty(profile.getPassword())) {
            return R.string.device_profile_error_password;
        }
        // EAP
        if (profile.getSecurityType() == SecurityType.EAP) {
            // ca cert
            if (!profile.isEapCaCertSaved()) {
                return R.string.device_profile_error_ca_cert;
            }
            // identity
            if (TextUtils.isEmpty(profile.getEapIdentity())) {
                return R.string.device_profile_error_identity;
            }
            // specific fields
            if (profile.getEapMethod() == EapMethod.TLS) {
                // client cert
                if (!profile.isEapClientCertSaved()) {
                    return R.string.device_profile_error_client_cert;
                }
                // private key
                if (!profile.isEapPrivateKeySaved()) {
                    return R.string.device_profile_error_private_key;
                }
            } else { // PEAP and TTLS
                if (TextUtils.isEmpty(profile.getEapAnonymousIdentity())) {
                    return R.string.device_profile_error_anonymous_identity;
                }
            }
        }
        if (profile.getIpType() == Static_IP) {
            // static ip address
            if (TextUtils.isEmpty(profile.getStaticIp())
                    || !ConversionUtil.isIpAddress(profile.getStaticIp())) {
                return R.string.device_profile_error_ip;
            }
            // gateway ip
            if (TextUtils.isEmpty(profile.getGatewayIp())
                    || !ConversionUtil.isIpAddress(profile.getGatewayIp())) {
                return R.string.device_profile_error_gateway;
            }
            // subnet mask
            if (TextUtils.isEmpty(profile.getSubnetMask())
                    || !ConversionUtil.isIpAddress(profile.getSubnetMask())) {
                return R.string.device_profile_error_subnet_mask;
            }
            // DNS
            if (TextUtils.isEmpty(profile.getDnsServer())
                    || !ConversionUtil.isIpAddress(profile.getDnsServer())) {
                return R.string.device_profile_error_dns;
            }
        }
        return 0;
    }
}