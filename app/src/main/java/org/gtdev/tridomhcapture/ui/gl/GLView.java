package org.gtdev.tridomhcapture.ui.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class GLView extends GLSurfaceView {
    private ObjRenderer renderer;
    ScaleGestureDetector SGD;
    private float mPreviousX;
    private float mPreviousY;

    //  Constructor
    public GLView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        SGD = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setRenderer(ObjRenderer renderer) {
        this.renderer = renderer;
        super.setRenderer(renderer);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            renderer.scale *= detector.getScaleFactor();
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        float x = e.getX();
        float y = e.getY();
        int motionaction = e.getAction() & MotionEvent.ACTION_MASK;
        switch (motionaction) {
            case MotionEvent.ACTION_DOWN:
                // Prevent jumping around.
                mPreviousX = x;
                mPreviousY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (renderer != null) {
                    float deltaX = (x - mPreviousX) / 2f;
                    float deltaY = (y - mPreviousY) / 2f;
                    renderer.mDeltaX += deltaX;
                    renderer.mDeltaY += deltaY;
                }
                mPreviousX = x;
                mPreviousY = y;
                break;
        }
        SGD.onTouchEvent(e);
        return true;
    }

    // Called when reset.
    public void Reset() {
        renderer.scale = 1;
        Matrix.setIdentityM(renderer.mAccumulatedRotation, 0);
    }

}
