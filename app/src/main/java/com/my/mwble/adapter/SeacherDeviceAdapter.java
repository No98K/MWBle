package com.my.mwble.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.my.mwble.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索设备Adapter
 */
public class SeacherDeviceAdapter extends RecyclerView.Adapter {
    private Context context;
    private List<BluetoothDevice> data;
    private CallBack callBack;

    public SeacherDeviceAdapter(Context context, CallBack callBack) {
        this.context = context;
        this.data = new ArrayList<>();
        this.callBack = callBack;
    }

    public void setData(List<BluetoothDevice> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_device, null);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof MyHolder) {
            ((MyHolder) holder).tvName.setText("设备名称:" + data.get(position).getName());
            ((MyHolder) holder).tvAddress.setText("设备地址:" + data.get(position).getAddress());

            ((MyHolder) holder).llAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callBack != null) {
                        callBack.onItemClick(position, data.get(position));
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private class MyHolder extends RecyclerView.ViewHolder {
        private LinearLayout llAll;
        private TextView tvName;
        private TextView tvAddress;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            llAll = itemView.findViewById(R.id.item_ll_device_all);
            tvName = itemView.findViewById(R.id.item_tv_device_name);
            tvAddress = itemView.findViewById(R.id.item_tv_device_address);
        }
    }

    public interface CallBack {
        void onItemClick(int position, BluetoothDevice device);
    }
}
