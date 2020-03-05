package org.gtdev.tridomhcapture.rs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Pair;

import androidx.appcompat.app.AppCompatActivity;

import com.intel.realsense.librealsense.CameraInfo;
import com.intel.realsense.librealsense.Device;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.ProductLine;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.Sensor;
import com.intel.realsense.librealsense.StreamProfile;
import com.intel.realsense.librealsense.StreamType;

import org.gtdev.tridomhcapture.R;
import org.gtdev.tridomhcapture.ui.FirmwareUpdateDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RsWrapper {
    private static RsWrapper instance;
    private RsWrapper() { }
    public static RsWrapper getInstance() {
        if(instance == null) {
            instance = new RsWrapper();
        }
        return instance;
    }

    private RsContext mRsContext;
    private Context mAppContext;
    private Map<ProductLine,String> mMinimalFirmwares = new HashMap<>();
    private static final String MINIMAL_D400_FW_VERSION = "5.10.0.0";

    public void init(Context AppContext) {
        this.mAppContext = AppContext;
        //RsContext.init must be called once in the application lifetime before any interaction with physical RealSense devices.
        //For multi activities applications use the application context instead of the activity context
        RsContext.init(mAppContext);

        //Register to notifications regarding RealSense devices attach/detach events via the DeviceListener.
        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(mListener);

        mMinimalFirmwares.put(ProductLine.D400, MINIMAL_D400_FW_VERSION);
    }

    public interface onDeviceChangedListener {
        void onDeviceAttach();
        void onDeviceDetach();
    }

    private List<onDeviceChangedListener> mRegistration = new ArrayList<>();

    public void registerListener(onDeviceChangedListener lis) {
        mRegistration.add(lis);
    }

    public void unregisterListener(onDeviceChangedListener lis) {
        mRegistration.remove(lis);
    }

    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {
            for(onDeviceChangedListener o:mRegistration)
                o.onDeviceAttach();
        }

        @Override
        public void onDeviceDetach() {
            for(onDeviceChangedListener o:mRegistration)
                o.onDeviceDetach();
        }
    };

    public RsContext getRsContext() {
        return mRsContext;
    }

    public boolean validateFwVersion(AppCompatActivity activity, Device device) {
        final String currFw = device.getInfo(CameraInfo.FIRMWARE_VERSION).split("\n")[0];
        final ProductLine pl = ProductLine.valueOf(device.getInfo(CameraInfo.PRODUCT_LINE));
        if(mMinimalFirmwares.containsKey(pl)){
            final String minimalFw = mMinimalFirmwares.get(pl);
            if(!compareFwVersion(currFw, minimalFw)){
                FirmwareUpdateDialog fud = new FirmwareUpdateDialog();
                Bundle bundle = new Bundle();
                bundle.putBoolean(activity.getString(R.string.firmware_update_required), true);
                fud.setArguments(bundle);
                fud.show(activity.getSupportFragmentManager(), null);
                return false;
            }
        }

        SharedPreferences sharedPref = activity.getSharedPreferences(activity.getString(R.string.app_settings), Context.MODE_PRIVATE);
        boolean showUpdateMessage = sharedPref.getBoolean(activity.getString(R.string.show_update_firmware), true);
        if (!showUpdateMessage || !device.supportsInfo(CameraInfo.RECOMMENDED_FIRMWARE_VERSION))
            return true;

        final String recommendedFw = device.getInfo(CameraInfo.RECOMMENDED_FIRMWARE_VERSION);
        if(!compareFwVersion(currFw, recommendedFw)){
            FirmwareUpdateDialog fud = new FirmwareUpdateDialog();
            fud.show(activity.getSupportFragmentManager(), null);
            return false;
        }
        return true;
    }

    private boolean compareFwVersion(String currFw, String otherFw){
        String[] sFw = currFw.split("\\.");
        String[] sRecFw = otherFw.split("\\.");
        for (int i = 0; i < sRecFw.length; i++) {
            if (Integer.parseInt(sFw[i]) > Integer.parseInt(sRecFw[i]))
                break;
            if (Integer.parseInt(sFw[i]) < Integer.parseInt(sRecFw[i])) {
                return false;
            }
        }
        return true;
    }

    public static Map<Integer, List<StreamProfile>> createProfilesMap(Device device){
        Map<Integer, List<StreamProfile>> rv = new HashMap<>();
        List<Sensor> sensors = device.querySensors();
        for (Sensor s : sensors){
            List<StreamProfile> profiles = s.getStreamProfiles();
            for (StreamProfile p : profiles){
                Pair<StreamType, Integer> pair = new Pair<>(p.getType(), p.getIndex());
                if(!rv.containsKey(pair.hashCode()))
                    rv.put(pair.hashCode(), new ArrayList<StreamProfile>());
                rv.get(pair.hashCode()).add(p);
            }
        }
        return rv;
    }
}
