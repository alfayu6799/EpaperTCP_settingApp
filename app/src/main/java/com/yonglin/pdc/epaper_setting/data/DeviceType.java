
package com.yonglin.pdc.epaper_setting.data;

import android.util.Log;
import android.util.SparseArray;

public enum DeviceType {

//	   <string-array name="device_types">
//        <item>EPD 7.8"</item>
//        <item>EPD 9.7"</item>
//        <item>RTM</item>
//     </string-array>
// The int values here are matched in string-array and spinner order, must be kept in sync.

    EPD_78(0), EPD_97(1), RTM(2);
    private static final SparseArray<DeviceType> lookupArray = new SparseArray<>(3);

    static {
        for (DeviceType type : DeviceType.values()) {
            lookupArray.append(type.value, type);
        }
    }

    private final int value;

    DeviceType(int value) {
        this.value = value;
    }

    public static String getStringValue(DeviceType type) {
        switch (type) {
            case EPD_78:
                return "EPD7800";
            case EPD_97:
                return "EPD9700";
            case RTM:
                return "RTM";
            default:
                return null;
        }
    }

    public static String getPrefix(DeviceType type) {
        switch (type) {
            case EPD_78:
                return Constants.prefixArray[0];
            case EPD_97:
                return Constants.prefixArray[1];
            case RTM:
                return Constants.prefixArray[2];
            default:
                return null;
        }
    }

    public int getIntValue() {
        return value;
    }

    public static DeviceType parseInt(int value) {
        DeviceType result = lookupArray.get(value);
        if (result == null) {
            Log.d("DeviceType", "Invalid device type detected!");
            return null;
        } else {
            return result;
        }
    }
}
