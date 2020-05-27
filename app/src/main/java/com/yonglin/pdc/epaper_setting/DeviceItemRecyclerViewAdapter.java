package com.yonglin.pdc.epaper_setting;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.yonglin.pdc.epaper_setting.data.DeviceData;

import java.util.List;

public class DeviceItemRecyclerViewAdapter extends RecyclerView.Adapter<DeviceItemRecyclerViewAdapter.DeviceItemViewHolder> {

    private List<DeviceData> mDeviceList;
    private Context mContext;

    private SelectionTracker mSelectionTracker;

    private boolean isSelectionMode = true;


    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
    }

    public DeviceItemRecyclerViewAdapter(List<DeviceData> deviceList, Context context) {
        this.mDeviceList = deviceList;
        this.mContext = context;
        setHasStableIds(true);
    }

    public void setSelectionTracker(SelectionTracker mSelectionTracker) {
        this.mSelectionTracker = mSelectionTracker;
        clearAllSelection();
    }

    public List<DeviceData> getDeviceList() {
        return mDeviceList;
    }

    public void setDeviceList(List<DeviceData> mDeviceList) {
        mSelectionTracker.clearSelection();
        this.mDeviceList = mDeviceList;
        notifyDataSetChanged();
    }

    public void clearAllSelection() {
        mSelectionTracker.clearSelection();
        for(DeviceData device: mDeviceList){
            device.setSelected(false);
        }
        notifyDataSetChanged();
    }

    public void selectAll() {
        // go through all keys which are position id here
        for (int i = 0; i < mDeviceList.size(); i++) {
            mSelectionTracker.select(Long.valueOf(i));
        }
        for(DeviceData device: mDeviceList){
            device.setSelected(true);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new DeviceItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceItemViewHolder holder, final int position) {
        DeviceData item = mDeviceList.get(position);
        // hide checkbox in readonly mode
        boolean isSelected = isSelectionMode && mSelectionTracker.isSelected(Long.valueOf(position));
        holder.bind(item, isSelected);
    }

    @Override
    public int getItemCount() {
        return mDeviceList == null ? 0 : mDeviceList.size();
    }

    @Override
    public long getItemId(int position) {
        // use position as id
        return position;
    }

    class DeviceItemViewHolder extends RecyclerView.ViewHolder {
        private TextView txtDeviceName;
        private ImageView imgDeviceCheckedStatus;
        private ImageView imgDeviceStatus;

        DeviceItemViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDeviceName = itemView.findViewById(R.id.device_name);
            imgDeviceCheckedStatus = itemView.findViewById(R.id.device_checked_status);
            imgDeviceStatus = itemView.findViewById(R.id.device_status);
        }

        void bind(DeviceData item, boolean isSelected) {
            txtDeviceName.setText(item.getSsid());
            // If the item is selected then we change its state to activated
            imgDeviceCheckedStatus.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
            switch (item.getStatus()) {
                case FAILED:
                    imgDeviceStatus.setImageDrawable( mContext.getDrawable(R.drawable.baseline_error_outline_24));
                    imgDeviceStatus.setColorFilter(Color.RED);
                    imgDeviceStatus.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    imgDeviceStatus.setImageDrawable( mContext.getDrawable(R.drawable.baseline_done_24));
                    imgDeviceStatus.setColorFilter(Color.GREEN);
                    imgDeviceStatus.setVisibility(View.VISIBLE);
                    break;
                default:
                    imgDeviceStatus.setVisibility(View.INVISIBLE);
            }
            item.setSelected(isSelected);
        }

        ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new DeviceItemDetails(getAdapterPosition(), mDeviceList.get(getAdapterPosition()));
        }
    }
}

