package org.gtdev.tridomhcapture;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.Device;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.Extension;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.GLRsSurfaceView;
import com.intel.realsense.librealsense.StreamFormat;
import com.intel.realsense.librealsense.StreamType;

import org.gtdev.tridomhcapture.rs.RsWrapper;
import org.gtdev.tridomhcapture.rs.Streamer;
import org.gtdev.tridomhcapture.ui.CameraControls;
import org.gtdev.tridomhcapture.ui.FirmwareUpdateProgressDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements RsWrapper.onDeviceChangedListener, CameraControls.OnShutterButtonListener {
    private static final String TAG = "Capture MainActivity";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;

    private boolean mPermissionsGrunted = false;
    private final Handler mHandler = new MainHandler();
    private static final int UPDATE_RECORD_TIME = 5;

    private Context mAppContext;
    private TextView mBackGroundText;
    private GLRsSurfaceView mGLSurfaceView;
    private Streamer mStreamer;
    private Map<Integer, TextView> mLabels;
    private CameraControls mCameraControls;

    private RsWrapper rsWrapper;

    private TextView mRecordingTimeView;
    private FrameLayout mRecordingTimeRect;
    private ImageView mThumbnail;
    private ProgressBar mWaitIndicator;

    private boolean mRecordingStarted = false;
    private long mRecordingStartTime;

    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_RECORD_TIME:
                    updateRecordingTime();
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecordingTimeRect = findViewById(R.id.recording_time_rect);
        mRecordingTimeView = findViewById(R.id.recording_time);
        mCameraControls = findViewById(R.id.camera_controls);
        mThumbnail = findViewById(R.id.preview_thumb);
        mWaitIndicator = findViewById(R.id.wait_progressbar);

        mRecordingTimeRect.setVisibility(View.GONE);
        mCameraControls.setShutterButtonListener(this);
        mThumbnail.setOnClickListener((v) -> {
            if(!mRecordingStarted)
                goPlaybackActivity();
        });

        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);
        mGLSurfaceView = findViewById(R.id.glSurfaceView);
        mGLSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // Android 9 also requires camera permissions
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }

        mPermissionsGrunted = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }
        mPermissionsGrunted = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(size);
        calculateMargins(size);
        if(mPermissionsGrunted)
            init();
        else
            Log.e(TAG, "missing permissions");
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
        rsWrapper = RsWrapper.getInstance();
        rsWrapper.unregisterListener(this);
        if(rsWrapper.getRsContext() != null)
            rsWrapper.getRsContext().close();
    }

    private void calculateMargins(Point size) {
        int l = size.x > size.y ? size.x : size.y;
        int tm = getResources().getDimensionPixelSize(R.dimen.preview_top_margin);
        int bm = getResources().getDimensionPixelSize(R.dimen.preview_bottom_margin);
        int mTopMargin = l / 6 * tm / (tm + bm);
        int mBottomMargin = l / 6 - mTopMargin;
        mCameraControls.setMargins(mTopMargin, mBottomMargin);
    }

    private void init() {
        rsWrapper = RsWrapper.getInstance();
        rsWrapper.init(mAppContext);
        rsWrapper.registerListener(this);

        try(DeviceList dl = rsWrapper.getRsContext().queryDevices()){
            if(dl.getDeviceCount() > 0) {
                showConnectLabel(false);
                showWaitIndicator(false);
                try(Device d = dl.createDevice(0)){
                    if(d == null)
                        return;
                    if(d.is(Extension.UPDATE_DEVICE)){
                        FirmwareUpdateProgressDialog fupd = new FirmwareUpdateProgressDialog();
                        fupd.setCancelable(false);
                        fupd.show(getSupportFragmentManager(), null);
                    } else {
                        if (!rsWrapper.validateFwVersion(this, d))
                            return;
                        start();
                    }
                }

            } else
                mCameraControls.setShutterEnable(false);
        }
    }

    private void showConnectLabel(final boolean state){
        runOnUiThread(() -> mBackGroundText.setVisibility(state ? View.VISIBLE : View.GONE));
    }

    private void showWaitIndicator(final boolean state){
        runOnUiThread(() -> mWaitIndicator.setVisibility(state ? View.VISIBLE : View.GONE));
    }

    private synchronized void start() {
        mStreamer = new Streamer(this,true, new Streamer.Listener() {
            @Override
            public void config(Config config) {
//                config.enableStream(StreamType.COLOR, -1, 640, 480, StreamFormat.RGB8, 15);
//                config.enableStream(StreamType.DEPTH, -1,  1280, 720, StreamFormat.Z16, 6);
            }

            @Override
            public void onFrameset(FrameSet frameSet) {
                mGLSurfaceView.upload(frameSet);
                Map<Integer, Pair<String, Rect>> rects = mGLSurfaceView.getRectangles();
                printLables(rects);
            }
        });
        mCameraControls.setShutterEnable(true);
        try {
            mGLSurfaceView.clear();
            mStreamer.start();
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to start streaming");
            Toast.makeText(this, getString(R.string.failed_start_streaming), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
    }

    private synchronized void stop() {
        clearLables();
        if(mStreamer != null)
            mStreamer.stop();
        if(mGLSurfaceView != null)
            mGLSurfaceView.clear();
    }

    @Override
    public void onDeviceAttach() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(size);
        calculateMargins(size);
        if(mPermissionsGrunted)
            init();
    }

    @Override
    public void onDeviceDetach() {
        showConnectLabel(true);
        showWaitIndicator(true);
        mCameraControls.setShutterEnable(false);
        stop();
    }

    private synchronized Map<Integer, TextView> createLabels(Map<Integer, Pair<String, Rect>> rects){
        if(rects == null)
            return null;
        mLabels = new HashMap<>();

        final RelativeLayout rl = findViewById(R.id.labels_layout);
        for(Map.Entry<Integer, Pair<String, Rect>> e : rects.entrySet()){
            TextView tv = new TextView(getApplicationContext());
            ViewGroup.LayoutParams lp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(lp);
            tv.setTextColor(Color.parseColor("#ffffff"));
            tv.setTextSize(14);
            rl.addView(tv);
            mLabels.put(e.getKey(), tv);
        }
        return mLabels;
    }

    private void printLables(final Map<Integer, Pair<String, Rect>> rects){
        if(rects == null)
            return;
        final Map<Integer, String> lables = new HashMap<>();
        if(mLabels == null)
            mLabels = createLabels(rects);
        for(Map.Entry<Integer, Pair<String, Rect>> e : rects.entrySet()){
            lables.put(e.getKey(), e.getValue().first);
        }

        runOnUiThread(() -> {
            for(Map.Entry<Integer,TextView> e : mLabels.entrySet()){
                Integer uid = e.getKey();
                if(rects.get(uid) == null)
                    continue;
                Rect r = rects.get(uid).second;
                TextView tv = e.getValue();
                tv.setX(r.left);
                tv.setY(r.top);
                tv.setText(lables.get(uid));
            }
        });
    }

    private void clearLables(){
        if(mLabels != null){
            for(Map.Entry<Integer, TextView> label : mLabels.entrySet())
                label.getValue().setVisibility(View.GONE);
            mLabels = null;
        }
    }

    private void goPlaybackActivity() {
        Intent intent = new Intent(this, PlaybackActivity.class);
        startActivity(intent);
    }

    private static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds) {
        long seconds = milliSeconds / 1000; // round down to compute seconds
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);

        StringBuilder timeStringBuilder = new StringBuilder();

        // Hours
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(hours);

            timeStringBuilder.append(':');
        }

        // Minutes
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');

        // Seconds
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);

        // Centi seconds
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - seconds * 1000) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }
        return timeStringBuilder.toString();
    }

    public void setRecordingTime(String text) {
        mRecordingTimeView.setText(text);
    }

    private void updateRecordingTime() {
        if(!mRecordingStarted)
            return;
        long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime;
        String text = millisecondToTimeString(delta, false);
        setRecordingTime(text);
        long targetNextUpdateDelay = 1000;
        long actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);
        mHandler.sendEmptyMessageDelayed(
                UPDATE_RECORD_TIME, actualNextUpdateDelay);
    }

    private String getFilePath(){
//        File rsFolder = new File(getExternalFilesDir(null).getAbsolutePath() +
//                File.separator + getString(R.string.realsense_folder));
//        rsFolder.mkdir();
        File folder = new File(getExternalFilesDir(null).getAbsolutePath() +
                File.separator + "video");
        folder.mkdir();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());
        File file = new File(folder, currentDateAndTime + ".bag");
        return file.getAbsolutePath();
    }

    @Override
    public void onShutterButtonClick() {
        if(!mRecordingStarted){
            stop();
            mStreamer = new Streamer(this,true, new Streamer.Listener() {
                @Override
                public void config(Config config) {
                    config.enableRecordToFile(getFilePath());
                }

                @Override
                public void onFrameset(FrameSet frameSet) {
                    mGLSurfaceView.upload(frameSet);
                }
            });
            try {
                mStreamer.start();
                mRecordingStarted = true;
                mRecordingStartTime = SystemClock.uptimeMillis();
                mHandler.sendEmptyMessage(UPDATE_RECORD_TIME);
                mRecordingTimeRect.setVisibility(View.VISIBLE);
                Log.d(TAG, "streaming started successfully");
            } catch (Exception e) {
                Log.d(TAG, "failed to start streaming");
            }
        } else {
            stop();
            mRecordingStarted = false;
            mRecordingTimeRect.setVisibility(View.GONE);
            start();
        }
    }
}
