package com.yonglin.pdc.epaper_setting.fragments;

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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.yonglin.pdc.epaper_setting.DeviceItemDetailsLookup;
import com.yonglin.pdc.epaper_setting.DeviceItemKeyProvider;
import com.yonglin.pdc.epaper_setting.DeviceItemRecyclerViewAdapter;
import com.yonglin.pdc.epaper_setting.MainActivity;
import com.yonglin.pdc.epaper_setting.R;
import com.yonglin.pdc.epaper_setting.RunTaskActivity;
import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.data.DeviceData;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.DeviceStatus;
import com.yonglin.pdc.epaper_setting.data.DeviceType;
import com.yonglin.pdc.epaper_setting.data.GlobalData;
import com.yonglin.pdc.epaper_setting.utils.ConversionUtil;
import com.yonglin.pdc.epaper_setting.utils.NetworkUtil;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.widget.AdapterView.INVALID_POSITION;

public class BatchConfigurationFragment extends Fragment {
    private static final String TAG = "BatchConfigFragment";

    private View mRootView;
    private Context mContext;
    private MainActivity mActivity;

    private Button mApplyButton;

    private Spinner mDeviceTypeSpinner;

    private Spinner mDeviceConfigProfileSpinner;

    private ImageButton mSelectAllButton;
    private ImageButton mClearAllButton;
    private ImageButton mEditProfileButton;
    private ImageButton mSearchButton;
    private ImageButton mLogButton;

    private RecyclerView mRecyclerView;
    private DeviceItemRecyclerViewAdapter mAdapter;
    private SelectionTracker mSelectionTracker;
    private TextView mDeviceCountTextView;


    private Boolean mIsScanningWifi = false;
    private Boolean mIsReceiverUnregistered = true;

    private DeviceProfile mCurrentProfile = new DeviceProfile();

    private ArrayList<DeviceData> mDeviceItemList = new ArrayList<>();
    //    ArrayList<DeviceData> mFullWifiItemList = new ArrayList<>();
    // default must match the 1st item of the spinner
    private DeviceType mCurrentDevice = DeviceType.EPD_78;

    public static BatchConfigurationFragment newInstance() {
        return new BatchConfigurationFragment();
    }

    public BatchConfigurationFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_batch_configuration, container, false);
        // binding
        mDeviceTypeSpinner = mRootView.findViewById(R.id.batch_configuration_device_type_list);
        mDeviceConfigProfileSpinner = mRootView.findViewById(R.id.batch_configuration_device_profile_list);
        mSelectAllButton = mRootView.findViewById(R.id.batch_configuration_select_all);
        mClearAllButton = mRootView.findViewById(R.id.batch_configuration_clear_all);
//        mEditProfileButton = mRootView.findViewById(R.id.batch_configuration_edit_profile);
        mDeviceCountTextView = mRootView.findViewById(R.id.batch_configuration_selection_status);
        mSearchButton = mRootView.findViewById(R.id.batch_configuration_search_devices);
        mRecyclerView = mRootView.findViewById(R.id.batch_recycler_view_device_list);
        mApplyButton = mRootView.findViewById(R.id.batch_configuration_apply);
        mLogButton = mRootView.findViewById(R.id.batch_configuration_show_log);

        mActivity = (MainActivity) getActivity();
        return mRootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            switch (requestCode) {
                case Constants.INTENT_REQUEST_CODE_TASK_BATCH_CONFIG_DEVICE:
                    if (resultCode == RESULT_OK && data.getExtras() != null) {
                        mDeviceItemList = ConversionUtil.deviceListFromJson(data.getStringExtra(Constants.INTENT_EXTRA_JSON_DEVICE_LIST));
                        mAdapter.setDeviceList(mDeviceItemList);
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

        updateSpinner(mDeviceTypeSpinner, R.array.device_types);
        mDeviceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                DeviceType newDevice = DeviceType.parseInt(i);

                if (mCurrentDevice != newDevice) {
                    // update profile list per selection
                    updateProfileSpinner(newDevice);

                    // clear wifi list
                    if (mDeviceItemList.size() > 0) {
                        mDeviceItemList.clear();
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


//        mEditProfileButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDeviceConfigProfileSpinner.getSelectedItem() != null) {
//                    // pass current profile
//                    GlobalData.getInstance().setCurrentEditProfile(mDeviceConfigProfileSpinner.getSelectedItem().toString());
//                }
//                mActivity.getMenu().performIdentifierAction(R.id.action_profiles, 0);
//            }
//        });

        mApplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mDeviceConfigProfileSpinner.getSelectedItemPosition() == INVALID_POSITION) {
                    Toast.makeText(mContext, mContext.getString(R.string.device_configuration_no_profile), Toast.LENGTH_SHORT).show();
                    return;
                }

                String deviceListJson;
                if (mSelectionTracker.hasSelection()) {
                    // load data
                    ArrayList<DeviceData> newList = new ArrayList<>();
                    for (DeviceData device : mDeviceItemList) {
                        if (device.isSelected()) {
                            // clear failure status before rerun
                            device.setStatus(DeviceStatus.NONE);
                            newList.add(device);
                        }
                    }
                    deviceListJson = ConversionUtil.deviceListToJson(newList);
                    // and then reset
                    mAdapter.clearAllSelection();
                } else {
                    Toast.makeText(mContext, mContext.getString(R.string.device_configuration_no_device_selected), Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(mContext, RunTaskActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_JSON_DEVICE_LIST, deviceListJson);
                intent.setAction(Constants.INTENT_ACTION_CONFIG_DEVICE);
                Gson gson = new Gson();
                String profileName = mDeviceConfigProfileSpinner.getSelectedItem().toString();
                mCurrentProfile = GlobalData.getInstance().getProfileMap().get(profileName);
                intent.putExtra(Constants.INTENT_EXTRA_JSON_DEVICE_PROFILE, gson.toJson(mCurrentProfile));
                startActivityForResult(intent, Constants.INTENT_REQUEST_CODE_TASK_BATCH_CONFIG_DEVICE);
            }
        });

        mDeviceCountTextView.setText(String.format(getResources().getString(R.string.device_configuration_device_selected), 0, 0));
        mDeviceConfigProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

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
                    mActivity.showLoader(true, mContext.getString(R.string.device_configuration_scanning_wifi));
                    mActivity.getMenu().setGroupEnabled(R.id.menu_group, false);
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

//            @Override
//            public void onItemStateChanged(@NonNull Object key, boolean selected) {
//                super.onItemStateChanged(key, selected);
//            }
        });

        updateProfileSpinner(mCurrentDevice);
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
                mActivity.getMenu().setGroupEnabled(R.id.menu_group, true);
                mIsReceiverUnregistered = true;
                mContext.unregisterReceiver(receiverWifi);
            }
        }
    };

    public void updateProfileSpinner(DeviceType newDevice) {
        if (newDevice == null) {
            // get current one, and refresh profile list (after profile updated)
            newDevice = mCurrentDevice;
        }

        // remember last item, if it is not removed after refresh, reselect it.
        String lastProfile = null;
        if (mDeviceConfigProfileSpinner.getSelectedItemPosition() != INVALID_POSITION) {
            lastProfile = mDeviceConfigProfileSpinner.getSelectedItem().toString();
        }
        Log.d(TAG, "last profile: " + lastProfile);

//        String[] array = GlobalData.getInstance().getProfileMap().keySet().toArray(new String[0]);
        // prepare array filtered by device type
        ArrayList<String> filteredProfiles = new ArrayList<>();
        for (DeviceProfile p : GlobalData.getInstance().getProfileMap().values()) {
            if (p.getDeviceType() == newDevice) {
                filteredProfiles.add(p.getName());
            }
        }

        // TODO select the current item after each refresh spinner using current name or new name
        updateSpinner(mDeviceConfigProfileSpinner, filteredProfiles.toArray(new String[0]));

        if (lastProfile != null) {
            setSelection(mDeviceConfigProfileSpinner, lastProfile);
            Log.d(TAG, "last profile reused: " + lastProfile);
        }
    }

    private void setSelection(Spinner spinner, String value) {
        if (value != null) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
            for (int i = adapter.getCount() - 1; i >= 0; --i) {
                if (value.equals(adapter.getItem(i))) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void updateSpinner(Spinner spinner, String[] array) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_spinner_item, array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void updateSpinner(Spinner spinner, @ArrayRes int resArrayId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext,
                resArrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}
