package org.gtdev.tridomhcapture;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import com.intel.realsense.librealsense.CameraInfo;
import com.intel.realsense.librealsense.Device;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamProfile;
import com.intel.realsense.librealsense.StreamType;

import org.gtdev.tridomhcapture.rs.RsWrapper;
import org.gtdev.tridomhcapture.rs.StreamProfileSelector;
import org.gtdev.tridomhcapture.ui.FirmwareUpdateDialog;
import org.gtdev.tridomhcapture.ui.ServerConfDialog;
import org.gtdev.tridomhcapture.ui.StreamProfileDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "librs camera settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_settings_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static String getDeviceConfig(String pid, StreamType streamType, int streamIndex){
        return pid + "_" + streamType.name() + "_" + streamIndex;
    }

    public static String getEnabledDeviceConfigString(String pid, StreamType streamType, int streamIndex){
        return getDeviceConfig(pid, streamType, streamIndex) + "_enabled";
    }

    public static String getIndexdDeviceConfigString(String pid, StreamType streamType, int streamIndex){
        return getDeviceConfig(pid, streamType, streamIndex) + "_index";
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private Device _device;
        private StreamProfileSelector[] profilesList;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            int tries = 1;
            RsContext ctx = new RsContext();
            for(int i = 0; i < tries; i++) {
                try (DeviceList devices = ctx.queryDevices()) {
                    if (devices.getDeviceCount() == 0) {
                        Thread.sleep(500);
                        continue;
                    }
                    _device = devices.createDevice(0);
                    profilesList = createSettingList(_device);
                    super.onCreate(savedInstanceState);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "failed to load settings, error: " + e.getMessage());
                }
            }
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (_device != null)
                _device.close();
        }

        private StreamProfileSelector[] createSettingList(final Device device){
            Map<Integer, List<StreamProfile>> profilesMap = RsWrapper.createProfilesMap(device);

            SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.app_settings), Context.MODE_PRIVATE);
            if(!device.supportsInfo(CameraInfo.PRODUCT_ID))
                throw new RuntimeException("try to config unknown device");
            String pid = device.getInfo(CameraInfo.PRODUCT_ID);
            List<StreamProfileSelector> lines = new ArrayList<>();
            for(Map.Entry e : profilesMap.entrySet()){
                List<StreamProfile> list = (List<StreamProfile>) e.getValue();
                StreamProfile p = list.get(0);
                boolean enabled = sharedPref.getBoolean(getEnabledDeviceConfigString(pid, p.getType(), p.getIndex()), false);
                int index = sharedPref.getInt(getIndexdDeviceConfigString(pid, p.getType(), p.getIndex()), 0);
                lines.add(new StreamProfileSelector(enabled, index, list));
            }

            Collections.sort(lines);

            return lines.toArray(new StreamProfileSelector[lines.size()]);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference app_version = findPreference("app_version");
            Preference lib_version = findPreference("librealsense_version");

            app_version.setSummary(BuildConfig.VERSION_NAME);
            RsWrapper rsWrapper = RsWrapper.getInstance();
            lib_version.setSummary(rsWrapper.getRsContext().getVersion());

            PreferenceCategory cameraCategory = findPreference("camera_category");
            Preference connect_notify = findPreference("connect_notify");

//            { //test
//                StreamProfileDialog streamProfileDialog = new StreamProfileDialog(getContext(), null);
//                streamProfileDialog.setKey("test");
//                streamProfileDialog.setTitle("Test");
//                cameraCategory.addPreference(streamProfileDialog);
//            }

            Preference firmwareUpdate = new Preference(getContext(), null);
            firmwareUpdate.setTitle(getString(R.string.preferences_firmware_update));
            firmwareUpdate.setOnPreferenceClickListener(preference -> {
                FirmwareUpdateDialog fud = new FirmwareUpdateDialog();
                Bundle bundle = new Bundle();
                bundle.putBoolean(getString(R.string.firmware_update_request), true);
                fud.setArguments(bundle);
                fud.show(getChildFragmentManager(), "fw_update_dialog");
                return true;
            });

            if(profilesList != null) {
                connect_notify.setVisible(false);
                cameraCategory.addPreference(firmwareUpdate);
                for (StreamProfileSelector sps : profilesList) {
                    Log.d(TAG, "Adding camera profile: " + sps.getName());
                    StreamProfileDialog streamProfileDialog = new StreamProfileDialog(getContext(), null);
                    streamProfileDialog.setKey("profile_" + sps.getName());
                    streamProfileDialog.setTitle(sps.getName());
                    streamProfileDialog.setProfile(sps);
                    streamProfileDialog.set_device(_device);
                    //streamProfileDialog.setSummary(sps.isEnabled() ? getString(R.string.pref_enabled) : getString(R.string.pref_disabled));
                    cameraCategory.addPreference(streamProfileDialog);
                }
            }

        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            DialogFragment dialogFragment = null;
            if (preference instanceof ServerConfDialog)
                dialogFragment = ServerConfDialog.ServerConfDialogFragmentCompat.newInstance(preference.getKey());
            if (preference instanceof StreamProfileDialog)
                dialogFragment = StreamProfileDialog.StreamProfileDialogFragmentCompat.newInstance(preference.getKey());

            if (dialogFragment != null) {
                // The dialog was created (it was one of our custom Preferences), show the dialog for it
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getFragmentManager(), "android.support.v7.preference" +
                        ".PreferenceFragment.DIALOG");
            } else {
                // Dialog creation could not be handled here. Try with the super method.
                super.onDisplayPreferenceDialog(preference);
            }
        }
    }
}