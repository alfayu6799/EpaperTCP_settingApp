package com.yonglin.pdc.epaper_setting.data;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.yonglin.pdc.epaper_setting.utils.NetworkUtil;


// data class for wifi scanned result
public class DeviceData implements Comparable<DeviceData> {

    // basic data
    private String ssid;
    private SecurityType securityType;
    private int rssi;

    // status
    private DeviceStatus status = DeviceStatus.NONE;
    private boolean isSelected = false;

    public DeviceData(String ssid, SecurityType securityType) {
        this.ssid = ssid;
        this.securityType = securityType;
    }

    public DeviceData(ScanResult scanResult) {
        this.ssid = scanResult.SSID;
        this.securityType = NetworkUtil.getScanResultSecurity(scanResult);
        this.rssi = WifiManager.calculateSignalLevel(scanResult.level, 4);
    }

    @Override
    public int compareTo(DeviceData data) {
        if (this.ssid.equals(data.ssid)) {
            return this.securityType.toString().compareTo(data.securityType.toString());
        }
        return this.ssid.compareTo(data.ssid);
    }

    @Override
    public String toString() {
        return "DeviceData{" +
                "ssid='" + ssid + '\'' +
                ", securityType=" + securityType +
                ", rssi=" + rssi +
                ", status=" + status +
                ", isSelected=" + isSelected +
                '}';
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public SecurityType getSecurityType() {
        return securityType;
    }

    public void setSecurityType(SecurityType securityType) {
        this.securityType = securityType;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
