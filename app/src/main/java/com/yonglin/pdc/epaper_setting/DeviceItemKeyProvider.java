package com.yonglin.pdc.epaper_setting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceItemKeyProvider extends ItemKeyProvider<Long> {
    RecyclerView mRecyclerView;

//    public DeviceItemKeyProvider(int scope, List<Long> items) {
    public DeviceItemKeyProvider(RecyclerView recyclerView) {
        super(ItemKeyProvider.SCOPE_MAPPED);
        this.mRecyclerView = recyclerView;
    }

    @Nullable
    @Override
    public Long getKey(int position) {
return        mRecyclerView.getAdapter().getItemId(position);
//        return items.get(position);
    }

    @Override
    public int getPosition(@NonNull Long key) {
        RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForItemId(key);
        return viewHolder == null ? RecyclerView.NO_POSITION : viewHolder.getLayoutPosition();


//        return items.indexOf(key);
    }
}


