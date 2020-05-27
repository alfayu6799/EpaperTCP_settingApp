package com.yonglin.pdc.epaper_setting;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;

import com.yonglin.pdc.epaper_setting.data.DeviceData;

public class DeviceItemDetails extends ItemDetailsLookup.ItemDetails {

    private int pos;
    private DeviceData item;

    public DeviceItemDetails(int pos, DeviceData item) {
        this.pos = pos;
        this.item = item;
    }

    @Override
    public int getPosition() {
        return pos;
    }

    @Nullable
    @Override
    public Object getSelectionKey() {
        return Long.valueOf(pos);
    }

    @Override
    public boolean inSelectionHotspot(@NonNull MotionEvent e) {
        // allow single tap to enable selection mode
        return true;
    }
}