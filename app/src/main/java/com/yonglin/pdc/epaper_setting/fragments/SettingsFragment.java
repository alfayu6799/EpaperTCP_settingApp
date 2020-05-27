package com.yonglin.pdc.epaper_setting.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
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
import com.yonglin.pdc.epaper_setting.SelectWifiDialogActivity;
import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.DeviceType;
import com.yonglin.pdc.epaper_setting.data.EapMethod;
import com.yonglin.pdc.epaper_setting.data.IpType;
import com.yonglin.pdc.epaper_setting.data.SecurityType;
import com.yonglin.pdc.epaper_setting.tasks.CopyFileAsyncTask;
import com.yonglin.pdc.epaper_setting.utils.ConversionUtil;

import java.util.UUID;

import static android.app.Activity.RESULT_OK;
import static com.yonglin.pdc.epaper_setting.data.IpType.DHCP;
import static com.yonglin.pdc.epaper_setting.data.IpType.Static_IP;

public class SettingsFragment extends Fragment
        implements AdapterView.OnItemSelectedListener, TextWatcher {
    private static final String TAG = "SettingsFragment";

    private Context mContext;
    private MainActivity mActivity;

    // keep a snapshot for comparison
    private DeviceProfile mOldProfile;

    // state control
    private boolean mIsBatchMode;
    private boolean mIsChanged = false;
    private boolean mIsPopulating = false;
    private DeviceType mDeviceType = DeviceType.EPD_78;
    private UUID mUuid;

    // notify parent fragment for changes
    OnSettingsChangedListener mCallback;

    // shared settings ui
    private TextView mServerIpTextView;
    private TextView mServerPortTextView;
    private TextView mSsidTextView;
    private Spinner mSecuritySpinner;
    private TextView mPasswordView;
    private CheckBox mShowPasswordCheckBox;
    private Spinner mIpSettingsSpinner;
    private ImageButton mSearchWifiButton;
    // profile static ip fields
    private TextView mIpAddressView;
    private TextView mGatewayView;
    private TextView mSubnetMaskView;
    private TextView mDnsView;
    // eap
    private Spinner mEapMethodSpinner;
    private Spinner mPhase2Spinner;
    private TextView mEapIdentityView;
    private TextView mEapAnonymousView;
    // cert saved status and open file buttons.
    private TextView mEapCaCertStatus;
    private TextView mEapClientCertStatus;
    private TextView mEapPrivateKeyStatus;
    private ImageButton mEapCaCertClearButton;
    private ImageButton mEapClientCertClearButton;
    private ImageButton mEapPrivateKeyClearButton;
    private ImageButton mEapCaCertButton;
    private ImageButton mEapClientCertButton;
    private ImageButton mEapPrivateKeyButton;
    // RTM MQTT fields
    private TextView mMqttTopicTextView;
    private TextView mMqttUsernameTextView;
    private TextView mMqttPasswordTextView;
    // layouts visible controls
    private View mStaticIpLayout;
    private View mSecurityFieldLayout;
    private View mEapLayout;
    private View mEapClientCertLayout;
    private View mEapPrivateKeyLayout;
    private View mEapPhase2Layout;
    private View mPasswordLayout;
    private View mAnonymousLayout;
    private View mServerPortLayout;
    private View mMqttLayout;

    public void setOnSettingsChangedListener(OnSettingsChangedListener callback) {
        this.mCallback = callback;
    }

    public void setProfile(DeviceProfile profile, boolean isBatchMode) {
        if (profile != null) {
            this.mUuid = profile.getUuid();
            this.mDeviceType = profile.getDeviceType();
            Gson gson = new Gson();
            String profileString = gson.toJson(profile);
            mOldProfile = gson.fromJson(profileString, DeviceProfile.class);
        } else {
            this.mUuid = null;
            this.mDeviceType = DeviceType.EPD_78;
            this.mOldProfile = null;
        }
        this.mIsBatchMode = isBatchMode;

        populateProfile(profile, false);
    }

    public void setDeviceType(DeviceType deviceType) {
        Log.d(TAG, "setDeviceType: " + deviceType);
        this.mDeviceType = deviceType;
        populateProfile(null, true);
    }

    public boolean isChanged() {
        if (mIsChanged) return true;
        // empty form before add flow
        if (mOldProfile == null) return false;
        if (readCurrentProfile().compareTo(mOldProfile) != 0) {
            return true;
        }
        return false;
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    public SettingsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View settingsView = inflater.inflate(R.layout.fragment_settings, container, false);
        // binding
        mSsidTextView = settingsView.findViewById(R.id.ssid);
        mPasswordView = settingsView.findViewById(R.id.password);
        mShowPasswordCheckBox = settingsView.findViewById(R.id.show_password);
        mIpSettingsSpinner = settingsView.findViewById(R.id.ip_settings);
        mEapIdentityView = settingsView.findViewById(R.id.identity);
        mEapAnonymousView = settingsView.findViewById(R.id.anonymous);
        mSecuritySpinner = settingsView.findViewById(R.id.device_profile_encrypt_spinner);
        mEapMethodSpinner = settingsView.findViewById(R.id.method);
        mPhase2Spinner = settingsView.findViewById(R.id.phase2);
        mIpAddressView = settingsView.findViewById(R.id.ipaddress);
        mGatewayView = settingsView.findViewById(R.id.gateway);
        mSubnetMaskView = settingsView.findViewById(R.id.network_subnet_mask);
        mDnsView = settingsView.findViewById(R.id.dns);
        mServerIpTextView = settingsView.findViewById(R.id.device_profile_server_ip_edittext);
        mServerPortTextView = settingsView.findViewById(R.id.device_profile_server_port_edittext);
        mMqttTopicTextView = settingsView.findViewById(R.id.device_profile_mqtt_topic_edittext);
        mMqttUsernameTextView = settingsView.findViewById(R.id.device_profile_mqtt_username_edittext);
        mMqttPasswordTextView = settingsView.findViewById(R.id.device_profile_mqtt_password_edittext);
        mEapCaCertStatus = settingsView.findViewById(R.id.ca_cert_status);
        mEapCaCertClearButton = settingsView.findViewById(R.id.ca_certificate_clear_button);
        mEapCaCertButton = settingsView.findViewById(R.id.ca_certificate_open_file_button);
        mEapClientCertStatus = settingsView.findViewById(R.id.client_cert_status);
        mEapClientCertClearButton = settingsView.findViewById(R.id.client_certificate_clear_button);
        mEapClientCertButton = settingsView.findViewById(R.id.client_certificate_open_file_button);
        mEapPrivateKeyStatus = settingsView.findViewById(R.id.private_key_status);
        mEapPrivateKeyClearButton = settingsView.findViewById(R.id.private_key_clear_button);
        mEapPrivateKeyButton = settingsView.findViewById(R.id.private_key_open_file_button);
        mSearchWifiButton = settingsView.findViewById(R.id.ssid_scanner_button);
        // layouts visible controls
        mStaticIpLayout = settingsView.findViewById(R.id.staticip);
        mSecurityFieldLayout = settingsView.findViewById(R.id.security_fields);
        mEapLayout = settingsView.findViewById(R.id.eap);
        mEapClientCertLayout = settingsView.findViewById(R.id.l_client_cert);
        mEapPrivateKeyLayout = settingsView.findViewById(R.id.l_private_key);
        mEapPhase2Layout = settingsView.findViewById(R.id.l_phase2);
        mPasswordLayout = settingsView.findViewById(R.id.password_layout);
        mAnonymousLayout = settingsView.findViewById(R.id.l_anonymous);
        mServerPortLayout = settingsView.findViewById(R.id.device_profile_server_port_layout);
        mMqttLayout = settingsView.findViewById(R.id.device_profile_mqtt_fields);

        mActivity = (MainActivity) getActivity();

        // listen to changes
        mSsidTextView.addTextChangedListener(this);
        mPasswordView.addTextChangedListener(this);
        mEapIdentityView.addTextChangedListener(this);
        mEapAnonymousView.addTextChangedListener(this);
        mIpAddressView.addTextChangedListener(this);
        mGatewayView.addTextChangedListener(this);
        mSubnetMaskView.addTextChangedListener(this);
        mDnsView.addTextChangedListener(this);
        mServerIpTextView.addTextChangedListener(this);
        mServerPortTextView.addTextChangedListener(this);
        mMqttTopicTextView.addTextChangedListener(this);
        mMqttUsernameTextView.addTextChangedListener(this);
        mMqttPasswordTextView.addTextChangedListener(this);
        mEapCaCertStatus.addTextChangedListener(this);
        mEapClientCertStatus.addTextChangedListener(this);
        mEapPrivateKeyStatus.addTextChangedListener(this);
        return settingsView;
    }

    // callback to update ui status after saving the cert files
    private CopyFileAsyncTask.SyncTaskCallback mCopyFileAsyncTaskCallBack = new CopyFileAsyncTask.SyncTaskCallback() {
        @Override
        public void onCompleted(final String targetFileName) {
            Log.d(TAG, "onCompleted: " + targetFileName);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (targetFileName) {
                        case DeviceProfile.CA_CERT_FILE:
                            mEapCaCertStatus.setText(R.string.file_status_saved);
                            break;
                        case DeviceProfile.CLIENT_CERT_FILE:
                            mEapClientCertStatus.setText(R.string.file_status_saved);
                            break;
                        case DeviceProfile.PRIVATE_KEY_FILE:
                            mEapPrivateKeyStatus.setText(R.string.file_status_saved);
                            break;
                        default:
                    }
                }
            });
        }

        @Override
        public void onFailure(final String targetFileName, final String error) {
            Log.e(TAG, "onFailure: " + targetFileName);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, error, Toast.LENGTH_SHORT).show();
                    switch (targetFileName) {
                        case DeviceProfile.CA_CERT_FILE:
                            mEapCaCertStatus.setText(R.string.file_status_missing);
                            break;
                        case DeviceProfile.CLIENT_CERT_FILE:
                            mEapClientCertStatus.setText(R.string.file_status_missing);
                            break;
                        case DeviceProfile.PRIVATE_KEY_FILE:
                            mEapPrivateKeyStatus.setText(R.string.file_status_missing);
                            break;
                        default:
                    }
                }
            });
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.e(TAG, "onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            switch (requestCode) {
                case Constants.INTENT_REQUEST_CODE_PICK_SSID:
                    if (resultCode == RESULT_OK && data.getExtras() != null) {
                        mSsidTextView.setText(data.getExtras().getString(Constants.INTENT_EXTRA_SSID));
                    }
                    break;
                case Constants.INTENT_REQUEST_CODE_PICK_CA_CERT_FILE:
                    if (resultCode == RESULT_OK && data.getData() != null) {
                        Uri uri = data.getData();
                        Log.d(TAG, "Uri: " + uri.toString());
                        new CopyFileAsyncTask(mCopyFileAsyncTaskCallBack).execute(mContext,
                                mUuid, DeviceProfile.CA_CERT_FILE, uri);
                    }
                    break;
                case Constants.INTENT_REQUEST_CODE_PICK_CLIENT_CERT_FILE:
                    if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                        Uri uri = data.getData();
                        Log.d(TAG, "Uri: " + uri.toString());
                        new CopyFileAsyncTask(mCopyFileAsyncTaskCallBack).execute(mContext,
                                mUuid, DeviceProfile.CLIENT_CERT_FILE, uri);
                    }
                    break;
                case Constants.INTENT_REQUEST_CODE_PICK_PRIVATE_KEY_FILE:
                    if (resultCode == RESULT_OK && data.getData() != null) {
                        Uri uri = data.getData();
                        Log.d(TAG, "Uri: " + uri.toString());
                        new CopyFileAsyncTask(mCopyFileAsyncTaskCallBack).execute(mContext,
                                mUuid, DeviceProfile.PRIVATE_KEY_FILE, uri);
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
        updateSpinner(mSecuritySpinner, R.array.wifi_security_types);
        updateSpinner(mEapMethodSpinner, R.array.wifi_eap_method);
        updateSpinner(mPhase2Spinner, R.array.wifi_phase2_entries);

        mShowPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int pos = mPasswordView.getSelectionEnd();
                mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT
                        | (isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        : InputType.TYPE_TEXT_VARIATION_PASSWORD));
                if (pos >= 0) {
                    ((EditText) mPasswordView).setSelection(pos);
                }
            }
        });

        // search ssid button
        mSearchWifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, SelectWifiDialogActivity.class);
                startActivityForResult(intent, Constants.INTENT_REQUEST_CODE_PICK_SSID);
            }
        });

        mEapCaCertClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEapCaCertStatus.setText(R.string.file_status_missing);
            }
        });

        mEapClientCertClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEapClientCertStatus.setText(R.string.file_status_missing);
            }
        });

        mEapPrivateKeyClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEapPrivateKeyStatus.setText(R.string.file_status_missing);
            }
        });

        mEapCaCertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,
                        mContext.getString(R.string.select_file_title)),
                        Constants.INTENT_REQUEST_CODE_PICK_CA_CERT_FILE);
            }
        });

        mEapClientCertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,
                        mContext.getString(R.string.select_file_title)),
                        Constants.INTENT_REQUEST_CODE_PICK_CLIENT_CERT_FILE);
            }
        });

        mEapPrivateKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,
                        mContext.getString(R.string.select_file_title)),
                        Constants.INTENT_REQUEST_CODE_PICK_PRIVATE_KEY_FILE);
            }
        });

        Log.d(TAG, "onViewCreated done.");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    // we will be resetting fields on spinner changes so no need to add this in onItemSelected()
    // as text changes always get triggered on spinner changes
    private void updateChangeStatus() {
        if (!mIsPopulating) {
            Log.e(TAG, "User event updateChangeStatus");
//            if (!mIsChanged) {
                // changed just now, fire event
                Log.e(TAG, "FIRE event: ");
                if (mCallback != null) {
                    mCallback.onChanged();
                }
//            }
            mIsChanged = true;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        Log.d(TAG, "Text changed: " + s);
        updateChangeStatus();
    }

    @Override
    public void afterTextChanged(Editable s) {
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
        if (parent == mSecuritySpinner) {
            Log.d(TAG, "Fired: Security spinner,pos: " + position);
            resetSecurityFields();
            final SecurityType type = SecurityType.parseInt(position);
            showSecurityFields(type);
        } else if (parent == mEapMethodSpinner) {
            Log.d(TAG, "Fired: EAP method spinner,pos: " + position);
            resetEapMethodFields();
            showEapFieldsByMethod(EapMethod.parseInt(mEapMethodSpinner.getSelectedItemPosition()));
        } else if (parent == mIpSettingsSpinner) {
            Log.d(TAG, "Fired: IP settings spinner,pos: " + position);
            resetIpFields();
        } else { // mPhase2Spinner
            Log.d(TAG, "Fired: Other spinner: " + parent.toString());
            // nothing to do, only one item supported now.
        }
        showIpConfigFields();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }


    private void showIpConfigFields() {
        if (mIpSettingsSpinner.getSelectedItemPosition() == IpType.getIntValue(Static_IP)) {
            mStaticIpLayout.setVisibility(View.VISIBLE);
        } else {
            mStaticIpLayout.setVisibility(View.GONE);
        }
    }

    private void showSecurityFields(SecurityType securityType) {
        if (securityType == SecurityType.OPEN && mDeviceType != DeviceType.RTM) {
            mSecurityFieldLayout.setVisibility(View.GONE);
            return;
        }

        mSecurityFieldLayout.setVisibility(View.VISIBLE);
        mPasswordLayout.setVisibility(View.VISIBLE);

        if (securityType == SecurityType.EAP) {
            mEapLayout.setVisibility(View.VISIBLE);
            showEapFieldsByMethod(EapMethod.parseInt(mEapMethodSpinner.getSelectedItemPosition()));
        } else {
            mEapLayout.setVisibility(View.GONE);
        }
    }


    private void showEapFieldsByMethod(EapMethod eapMethod) {
        switch (eapMethod) {
            case TLS:
                mEapPhase2Layout.setVisibility(View.GONE);
                mEapAnonymousView.setText("");
                mAnonymousLayout.setVisibility(View.GONE);
                // TLS -> change behavior, keeping password field but don't check it.
//                mPasswordView.setText("");
//                mPasswordLayout.setVisibility(View.GONE);
                mPasswordLayout.setVisibility(View.VISIBLE);
                mEapClientCertLayout.setVisibility(View.VISIBLE);
                mEapPrivateKeyLayout.setVisibility(View.VISIBLE);
                break;
            case PEAP:
                mEapPhase2Layout.setVisibility(View.VISIBLE);
                mAnonymousLayout.setVisibility(View.VISIBLE);
                mPasswordLayout.setVisibility(View.VISIBLE);
                mEapClientCertLayout.setVisibility(View.GONE);
                mEapPrivateKeyLayout.setVisibility(View.GONE);
                break;
            case TTLS:
                mPasswordLayout.setVisibility(View.VISIBLE);
                mEapPhase2Layout.setVisibility(View.VISIBLE);
                mAnonymousLayout.setVisibility(View.VISIBLE);
                mEapClientCertLayout.setVisibility(View.GONE);
                mEapPrivateKeyLayout.setVisibility(View.GONE);
                break;
        }
    }

    private void populateProfile(final DeviceProfile profile, final boolean newChangedStatus) {
        mIsPopulating = true;
        Log.e(TAG, "populateProfile-start");
        // turn off the spinner event handler while we updates
        mIpSettingsSpinner.setOnItemSelectedListener(null);
        mEapMethodSpinner.setOnItemSelectedListener(null);
        mPhase2Spinner.setOnItemSelectedListener(null);
        mSecuritySpinner.setOnItemSelectedListener(null);

        // reset fields
        resetAllFields();

        // update by profile
        if (profile != null && (profile.getServerIp() != null) && !TextUtils.isEmpty(profile.getName())) {
            // ui
            showSecurityFields(profile.getSecurityType());
            mSecuritySpinner.setSelection(SecurityType.getIntValue(profile.getSecurityType()));
            if (profile.getSecurityType() == SecurityType.EAP) {
                mEapMethodSpinner.setSelection(profile.getEapMethod().getIntValue());
                mPhase2Spinner.setSelection(profile.getEapPhase2Auth());
            }

            if (profile.getIpType() == Static_IP) {
                mIpSettingsSpinner.setSelection(IpType.getIntValue(Static_IP));
            } else {
                mIpSettingsSpinner.setSelection(IpType.getIntValue(DHCP));
            }

            boolean isRtm = (profile.getDeviceType() == DeviceType.RTM);
            if (!isRtm) {
                if (mIsBatchMode) {
                    updateSpinner(mEapMethodSpinner, R.array.wifi_eap_method_profile);
                } else {
                    updateSpinner(mEapMethodSpinner, R.array.wifi_eap_method);
                }
            }
            setRtmView(isRtm);

            showIpConfigFields();

            // populate data
            Log.d(TAG, "Populating profile data: " + profile.getName());
            mServerIpTextView.setText(profile.getServerIp());
            mServerPortTextView.setText(profile.getServerPort());
            mSsidTextView.setText(profile.getServerSsid());
            if (profile.getSecurityType() == SecurityType.EAP) {
                mEapIdentityView.setText(profile.getEapIdentity());
                mEapAnonymousView.setText(profile.getEapAnonymousIdentity());
                mEapCaCertStatus.setText(profile.isEapCaCertSaved() ? R.string.file_status_saved
                        : R.string.file_status_missing);
                mEapClientCertStatus.setText(profile.isEapClientCertSaved() ? R.string.file_status_saved
                        : R.string.file_status_missing);
                mEapPrivateKeyStatus.setText(profile.isEapPrivateKeySaved() ? R.string.file_status_saved
                        : R.string.file_status_missing);
            }
            mPasswordView.setText(profile.getPassword());

            if (profile.getIpType() == Static_IP) {
                mIpAddressView.setText(profile.getStaticIp());
                mGatewayView.setText(profile.getGatewayIp());
                mSubnetMaskView.setText(profile.getSubnetMask());
                mDnsView.setText(profile.getDnsServer());
            }

            if (profile.getDeviceType() == DeviceType.RTM) {
                mMqttTopicTextView.setText(profile.getRtmMqttTopic());
                mMqttUsernameTextView.setText(profile.getRtmMqttUsername());
                mMqttPasswordTextView.setText(profile.getRtmMqttPassword());
            }
        } else { // no profile, set the default screen
            boolean isDefaultToRtm = (mDeviceType == DeviceType.RTM);
            if (!isDefaultToRtm) {
                if (mIsBatchMode) {
                    updateSpinner(mEapMethodSpinner, R.array.wifi_eap_method_profile);
                } else {
                    updateSpinner(mEapMethodSpinner, R.array.wifi_eap_method);
                }
            }
            setRtmView(isDefaultToRtm);
            // provide defaults
            mMqttTopicTextView.setText(Constants.RTM_MQTT_TOPIC);
            mMqttUsernameTextView.setText(Constants.RTM_MQTT_USERNAME);
            mMqttPasswordTextView.setText(Constants.RTM_MQTT_PASSWORD);
        }
        // enabled the listener again, post it to handler so it does not get enabled too soon.
        mIpSettingsSpinner.post(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "populateProfile-end");
                mIpSettingsSpinner.setOnItemSelectedListener(SettingsFragment.this);
                mEapMethodSpinner.setOnItemSelectedListener(SettingsFragment.this);
                mPhase2Spinner.setOnItemSelectedListener(SettingsFragment.this);
                mSecuritySpinner.setOnItemSelectedListener(SettingsFragment.this);
                // restore status
                mIsChanged = newChangedStatus;
                mIsPopulating = false;
            }
        });
    }

    /**
     * Read current input on the form as a profile.
     *
     * @return device profile
     */
    public DeviceProfile readCurrentProfile() {
        DeviceProfile profile = new DeviceProfile();
        if (mUuid == null) {
            Log.d(TAG, "New device profile returned.");
        } else {
            profile.setUuid(mUuid);
        }

        profile.setDeviceType(mDeviceType);

        if (!ConversionUtil.isTextViewEmpty(mServerIpTextView)) {
            profile.setServerIp(mServerIpTextView.getText().toString());
        } else {
            profile.setServerIp(null);
        }

        if (!ConversionUtil.isTextViewEmpty(mServerPortTextView)) {
            profile.setServerPort(mServerPortTextView.getText().toString());
        } else {
            profile.setServerPort(null);
        }

        if (!ConversionUtil.isTextViewEmpty(mSsidTextView)) {
            profile.setServerSsid(mSsidTextView.getText().toString());
        } else {
            profile.setServerSsid(null);
        }

        if (profile.getDeviceType() == DeviceType.RTM) {
            if (!ConversionUtil.isTextViewEmpty(mMqttTopicTextView)) {
                profile.setRtmMqttTopic(mMqttTopicTextView.getText().toString());
            } else {
                profile.setRtmMqttTopic(null);
            }

            if (!ConversionUtil.isTextViewEmpty(mMqttUsernameTextView)) {
                profile.setRtmMqttUsername(mMqttUsernameTextView.getText().toString());
            } else {
                profile.setRtmMqttUsername(null);
            }

            if (!ConversionUtil.isTextViewEmpty(mMqttPasswordTextView)) {
                profile.setRtmMqttPassword(mMqttPasswordTextView.getText().toString());
            } else {
                profile.setRtmMqttPassword(null);
            }
        } else {
            profile.setRtmMqttTopic(null);
            profile.setRtmMqttUsername(null);
            profile.setRtmMqttPassword(null);
        }

        profile.setSecurityType(SecurityType.parseInt(mSecuritySpinner.getSelectedItemPosition()));

        if ((profile.getSecurityType() != SecurityType.OPEN) && !ConversionUtil.isTextViewEmpty(mPasswordView)) {
            profile.setPassword(mPasswordView.getText().toString());
        } else {
            profile.setPassword(null);
        }

        if (profile.getSecurityType() == SecurityType.EAP) {
            profile.setEapMethod(EapMethod.parseInt(mEapMethodSpinner.getSelectedItemPosition()));

            profile.setEapCaCertSaved(mEapCaCertStatus.getText().toString().
                    equals(getString(R.string.file_status_saved)));

            if (!ConversionUtil.isTextViewEmpty(mEapIdentityView)) {
                profile.setEapIdentity(mEapIdentityView.getText().toString());
            } else {
                profile.setEapIdentity(null);
            }

            if (profile.getEapMethod() == EapMethod.TLS) {
                profile.setEapClientCertSaved(mEapClientCertStatus.getText().toString().
                        equals(getString(R.string.file_status_saved)));
                profile.setEapPrivateKeySaved(mEapPrivateKeyStatus.getText().toString().
                        equals(getString(R.string.file_status_saved)));
            } else {
                profile.setEapPhase2Auth(mPhase2Spinner.getSelectedItemPosition());
                if (!ConversionUtil.isTextViewEmpty(mEapAnonymousView)) {
                    profile.setEapAnonymousIdentity(mEapAnonymousView.getText().toString());
                } else {
                    profile.setEapAnonymousIdentity(null);
                }
            }
        } else {
            profile.setEapMethod(EapMethod.PEAP);
            profile.setEapCaCertSaved(false);
            profile.setEapIdentity(null);
            profile.setEapClientCertSaved(false);
            profile.setEapPrivateKeySaved(false);
            profile.setEapPhase2Auth(0);
            profile.setEapAnonymousIdentity(null);
        }

        if (mIpSettingsSpinner.getSelectedItemPosition() == IpType.getIntValue(Static_IP)) {
            profile.setIpType(Static_IP);
            if (!ConversionUtil.isTextViewEmpty(mIpAddressView)) {
                profile.setStaticIp(mIpAddressView.getText().toString());
            } else {
                profile.setStaticIp(null);
            }
            if (!ConversionUtil.isTextViewEmpty(mGatewayView)) {
                profile.setGatewayIp(mGatewayView.getText().toString());
            } else {
                profile.setGatewayIp(null);
            }
            if (!ConversionUtil.isTextViewEmpty(mSubnetMaskView)) {
                profile.setSubnetMask(mSubnetMaskView.getText().toString());
            } else {
                profile.setSubnetMask(null);
            }
            if (!ConversionUtil.isTextViewEmpty(mDnsView)) {
                profile.setDnsServer(mDnsView.getText().toString());
            } else {
                profile.setDnsServer(null);
            }
        } else {
            profile.setIpType(IpType.DHCP);
            profile.setStaticIp(null);
            profile.setGatewayIp(null);
            profile.setSubnetMask(null);
            profile.setDnsServer(null);
        }
        return profile;
    }

    /**
     * show/hide ui if device type RTM is selected.
     *
     * @param enabled
     */
    private void setRtmView(boolean enabled) {
        // lock this two options.
        if (enabled) {
            mSecuritySpinner.setSelection(SecurityType.getIntValue(SecurityType.PSK));
            mIpSettingsSpinner.setSelection(IpType.getIntValue(DHCP));
            mPasswordLayout.setVisibility(View.VISIBLE);
        }
        mSecuritySpinner.setEnabled(!enabled);
        mIpSettingsSpinner.setEnabled(!enabled);
        // hide/show server port
        mServerPortLayout.setVisibility(enabled ? View.GONE : View.VISIBLE);
        // how/hide MQTT fields
        mMqttLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    /**
     * reset all fields when device type changed between EPD to RTM
     */
    private void resetAllFields() {
        Log.d(TAG, "RESET: All fields");
        mServerIpTextView.setText("");
        mServerPortTextView.setText("");
        mSsidTextView.setText("");
        mMqttTopicTextView.setText("");
        mMqttUsernameTextView.setText("");
        mMqttPasswordTextView.setText("");
        mSecuritySpinner.setSelection(0);
        showSecurityFields(SecurityType.OPEN);
        resetSecurityFields();
        mIpSettingsSpinner.setSelection(0);
        resetIpFields();
    }

    /**
     * reset fields when security type changed
     */
    private void resetSecurityFields() {
        Log.d(TAG, "RESET: Security fields");

        mEapMethodSpinner.setSelection(0);
        resetEapMethodFields();
    }

    /**
     * reset fields when eap method changed
     */
    private void resetEapMethodFields() {
        Log.d(TAG, "RESET: EAP fields");
        mPasswordView.setText(""); // for TLS
        mEapIdentityView.setText("");
        mEapAnonymousView.setText("");
        mEapCaCertStatus.setText(R.string.file_status_missing);
        mEapClientCertStatus.setText(R.string.file_status_missing);
        mEapPrivateKeyStatus.setText(R.string.file_status_missing);
        mPhase2Spinner.setSelection(0);
    }

    /**
     * reset ip fields when ip type changed
     */
    private void resetIpFields() {
        Log.d(TAG, "RESET: IP fields");
        mIpAddressView.setText("");
        mGatewayView.setText("");
        mSubnetMaskView.setText("");
        mDnsView.setText("");
    }

    private void updateSpinner(Spinner spinner, @ArrayRes int resArrayId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext,
                resArrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Tell parent Fragment content is changed
    public interface OnSettingsChangedListener {
        public void onChanged();
    }
}
