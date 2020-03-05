package org.gtdev.tridomhcapture.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.gtdev.tridomhcapture.R;
import org.gtdev.tridomhcapture.SettingsActivity;

public class CameraControls extends FrameLayout {
    private static final String TAG = "CAM_Controls";

    private ImageView mVideoShutter;
    private View mMenu;

    private static int mTopMargin = 0;
    private static int mBottomMargin = 0;

    private Paint mPaint;

    OnShutterButtonListener receiver = null;

    public interface OnShutterButtonListener {
        void onShutterButtonClick();
    }

    public void setShutterButtonListener(OnShutterButtonListener listener) {
        receiver = listener;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mVideoShutter = findViewById(R.id.video_button);
        mMenu = findViewById(R.id.menu);
        mMenu.setEnabled(true);

        mVideoShutter.setOnClickListener((v)-> {
            if(receiver != null)
                receiver.onShutterButtonClick();
        });
        mMenu.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            getContext().startActivity(intent);
        });
    }

    public CameraControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(getResources().getColor(R.color.camera_control_bg_transparent));
        setWillNotDraw(false);
        setClipChildren(false);
        setMeasureAllChildren(true);
    }

    public CameraControls(Context context) {
        this(context, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mTopMargin != 0) {
            int w = canvas.getWidth(), h = canvas.getHeight();
            canvas.drawRect(0, 0, w, mTopMargin, mPaint);
            canvas.drawRect(0, h - mBottomMargin, w, h, mPaint);
        }
    }

    public void setMargins(int top, int bottom) {
        mTopMargin = top;
        mBottomMargin = bottom;
    }

    public void setShutterEnable(boolean enable) {
        mVideoShutter.setEnabled(enable);
    }
}
