package com.yonglin.pdc.epaper_setting;


import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.data.DeviceData;
import com.yonglin.pdc.epaper_setting.data.DeviceType;
import com.yonglin.pdc.epaper_setting.utils.NetworkUtil;

import java.util.ArrayList;
import java.util.List;

public class SelectWifiDialogActivity extends AppCompatActivity {
    private static final String TAG = "SelectWifiDialog";


    private Context mContext;

    private Button mCancelButton;
    private Button mOkButton;
    private TextView mSsidTextView;
    private ImageButton mSearchButton;

    private String mFilterPrefix;
    private boolean isScanningWifi = false;
    private boolean isReceiverUnregisterd = true;

    private boolean isDeviceMode = false;
    ArrayList<DeviceData> listItems = new ArrayList<>();

    private RecyclerView mRecyclerView;
    private WifiItemRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_select_wifi);
        setResult(RESULT_CANCELED);


        // request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //開啟位置權限才能看到搜尋的WiFi SSID
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
        // request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) { //開啟位置權限才能看到搜尋的WiFi SSID
                requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 2);
            }
        }



        // load filter from caller
        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            mFilterPrefix = intent.getStringExtra(Constants.INTENT_EXTRA_DEVICE_PREFIX);
            if (!TextUtils.isEmpty(mFilterPrefix)) {
                isDeviceMode = true;
            }
        }

        mContext = getBaseContext();
        mCancelButton = findViewById(R.id.select_wifi_cancel);
        mOkButton = findViewById(R.id.select_wifi_ok);
        mSsidTextView = findViewById(R.id.select_wifi_ssid_name);
        mSearchButton = findViewById(R.id.select_wifi_search_devices);

        setupRecycleView();

        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra(Constants.INTENT_EXTRA_SSID, mSsidTextView.getText());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) &
                        (SelectWifiDialogActivity.this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) { //開啟位置權限才能看到搜尋的WiFi SSID
                    final AlertDialog.Builder builder = new AlertDialog.Builder(SelectWifiDialogActivity.this);
                    builder.setTitle("this app needs location access");
                    builder.setMessage("please grant location access so this app can detect beacons.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    });
                    builder.show();
                } else {
                    isScanningWifi = true;
                    // clear old selection
                    mSsidTextView.setText("");
                    Toast.makeText(SelectWifiDialogActivity.this, SelectWifiDialogActivity.this.getString(R.string.search_ap_message), Toast.LENGTH_LONG).show();
                    registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    isReceiverUnregisterd = false;
                    NetworkUtil.startScan(SelectWifiDialogActivity.this);
                }
            }
        });

        // refresh data
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        isReceiverUnregisterd = false;
        NetworkUtil.startScan(SelectWifiDialogActivity.this);
        isScanningWifi = true;
        Toast.makeText(SelectWifiDialogActivity.this, SelectWifiDialogActivity.this.getString(R.string.search_ap_message), Toast.LENGTH_LONG).show();
    }

    private void setupRecycleView() {
        mRecyclerView = findViewById(R.id.select_wifi_recycler_view_ssid_list);
        mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));

        mAdapter = new WifiItemRecyclerViewAdapter(null, this, new WifiItemRecyclerViewAdapter.WifiItemClickListener() {
            @Override
            public void deviceItemClicked(View v, int position) {
                // update name
                Log.e(TAG, "position" + position);
                mSsidTextView.setText(mAdapter.getDeviceList().get(position).getSsid());
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        if (!isReceiverUnregisterd) {
            unregisterReceiver(receiverWifi);
        }
        super.onDestroy();
    }

    BroadcastReceiver receiverWifi = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> fullList;

            if (isScanningWifi) {
                isScanningWifi = false;
                fullList = NetworkUtil.getWifiScanResults(true, SelectWifiDialogActivity.this);

                // remove device items from the list
                listItems.clear();
                for (ScanResult scanResult : fullList) {
                    boolean isDevice = false;
                    String currentPrefix = null;
                    for (String prefix : Constants.prefixArray) {  //自家產品AP
                        if (scanResult.SSID.startsWith(prefix)) {
                            isDevice = true;
                            currentPrefix = prefix;
                            Log.d(TAG, String.format("Found device AP with SSID: %s (prefix: %s)", scanResult.SSID, prefix)); //自家產品AP
                            break;
                        }
                    }

                    if (!isDevice) {
                        Log.d(TAG, String.format("Found non-device AP with SSID: %s", scanResult.SSID)); //無線AP
                    }

                    // decide which to add
                    if (isDeviceMode) {
                        // FIXME quick hack
                        boolean isException = (!mFilterPrefix.equals(DeviceType.getPrefix(DeviceType.RTM)))
                                && Constants.prefixArray[3].equals(currentPrefix);

                        if (mFilterPrefix.equals(currentPrefix) || isException) {
                            listItems.add(new DeviceData(scanResult));
                        }
                    } else if (!isDevice) {
                        listItems.add(new DeviceData(scanResult));
                    }

                }

                if (listItems.isEmpty()) {
                    Toast.makeText(SelectWifiDialogActivity.this,
                            SelectWifiDialogActivity.this.getString(isDeviceMode ? R.string.device_wifi_not_found_message : R.string.server_wifi_not_found_message),
                            Toast.LENGTH_LONG).show();
                }

                if (mAdapter != null) {
                    mAdapter.setDeviceList(listItems);
                }

                isReceiverUnregisterd = true;
                SelectWifiDialogActivity.this.unregisterReceiver(receiverWifi);
            }
        }
    };
}

