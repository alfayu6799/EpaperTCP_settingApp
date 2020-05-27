package com.yonglin.pdc.epaper_setting;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

public class DeviceItemDetailsLookup extends ItemDetailsLookup<Long> {
    private final RecyclerView mRecyclerView;

    public DeviceItemDetailsLookup(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Nullable
    @Override
    public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
        View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
        if (view != null) {
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
            if (holder instanceof DeviceItemRecyclerViewAdapter.DeviceItemViewHolder) {
                return ((DeviceItemRecyclerViewAdapter.DeviceItemViewHolder) holder).getItemDetails();
            }
        }
        return null;
    }
}

