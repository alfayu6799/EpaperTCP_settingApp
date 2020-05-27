package com.yonglin.pdc.epaper_setting;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yonglin.pdc.epaper_setting.data.DeviceData;

import java.util.List;

public class WifiItemRecyclerViewAdapter extends RecyclerView.Adapter<WifiItemRecyclerViewAdapter.WifiItemViewHolder> {

    private List<DeviceData> mDeviceList;
    private Context mContext;

    private WifiItemClickListener mClickListener;

    public WifiItemRecyclerViewAdapter(List<DeviceData> deviceList, Context context, WifiItemClickListener listener) {
        this.mDeviceList = deviceList;
        this.mContext = context;
        this.mClickListener=listener;
        setHasStableIds(true);
    }


    public List<DeviceData> getDeviceList() {
        return mDeviceList;
    }

    public void setDeviceList(List<DeviceData> mDeviceList) {
        this.mDeviceList = mDeviceList;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public WifiItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new WifiItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiItemViewHolder holder, final int position) {
        DeviceData item = mDeviceList.get(position);
        holder.bind(item);
//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.e(getClass().getSimpleName(),"this is " + position);
//            }
//        });
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

    class WifiItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView txtDeviceName;
        private ImageView imgDeviceCheckedStatus;

        WifiItemViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDeviceName = itemView.findViewById(R.id.device_name);
            imgDeviceCheckedStatus = itemView.findViewById(R.id.device_checked_status);
            itemView.setOnClickListener(this);
        }

        void bind(DeviceData item) {
            txtDeviceName.setText(item.getSsid());
            imgDeviceCheckedStatus.setVisibility(View.INVISIBLE);
        }


        @Override
        public void onClick(View view) {
            if(mClickListener!=null) {
                mClickListener.deviceItemClicked(view, this.getAdapterPosition());
            }
        }
    }

    public interface WifiItemClickListener {
        void deviceItemClicked(View v, int position);
    }
}

