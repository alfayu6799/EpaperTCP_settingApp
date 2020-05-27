package com.yonglin.pdc.epaper_setting.fragments;

import android.content.Context;
import android.content.Intent;
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

import com.google.gson.Gson;
import com.yonglin.pdc.epaper_setting.MainActivity;
import com.yonglin.pdc.epaper_setting.R;
import com.yonglin.pdc.epaper_setting.RunTaskActivity;
import com.yonglin.pdc.epaper_setting.SelectWifiDialogActivity;
import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.data.DeviceData;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.DeviceType;
import com.yonglin.pdc.epaper_setting.data.SecurityType;
import com.yonglin.pdc.epaper_setting.utils.ConversionUtil;

import java.util.ArrayList;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class DeviceConfigurationFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "DeviceConfigFragment";

    private Context mContext;
    private MainActivity mActivity;

    private Button mApplyButton;
    private Spinner mDeviceTypeSpinner;
    private TextView mDeviceSsidTextView;
    private ImageButton mDeviceSearchWifiButton;

    private ImageButton mLogButton;

    private DeviceProfile mCurrentProfile = new DeviceProfile();

    private SettingsFragment mSettingsFragment;

    public static DeviceConfigurationFragment newInstance() {
        return new DeviceConfigurationFragment();
    }

    public DeviceConfigurationFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View deviceRootView = inflater.inflate(R.layout.fragment_device_configuration, container, false);
        // binding items outside shared layout
        mDeviceTypeSpinner = deviceRootView.findViewById(R.id.device_configuration_device_type_list);
        mApplyButton = deviceRootView.findViewById(R.id.device_configuration_apply);
        mLogButton = deviceRootView.findViewById(R.id.device_configuration_show_log);

        mDeviceSsidTextView = deviceRootView.findViewById(R.id.device_configuration_device_ssid);
        mDeviceSearchWifiButton = deviceRootView.findViewById(R.id.device_configuration_device_ssid_scanner_button);

        mActivity = (MainActivity) getActivity();
        mSettingsFragment = (SettingsFragment) getChildFragmentManager().findFragmentById(R.id.device_configuration_settings_container);

        return deviceRootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            switch (requestCode) {
                case Constants.INTENT_REQUEST_CODE_PICK_DEVICE_SSID:
                    if (resultCode == RESULT_OK && data.getExtras() != null) {
                        mDeviceSsidTextView.setText(data.getExtras().getString(Constants.INTENT_EXTRA_SSID));
                    }
                    break;
                case Constants.INTENT_REQUEST_CODE_TASK_BATCH_CONFIG_DEVICE:
                    if (resultCode == RESULT_OK && data.getExtras() != null) {
                        ArrayList<DeviceData> deviceItemList = ConversionUtil.deviceListFromJson(data.getStringExtra(Constants.INTENT_EXTRA_JSON_DEVICE_LIST));
                        DeviceData deviceData = deviceItemList.get(0);
                        // TODO add something to ui to better displaying result
                        Log.i(TAG, "Device status: " + deviceData.getStatus());
                        Toast.makeText(mContext, deviceData.getStatus().toString(), Toast.LENGTH_LONG).show();
                    }
                    break;
                default:
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // use fixed profile
        mCurrentProfile.setUuid(UUID.fromString(Constants.ONE_TIME_PROFILE_ID));

        // setup listeners
        mLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, RunTaskActivity.class));
            }
        });

        mDeviceSearchWifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, SelectWifiDialogActivity.class);
                // Do intent extra
                String prefix = DeviceType.getPrefix(DeviceType.parseInt(mDeviceTypeSpinner.getSelectedItemPosition()));
                intent.putExtra(Constants.INTENT_EXTRA_DEVICE_PREFIX, prefix);

                startActivityForResult(intent, Constants.INTENT_REQUEST_CODE_PICK_DEVICE_SSID);
            }
        });

        mApplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ConversionUtil.isTextViewEmpty(mDeviceSsidTextView)) {
                    Toast.makeText(mContext, mContext.getString(R.string.device_configuration_no_device), Toast.LENGTH_SHORT).show();
                    return;
                }

                mCurrentProfile = readCurrentProfile();
                if (!validateInput(mCurrentProfile)) {
                    return;
                }

                // create a list with just one device item to reuse the same RunTaskActivity
                ArrayList<DeviceData> newList = new ArrayList<>();
                // TODO optional: refactor find ssid page to return ScanResult for security type.
                boolean isRtm = (mCurrentProfile.getDeviceType() == DeviceType.RTM);
                DeviceData deviceData = new DeviceData(mDeviceSsidTextView.getText().toString(), isRtm ? SecurityType.PSK : SecurityType.OPEN);
                deviceData.setSelected(true);
                newList.add(deviceData);

                Intent intent = new Intent(mContext, RunTaskActivity.class);
                intent.putExtra(Constants.INTENT_EXTRA_JSON_DEVICE_LIST, ConversionUtil.deviceListToJson(newList));
                intent.setAction(Constants.INTENT_ACTION_CONFIG_DEVICE);
                Gson gson = new Gson();
                intent.putExtra(Constants.INTENT_EXTRA_JSON_DEVICE_PROFILE, gson.toJson(mCurrentProfile));
                startActivityForResult(intent, Constants.INTENT_REQUEST_CODE_TASK_BATCH_CONFIG_DEVICE);
            }
        });

        updateSpinner(mDeviceTypeSpinner, R.array.device_types);
        mDeviceTypeSpinner.post(new Runnable() {
            @Override
            public void run() {
                // populate init values
                mSettingsFragment.setProfile(mCurrentProfile, false);
                mDeviceTypeSpinner.setOnItemSelectedListener(DeviceConfigurationFragment.this);
            }
        });
        Log.d(TAG, "onViewCreated done.");
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }


    /**
     * respond to spinners on this page, the reset flow is from top to down (resetting items
     * on the top will reset all children), while the ip setting alone is a separate group.
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDeviceTypeSpinner) {
            Log.d(TAG, "Fired: device type spinner,pos: " + position);
            DeviceType deviceType = DeviceType.parseInt(mDeviceTypeSpinner.getSelectedItemPosition());
            mSettingsFragment.setDeviceType(deviceType);
            mCurrentProfile.setDeviceType(deviceType);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }

    /**
     * Read current input on the form to current profile.
     *
     * @return device profile
     */
    private DeviceProfile readCurrentProfile() {
        DeviceProfile settingsProfile = mSettingsFragment.readCurrentProfile();
        settingsProfile.setName("__DUMMY_ONE_TIME_PROFILE__"); // set the name for debugging
        // TODO for profile loading flow, copy the certs to single device run profile location
//        settingsProfile.setUuid(mCurrentProfile.getUuid());
        settingsProfile.setUuid(UUID.fromString(Constants.ONE_TIME_PROFILE_ID));
        settingsProfile.setDeviceType(DeviceType.parseInt(mDeviceTypeSpinner.getSelectedItemPosition()));
        return settingsProfile;
    }


    private void updateSpinner(Spinner spinner, @ArrayRes int resArrayId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext,
                resArrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     * validate a device profile and prompt errors
     *
     * @param profile
     * @return
     */
    private boolean validateInput(DeviceProfile profile) {
        int resStringId = ConversionUtil.validateInput(profile);
        if (resStringId == 0) {
            return true;
        } else {
            Toast.makeText(mContext, mContext.getString(resStringId), Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
