/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yonglin.pdc.epaper_setting.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textview.MaterialTextView;
import com.yonglin.pdc.epaper_setting.MainActivity;
import com.yonglin.pdc.epaper_setting.R;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.DeviceType;
import com.yonglin.pdc.epaper_setting.data.GlobalData;
import com.yonglin.pdc.epaper_setting.utils.ConversionUtil;
import com.yonglin.pdc.epaper_setting.utils.FileUtils;

import java.util.UUID;

public class ProfileFragment extends Fragment
        implements AdapterView.OnItemSelectedListener, SettingsFragment.OnSettingsChangedListener {
    private static final String TAG = "ProfileFragment";

    private Context mContext;
    private MainActivity mActivity;

//    private DeviceProfile mCurrentProfile = new DeviceProfile();

    // profile ui
    private Spinner mProfileSpinner;
    private ImageButton mAddProfileButton;
    private ImageButton mSaveProfileButton;
    private ImageButton mDeleteProfileButton;
    private Spinner mProfileDeviceTypeSpinner;

    private View mProfileContentView;

    private SettingsFragment mSettingsFragment;

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View profileRootView = inflater.inflate(R.layout.fragment_profile, container, false);

        // binding profiles ui
        mProfileSpinner = profileRootView.findViewById(R.id.device_profile_profile_name_spinner);
        mAddProfileButton = profileRootView.findViewById(R.id.device_profile_add_profile);
        mDeleteProfileButton = profileRootView.findViewById(R.id.device_profile_delete_profile);
        mSaveProfileButton = profileRootView.findViewById(R.id.device_profile_save_profile);
        mProfileDeviceTypeSpinner = profileRootView.findViewById(R.id.device_profile_device_type_list);

        mProfileContentView = profileRootView.findViewById(R.id.configPageView);

        mActivity = (MainActivity) getActivity();
        mSettingsFragment = (SettingsFragment) getChildFragmentManager().findFragmentById(R.id.device_profile_settings_container);
        return profileRootView;
    }

    @Override
    public void onChanged() {
        Log.e(TAG, "Lock profile");
        mProfileSpinner.setEnabled(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // add profile button
        mAddProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isProfileChanged()) {
                    confirmSaveProfiles();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle(mContext.getString(R.string.dialog_input_name_title));
                    View customView = LayoutInflater.from(mContext).inflate(R.layout.dialog_text_input, (ViewGroup) mActivity.findViewById(android.R.id.content), false);
                    final EditText profileNameEditText = customView.findViewById(R.id.dialog_edit_text);
                    final MaterialTextView warningTextView = customView.findViewById(R.id.dialog_text_input_warning);
                    builder.setView(customView);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    final AlertDialog dialog = builder.create();

                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            Button button = (dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                            button.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View view) {
                                    boolean isValidated = true;
                                    warningTextView.setVisibility(View.GONE);

                                    if (profileNameEditText.length() == 0) {
                                        warningTextView.setText(R.string.dialog_profile_confirm_name_invalid);
                                        warningTextView.setVisibility(View.VISIBLE);
                                        isValidated = false;
                                    }

                                    final String newName = profileNameEditText.getText().toString();

                                    if (GlobalData.getInstance().getProfileMap().containsKey(newName)) {
                                        warningTextView.setText(R.string.dialog_profile_confirm_name_exist);
                                        warningTextView.setVisibility(View.VISIBLE);
                                        isValidated = false;
                                    }

                                    Log.d(TAG, String.format("New name: %s, validated: %s", newName, isValidated));
                                    if (isValidated) {
                                        DeviceProfile newProfile = new DeviceProfile();
                                        newProfile.setName(newName);
                                        GlobalData.getInstance().getProfileMap().put(newName, newProfile);
                                        updateProfileSpinners();
                                        setSelection(mProfileSpinner, newName);
                                        populateProfile(null);
                                        // lock change
                                        mProfileSpinner.setEnabled(false);
                                        // enable edit
                                        mProfileContentView.setVisibility(View.VISIBLE);
                                        dialog.dismiss();
                                    }
                                }
                            });
                        }
                    });
                    dialog.show();
                }
            }
        });

        // delete profile button
        mDeleteProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GlobalData.getInstance().getProfileMap().size() > 0
                        && !TextUtils.isEmpty(mProfileSpinner.getSelectedItem().toString())) {
                    final String name = mProfileSpinner.getSelectedItem().toString();
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    View customView = LayoutInflater.from(mContext).inflate(R.layout.dialog_message, (ViewGroup) getActivity().findViewById(android.R.id.content), false);
                    final MaterialTextView msgTextView = customView.findViewById(R.id.dialog_text_view);
                    final String msg = String.format(mContext.getText(R.string.device_profile_confirm_delete).toString(), name);
                    msgTextView.setText(msg);
                    builder.setTitle(R.string.dialog_confirm_title);
                    builder.setView(customView);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            deleteProfile(name);
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }
            }
        });

        // save profile button
        mSaveProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProfiles(true);
            }
        });

        updateSpinner(mProfileDeviceTypeSpinner, R.array.device_types);

        String[] array = GlobalData.getInstance().getProfileMap().keySet().toArray(new String[0]);
        Log.d(TAG, "Init profile list: " + array.length);
        updateSpinner(mProfileSpinner, array);


        mProfileSpinner.post(new Runnable() {
            @Override
            public void run() {
                // populate init values
                if (GlobalData.getInstance().getProfileMap().size() > 0) {
                    // populate updated values
                    DeviceProfile profile = GlobalData.getInstance().getProfileMap().get(mProfileSpinner.getItemAtPosition(0).toString());
                    populateProfile(profile);
                    mProfileContentView.setVisibility(View.VISIBLE);
                } else {
                    populateProfile(null);
                    mProfileContentView.setVisibility(View.INVISIBLE);
                }
            }
        });

        mSettingsFragment.setOnSettingsChangedListener(this);
        Log.d(TAG, "onViewCreated done.");
    }


    public boolean isProfileChanged() {
        if (GlobalData.getInstance().getProfileMap().size() == 0) {
            return false;
        }
        final String profileName = mProfileSpinner.getSelectedItem().toString();
        Log.d(TAG, "isProfileChanged - check against: " + profileName);

        if (!mProfileSpinner.isEnabled()) {
            Log.d(TAG, "spinner still locked");
            return true;
        }

        if (GlobalData.getInstance().getSavedProfileMap().containsKey(profileName)) {
            DeviceProfile oldProfile = GlobalData.getInstance().getSavedProfileMap().get(profileName);
            DeviceProfile newProfile = readCurrentProfile();
            Log.d(TAG, "Comparing against saved profiles:\n" + oldProfile.toString() + "\n" + newProfile.toString());
            if (oldProfile.compareTo(newProfile) == 0) {
                Log.d(TAG, "Profile is not changed.");
                return false;
            }
        }
        return true;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        Log.d(TAG, "onAttach done.");
    }

    private void deleteProfile(String profileName) {
        if (GlobalData.getInstance().getProfileMap().containsKey(profileName)) {
            UUID uuid = GlobalData.getInstance().getProfileMap().get(profileName).getUuid();
            GlobalData.getInstance().getProfileMap().remove(profileName);
            updateProfileSpinners();
            if (GlobalData.getInstance().getProfileMap().size() == 0) {
                populateProfile(null);
                mProfileContentView.setVisibility(View.INVISIBLE);
            } else {
                mProfileSpinner.setEnabled(true);
                mProfileContentView.setVisibility(View.VISIBLE);
            }

            // persist changes
            FileUtils.saveProfiles(mContext, GlobalData.getInstance().getProfileMap());

            // clean up cert files
            FileUtils.deleteProfileFolder(mContext, uuid);
        }
    }

    /**
     * ask the user if the profiles should be saved before leaving current screen, if not, reload
     * the saved copy from storage and discard everything. This is called when menu pressed and
     * back pressed, but not on switching profile or adding profile, allowing user to save it at once.
     */
    public void confirmSaveProfiles() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View customView = LayoutInflater.from(mContext).inflate(R.layout.dialog_message, (ViewGroup) getActivity().findViewById(android.R.id.content), false);
        final MaterialTextView msgTextView = customView.findViewById(R.id.dialog_text_view);
        msgTextView.setText(R.string.device_profile_confirm_save_before_leaving);
        builder.setTitle(R.string.dialog_confirm_title);
        builder.setView(customView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                saveProfiles(false);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

                if (GlobalData.getInstance().getProfileMap().size() > 0) {
                    final String name = mProfileSpinner.getSelectedItem().toString();
                    Log.e(TAG, name);
                    GlobalData.getInstance().getProfileMap().clear();

                    // load profiles back
                    GlobalData.getInstance().getProfileMap().putAll(GlobalData.getInstance().getSavedProfileMap());

                    // if current name is not saved, quit add flow and load the defaults
                    // , else load old data back.
                    if (GlobalData.getInstance().getProfileMap().containsKey(name)) {
                        populateProfile(GlobalData.getInstance().getProfileMap().get(name));
                    } else {
                        updateProfileSpinners();
                    }
                } else {
                    populateProfile(null);
                    mProfileContentView.setVisibility(View.INVISIBLE);
                }
                mProfileSpinner.setEnabled(true);
            }
        });
        builder.show();
    }

    private void saveProfiles(boolean showPrompt) {
        // trigger add profile flow instead
        if (GlobalData.getInstance().getProfileMap().size() == 0) {
            Toast.makeText(mContext, mContext.getString(R.string.dialog_profile_add_new_first), Toast.LENGTH_LONG).show();
            mAddProfileButton.callOnClick();
            return;
        }
        final String name = mProfileSpinner.getSelectedItem().toString();
        final DeviceProfile profile = readCurrentProfile();
        if (!validateInput(profile)) {
            return;
        }

        if (showPrompt) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            View customView = LayoutInflater.from(mContext).inflate(R.layout.dialog_message, (ViewGroup) getActivity().findViewById(android.R.id.content), false);
            final MaterialTextView msgTextView = customView.findViewById(R.id.dialog_text_view);
            msgTextView.setText(R.string.device_profile_confirm_save);
            builder.setTitle(R.string.dialog_confirm_title);
            builder.setView(customView);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    // replace() is after api 24, so remove then put it
                    GlobalData.getInstance().getProfileMap().remove(name);
                    GlobalData.getInstance().getProfileMap().put(name, profile);
                    FileUtils.saveProfiles(mContext, GlobalData.getInstance().getProfileMap());
                    mProfileSpinner.setEnabled(true);
                    mProfileContentView.setVisibility(View.VISIBLE);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        } else {
            GlobalData.getInstance().getProfileMap().remove(name);
            GlobalData.getInstance().getProfileMap().put(name, profile);
            FileUtils.saveProfiles(mContext, GlobalData.getInstance().getProfileMap());
            mProfileSpinner.setEnabled(true);
            mProfileContentView.setVisibility(View.VISIBLE);
        }
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
        if (parent == mProfileSpinner) {
            Log.d(TAG, "Fired: Profile spinner, pos: " + position);

            String newName = parent.getItemAtPosition(position).toString();
            // populate updated values
            DeviceProfile profile = GlobalData.getInstance().getProfileMap().get(newName);
            populateProfile(profile);
        } else if (parent == mProfileDeviceTypeSpinner) {
            Log.d(TAG, "Fired: device type spinner,pos: " + position);
            mSettingsFragment.setDeviceType(DeviceType.parseInt(mProfileDeviceTypeSpinner.getSelectedItemPosition()));

        } else { // mPhase2Spinner
            Log.d(TAG, "Fired: Other spinner: " + parent.toString());
            // nothing to do, only one item supported now.
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
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

    private void populateProfile(final DeviceProfile profile) {
        Log.e(TAG, "populate starts");
        // turn off the spinner event handler while we updates
        mProfileDeviceTypeSpinner.setOnItemSelectedListener(null);
        mProfileSpinner.setOnItemSelectedListener(null);

        // update by profile
        if (profile != null && (profile.getServerIp() != null) && !TextUtils.isEmpty(profile.getName())) {
            // ui
            setSelection(mProfileSpinner, profile.getName());
            mProfileDeviceTypeSpinner.setSelection(profile.getDeviceType().getIntValue());

            // populate data
            Log.d(TAG, "Populating profile data: " + profile.getName());
            mSettingsFragment.setProfile(profile, true);
        } else {
            mSettingsFragment.setProfile(null, true);
        }
        // enabled the listener again, post it to handler so it does not get enabled too soon.
        mProfileSpinner.post(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "populate ends");
                mProfileDeviceTypeSpinner.setOnItemSelectedListener(ProfileFragment.this);
                mProfileSpinner.setOnItemSelectedListener(ProfileFragment.this);
            }
        });

    }

    /**
     * Read current input on the form as a profile.
     *
     * @return device profile
     */
    private DeviceProfile readCurrentProfile() {
        String profileName = mProfileSpinner.getSelectedItem().toString();

        DeviceProfile settingsProfile = mSettingsFragment.readCurrentProfile();

        // settingsProfile is just sub-contents, take data and update the current map
        DeviceProfile currentProfile = GlobalData.getInstance().getProfileMap().get(profileName);
        settingsProfile.setName(currentProfile.getName());
        settingsProfile.setUuid(currentProfile.getUuid());
        settingsProfile.setDeviceType(DeviceType.parseInt(mProfileDeviceTypeSpinner.getSelectedItemPosition()));
        GlobalData.getInstance().getProfileMap().remove(profileName);
        GlobalData.getInstance().getProfileMap().put(profileName, settingsProfile);

        return settingsProfile;
    }

    private void updateProfileSpinners() {
        // disable listener while we update.
        mProfileSpinner.setOnItemSelectedListener(null);
        String[] array = GlobalData.getInstance().getProfileMap().keySet().toArray(new String[0]);
        Log.d(TAG, "New profile list: " + array.length);
        updateSpinner(mProfileSpinner, array);
        // point to default location
        if (array.length > 0) {
            // populate data and restore listener
            DeviceProfile profile = GlobalData.getInstance().getProfileMap().get(mProfileSpinner.getItemAtPosition(0).toString());
            populateProfile(profile);
        } else { // no profile
            populateProfile(null);
            mProfileContentView.setVisibility(View.INVISIBLE);
            // restore listener
            mProfileSpinner.post(new Runnable() {
                @Override
                public void run() {
                    mProfileSpinner.setOnItemSelectedListener(ProfileFragment.this);
                }
            });
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