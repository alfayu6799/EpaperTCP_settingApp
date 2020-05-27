package com.yonglin.pdc.epaper_setting.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yonglin.pdc.epaper_setting.DeviceItemDetailsLookup;
import com.yonglin.pdc.epaper_setting.DeviceItemKeyProvider;
import com.yonglin.pdc.epaper_setting.DeviceItemRecyclerViewAdapter;
import com.yonglin.pdc.epaper_setting.MainActivity;
import com.yonglin.pdc.epaper_setting.R;
import com.yonglin.pdc.epaper_setting.RunTaskActivity;
import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.utils.ConversionUtil;
import com.yonglin.pdc.epaper_setting.data.DeviceData;
import com.yonglin.pdc.epaper_setting.data.DeviceStatus;
import com.yonglin.pdc.epaper_setting.data.DeviceType;
import com.yonglin.pdc.epaper_setting.utils.FileUtils;
import com.yonglin.pdc.epaper_setting.utils.NetworkUtil;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class FirmwareFragment extends Fragment {
    private static final String TAG = "FirmwareFragment";

    private View mRootView;
    private Context mContext;
    private MainActivity mActivity;

    private Button mApplyButton;

    private Spinner mDeviceTypeSpinner;

    private ImageButton mSelectAllButton;
    private ImageButton mClearAllButton;
    private ImageButton mBrowseFileButton;
    private ImageButton mClearFileButton;
    private ImageButton mSearchButton;
    private ImageButton mLogButton;

    private RecyclerView mRecyclerView;
    private DeviceItemRecyclerViewAdapter mAdapter;
    private SelectionTracker mSelectionTracker;
    private TextView mDeviceCountTextView;
    private TextView mFirmwareFileTextView;
    private TextView mChecksumTextView;
    private Uri mFirmwareUri;

    private Boolean mIsScanningWifi = false;
    private Boolean mIsReceiverUnregistered = true;

    ArrayList<DeviceData> mDeviceItemList = new ArrayList<>();
    ArrayList<DeviceData> mFullWifiItemList = new ArrayList<>();
    // default must match the 1st item of the spinner
    public DeviceType mCurrentDevice = DeviceType.EPD_78;

    public static FirmwareFragment newInstance() {
        return new FirmwareFragment();
    }

    public FirmwareFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_firmware, container, false);
        // binding
        mDeviceTypeSpinner = (Spinner) mRootView.findViewById(R.id.device_firmware_device_type_list);
        mSelectAllButton = mRootView.findViewById(R.id.device_firmware_select_all);
        mClearAllButton = mRootView.findViewById(R.id.device_firmware_clear_all);
        mBrowseFileButton = mRootView.findViewById(R.id.firmware_file_button);
        mClearFileButton = mRootView.findViewById(R.id.firmware_file_clear_button);
        mFirmwareFileTextView = mRootView.findViewById(R.id.firmware_file_status);
        mDeviceCountTextView = mRootView.findViewById(R.id.device_firmware_selection_status);
        mSearchButton = mRootView.findViewById(R.id.device_firmware_search_devices);
        mRecyclerView = mRootView.findViewById(R.id.device_firmware_recycler_view_device_list);
        mApplyButton = mRootView.findViewById(R.id.device_firmware_apply);
        mLogButton = mRootView.findViewById(R.id.device_firmware_show_log);

        mChecksumTextView = mRootView.findViewById(R.id.firmware_checksum_text);

        mActivity = (MainActivity) getActivity();
        return mRootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            switch (requestCode) {
                case Constants.INTENT_REQUEST_CODE_TASK_FIRMWARE_DEVICE:
                    if (resultCode == RESULT_OK && data.getExtras() != null) {
                        mDeviceItemList = ConversionUtil.deviceListFromJson(data.getStringExtra(Constants.INTENT_EXTRA_JSON_DEVICE_LIST));
                        mAdapter.setDeviceList(mDeviceItemList);
                    }
                    break;

                case Constants.INTENT_REQUEST_CODE_PICK_FIRMWARE_FILE: //Firmware update 20200522
                    if (resultCode == RESULT_OK && data.getData() != null) {
                        if (data != null) {
                            Uri uri = data.getData();
                            Log.d(TAG, "Firmware Uri: " + uri.toString());
                            mFirmwareUri = uri;
                            Pair<String, Long> metadata = FileUtils.getUriMetadata(mContext, uri);
                            mFirmwareFileTextView.setText(metadata.first);
                        }
                    }
                    break;
                default:

            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // setup listeners
        mLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, RunTaskActivity.class));
            }
        });

//        updateSpinner(mDeviceTypeSpinner, R.array.device_types);
        updateSpinner(mDeviceTypeSpinner, R.array.firmware_device_types);
        mDeviceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                DeviceType newDevice = DeviceType.parseInt(i);

                if (mCurrentDevice != newDevice) {
                    // clear wifi list
                    if (mDeviceItemList.size() > 0) {
                        mDeviceItemList.clear();
                        mFullWifiItemList.clear();
                        mAdapter.setDeviceList(new ArrayList<DeviceData>());
                        Toast.makeText(mContext, mContext.getString(R.string.device_configuration_device_type_changed), Toast.LENGTH_SHORT).show();
                    }
                }
                mCurrentDevice = newDevice;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        mSelectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAdapter.selectAll();

            }
        });

        mClearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAdapter.clearAllSelection();
            }
        });

        mBrowseFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,
                        mContext.getString(R.string.select_file_title)),
                        Constants.INTENT_REQUEST_CODE_PICK_FIRMWARE_FILE);
            }
        });

        mClearFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFirmwareFileTextView.setText(R.string.file_status_missing);
                mFirmwareUri = null;
            }
        });

        mApplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validateInput()) {
                    return;
                }
                //mAdapter.clearAllSelection(); //清空打勾選項?? 20200525
                Intent intent = new Intent(mContext, RunTaskActivity.class);
                ArrayList<DeviceData> newList = new ArrayList<>();
                for (DeviceData device : mDeviceItemList) {
                    if (device.isSelected()) {
                        // clear failure status before rerun
                        device.setStatus(DeviceStatus.NONE);
                        newList.add(device);
                    }
                }
                intent.putExtra(Constants.INTENT_EXTRA_JSON_DEVICE_LIST, ConversionUtil.deviceListToJson(newList));
                intent.setAction(Constants.INTENT_ACTION_UPLOAD_FIRMWARE);
                intent.putExtra(Constants.INTENT_EXTRA_DEVICE_FIRMWARE_URI, mFirmwareUri.toString());
                intent.putExtra(Constants.INTENT_EXTRA_JSON_DEVICE_FIRMWARE_CHECKSUM, mChecksumTextView.getText().toString());
                startActivityForResult(intent, Constants.INTENT_REQUEST_CODE_TASK_FIRMWARE_DEVICE);
            }
        });

        mDeviceCountTextView.setText(String.format(getResources().getString(R.string.device_configuration_device_selected), 0, 0));

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) &
                        (mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) { //開啟位置權限才能看到搜尋的WiFi SSID
                    final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
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
                    mIsScanningWifi = true;
                    mActivity.showLoader(true, "Scanning Wifi......");
                    mContext.registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    mIsReceiverUnregistered = false;
                    NetworkUtil.startScan(mContext);
                }
            }
        });

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext,
                DividerItemDecoration.VERTICAL));

        mAdapter = new DeviceItemRecyclerViewAdapter(new ArrayList<DeviceData>(), mContext);
        mRecyclerView.setAdapter(mAdapter);

        mSelectionTracker = new SelectionTracker.Builder<>(
                "device-selection",
                mRecyclerView,
                new DeviceItemKeyProvider(mRecyclerView),
                new DeviceItemDetailsLookup(mRecyclerView),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.<Long>createSelectAnything())
                .build();

        mAdapter.setSelectionTracker(mSelectionTracker);
        mSelectionTracker.addObserver(new SelectionTracker.SelectionObserver() {
            @Override
            public void onSelectionChanged() {
                super.onSelectionChanged();
                Log.d(TAG, "onSelectionChanged()");
                mDeviceCountTextView.setText(String.format(getResources().getString(R.string.device_configuration_device_selected), mSelectionTracker.getSelection().size(), mDeviceItemList.size()));
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null)
            mSelectionTracker.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mSelectionTracker.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if (!mIsReceiverUnregistered) {
            // TODO use application context?
            mContext.unregisterReceiver(receiverWifi);
        }
        super.onDestroy();
    }


    BroadcastReceiver receiverWifi = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> mFullScanList;
            List<ScanResult> mDeviceScanList;

            if (mIsScanningWifi) {
                mIsScanningWifi = false;

                mDeviceItemList.clear();
                mFullWifiItemList.clear();

                mFullScanList = NetworkUtil.getWifiScanResults(true, mContext);

                // get device items from the list
                mDeviceScanList = new ArrayList<>();
                String prefix = DeviceType.getPrefix(mCurrentDevice);
                for (ScanResult scanResult : mFullScanList) {
                    // FIXME EPD support two prefixes at the moment.
                    boolean isException = (!prefix.equals(DeviceType.getPrefix(DeviceType.RTM)))
                            && scanResult.SSID.contains(Constants.prefixArray[3]);

                    if (scanResult.SSID.contains(prefix) || isException) {
                        mDeviceScanList.add(scanResult);
                        mDeviceItemList.add(new DeviceData(scanResult));
                    }
                }

                for (ScanResult result : mFullScanList) {
                    mFullWifiItemList.add(new DeviceData(result));
                }

                if (mAdapter != null) {
                    mAdapter.setDeviceList(mDeviceItemList);
                    mDeviceCountTextView.setText(String.format(getResources().getString(R.string.device_configuration_device_selected), 0, mDeviceItemList.size()));
                }

                Log.d(TAG, "Wifi filter: " + prefix);
                Log.d(TAG, "Wifi count: " + mFullScanList.size());
                Log.d(TAG, "Device count: " + mDeviceItemList.size());

                if (mDeviceItemList.size() == 0) {
                    Toast.makeText(mContext, mContext.getString(R.string.device_configuration_device_not_found), Toast.LENGTH_SHORT).show();
                }

                mActivity.showLoader(false, "");
                mIsReceiverUnregistered = true;
                mContext.unregisterReceiver(receiverWifi);
            }
        }
    };

    private void updateSpinner(Spinner spinner, @ArrayRes int resArrayId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext,
                resArrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private boolean validateInput() {
        if (mFirmwareUri == null) {
            Toast.makeText(mContext, mContext.getString(R.string.device_firmware_no_file), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ConversionUtil.isTextViewEmpty(mChecksumTextView)) {
            Toast.makeText(mContext, mContext.getString(R.string.device_firmware_no_checksum), Toast.LENGTH_LONG).show();
            return false;
        }

        try {
            Integer.parseInt(mChecksumTextView.getText().toString(), 16);
        } catch (NumberFormatException e) {
            Toast.makeText(mContext, mContext.getString(R.string.device_firmware_no_checksum), Toast.LENGTH_LONG).show();
            return false;
        }

        if (!mSelectionTracker.hasSelection()) {
            Toast.makeText(mContext, mContext.getString(R.string.device_configuration_no_device_selected), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
