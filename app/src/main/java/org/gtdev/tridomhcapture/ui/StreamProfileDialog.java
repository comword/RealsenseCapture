package org.gtdev.tridomhcapture.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.intel.realsense.librealsense.CameraInfo;
import com.intel.realsense.librealsense.Device;
import com.intel.realsense.librealsense.Extension;
import com.intel.realsense.librealsense.StreamProfile;
import com.intel.realsense.librealsense.VideoStreamProfile;

import org.gtdev.tridomhcapture.R;
import org.gtdev.tridomhcapture.SettingsActivity;
import org.gtdev.tridomhcapture.rs.StreamProfileSelector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class StreamProfileDialog extends DialogPreference {
    private StreamProfileSelector profile;
    private Device _device;

    public StreamProfileDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
    }

    public void setProfile(StreamProfileSelector profileList) {
        this.profile = profileList;
    }

    public void set_device(Device _device) {
        this._device = _device;
    }

    public Device get_device() {
        return _device;
    }

    public StreamProfileSelector getProfile() {
        return profile;
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.stream_profile_list_view;
    }

    public static class StreamProfileDialogFragmentCompat extends PreferenceDialogFragmentCompat {
        private final String TAG = "StreamProfileDialogFrag";
        private StreamProfileSelector sps;
        private ClickTextInputEditText cl_format, cl_frameRate, cl_resolution;
        private Switch mEnable;
        String pid;

        List<String> formats = new ArrayList<>();
        List<String> frameRates = new ArrayList<>();
        List<String> resolutions = new ArrayList<>();

        public static StreamProfileDialogFragmentCompat newInstance(String key) {
            Bundle args = new Bundle();
            StreamProfileDialogFragmentCompat fragment = new StreamProfileDialogFragmentCompat();
            args.putString(ARG_KEY, key);
            fragment.setArguments(args);
            return fragment;
        }

        private StreamProfile loadProfile(StreamProfileSelector sps) {
            StreamProfile p = sps.getProfile();
            SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.app_settings), Context.MODE_PRIVATE);
            int index = sharedPref.getInt(SettingsActivity.getIndexdDeviceConfigString(pid, p.getType(), p.getIndex()), 0);
            if(index == -1 || index >= sps.getProfiles().size())
                return p;
            return sps.getProfiles().get(index);
        }

        @Override
        protected View onCreateDialogView(Context context) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View rootView = inflater.inflate(R.layout.stream_profile_list_view, null);

            cl_format = rootView.findViewById(R.id.profile_format);
            cl_frameRate = rootView.findViewById(R.id.profile_frame_rate);
            cl_resolution = rootView.findViewById(R.id.profile_resolution);
            mEnable = rootView.findViewById(R.id.stream_type_switch);

            sps = ((StreamProfileDialog) getPreference()).getProfile();
            pid = ((StreamProfileDialog) getPreference())._device.getInfo(CameraInfo.PRODUCT_ID);
            createSpinners(sps);
            StreamProfile previousProfile = loadProfile(sps);
            AtomicReference<String> format_sel = new AtomicReference<>(previousProfile.getFormat().name());
            AtomicReference<String> frameRate_sel = new AtomicReference<>(String.valueOf(previousProfile.getFrameRate()));
            VideoStreamProfile vsp = previousProfile.as(Extension.VIDEO_PROFILE);
            AtomicReference<String> resolution_sel = new AtomicReference<>(vsp.getWidth() + "x" + vsp.getHeight());

            PickerDialog.OnSaveChangeListener saveHandler = (title, selected) -> {
                Log.d(TAG, "Dialog: " + title + " Selected: " + selected);
                if(title.equals(getString(R.string.hint_format))){
                    sps.updateFormat(selected);
                    sps.updateFrameRate(frameRate_sel.get());
                    sps.updateResolution(resolution_sel.get());
                    cl_format.setText(selected);
                    format_sel.set(selected);
                } else if (title.equals(getString(R.string.hint_frame_rate))) {
                    sps.updateFrameRate(selected);
                    sps.updateFormat(format_sel.get());
                    sps.updateResolution(resolution_sel.get());
                    cl_frameRate.setText(selected);
                    frameRate_sel.set(selected);
                } else if (title.equals(getString(R.string.hint_resolution))) {
                    sps.updateResolution(selected);
                    sps.updateFormat(format_sel.get());
                    sps.updateFrameRate(frameRate_sel.get());
                    cl_resolution.setText(selected);
                    resolution_sel.set(selected);
                }
            };

            mEnable.setChecked(sps.isEnabled());
            mEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sps.updateEnabled(isChecked);
            });

            View.OnClickListener listener = v -> {
                int id = v.getId();
                PickerDialog pickerDialog = null;
                switch (id) {
                    case R.id.profile_format:
                        pickerDialog = PickerDialog.newInstance(getString(R.string.hint_format), "",
                                formats.toArray(new String[0]), format_sel.get(), saveHandler);
                        break;
                    case R.id.profile_frame_rate:
                        pickerDialog = PickerDialog.newInstance(getString(R.string.hint_frame_rate), "",
                                frameRates.toArray(new String[0]), frameRate_sel.get(), saveHandler);
                        break;
                    case R.id.profile_resolution:
                        pickerDialog = PickerDialog.newInstance(getString(R.string.hint_resolution), "",
                                resolutions.toArray(new String[0]), resolution_sel.get(), saveHandler);
                        break;
                }
                if(pickerDialog!=null)
                    pickerDialog.show(getChildFragmentManager(), ((ClickTextInputEditText)v).getHint()+"Dialog");
            };

            cl_format.setOnClickListener(listener);
            cl_frameRate.setOnClickListener(listener);
            cl_resolution.setOnClickListener(listener);
            cl_format.setText(format_sel.get());
            cl_frameRate.setText(frameRate_sel.get());
            cl_resolution.setText(resolution_sel.get());

            return rootView;
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.app_settings), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                StreamProfile p = sps.getProfile();
                editor.putBoolean(SettingsActivity.getEnabledDeviceConfigString(pid, p.getType(), p.getIndex()), sps.isEnabled());
                editor.putInt(SettingsActivity.getIndexdDeviceConfigString(pid, p.getType(), p.getIndex()), sps.getIndex());
                editor.commit();
            }
        }

        private void createSpinners(StreamProfileSelector sps) {
            Set<String> formatsSet = new HashSet<>();
            Set<String> frameRatesSet = new HashSet<>();
            Set<String> resolutionsSet = new HashSet<>();

            for(StreamProfile sp : sps.getProfiles()){
                formatsSet.add(sp.getFormat().name());
                frameRatesSet.add(String.valueOf(sp.getFrameRate()));
                if(!sp.is(Extension.VIDEO_PROFILE))
                    continue;
                VideoStreamProfile vsp = sp.as(Extension.VIDEO_PROFILE);
                resolutionsSet.add(vsp.getWidth() + "x" + vsp.getHeight());
            }

            formats = new ArrayList<>(formatsSet);
            frameRates = new ArrayList<>(frameRatesSet);
            resolutions = new ArrayList<>(resolutionsSet);
        }

    }

    public static class PickerDialog extends DialogFragment {
        private TextView mMsg;
        private OnSaveChangeListener listener;
        public String title;

        public static PickerDialog newInstance(String title, String msg, String[] list, String selected, OnSaveChangeListener listener) {
            
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("msg", msg);
            args.putStringArray("list", list);
            args.putString("selected", selected);
            PickerDialog fragment = new PickerDialog();
            fragment.title = title;
            fragment.listener = listener;
            fragment.setArguments(args);
            return fragment;
        }

        public interface OnSaveChangeListener {
            void save(String title, String selected);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View rootView = inflater.inflate(R.layout.diag_picker, null);
            Bundle data = getArguments();

            mMsg = rootView.findViewById(R.id.dialog_msg);
            mMsg.setText(data.getString("msg"));
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            NumberPicker picker = new NumberPicker(getContext());
            String selected = data.getString("selected");
            String[] list = data.getStringArray("list");
            picker.setDisplayedValues(list);
            picker.setMinValue(0);
            picker.setMaxValue(list.length-1);
            picker.setValue(java.util.Arrays.asList(list).indexOf(selected));
            LinearLayout ll = rootView.findViewById(R.id.dialog_picker);
            ll.addView(picker);
            builder.setTitle(data.getString("title"));
            builder.setView(rootView);
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                dismiss();
            });
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                listener.save(title, list[picker.getValue()]);
                dismiss();
            });
            return builder.create();
        }
    }
}

