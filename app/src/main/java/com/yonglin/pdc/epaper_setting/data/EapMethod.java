package com.yonglin.pdc.epaper_setting.data;

import android.util.SparseArray;

public enum EapMethod {
    // moving TLS to the last item so when we remove it from list in profile. The int value remains correct.
    PEAP(0),TTLS(1), TLS(2);

    private static final SparseArray<EapMethod> lookupArray = new SparseArray<>(3);

    static {
        for (EapMethod type : EapMethod.values()) {
            lookupArray.append(type.value, type);
        }
    }

    private final int value;

    EapMethod(int value) {
        this.value = value;
    }

    public int getIntValue() {
        return value;
    }

    public static EapMethod parseInt(int value) {
        EapMethod result = lookupArray.get(value);
        if (result == null) {
            return PEAP;
        } else {
            return result;
        }
    }
}
