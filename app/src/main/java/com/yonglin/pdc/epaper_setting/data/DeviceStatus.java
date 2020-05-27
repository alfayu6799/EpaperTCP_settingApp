package com.yonglin.pdc.epaper_setting.data;

import android.util.SparseArray;

public enum DeviceStatus {

    NONE(-1), QUEUED(0), PROCESSING(1), FAILED(2), SUCCESS(3);

    private static final SparseArray<DeviceStatus> lookupArray = new SparseArray<>(5);

    static {
        for (DeviceStatus type : DeviceStatus.values()) {
            lookupArray.append(type.value, type);
        }
    }

    private final int value;

    DeviceStatus(int value) {
        this.value = value;
    }

    public int getIntValue() {
        return value;
    }

    public static DeviceStatus parseInt(int value) {
        DeviceStatus result = lookupArray.get(value);
        if (result == null) {
            return NONE;
        } else {
            return result;
        }
    }
}
