package org.gtdev.tridomhcapture.ui.gl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ObjRenderer implements GLSurfaceView.Renderer {
    private final static String TAG = "ObjRender";
    private int mWindowHeight = 0;
    private int mWindowWidth = 0;
    private Context mContext;
    private Mesh mesh;
    // Intrinsic Matrices
    private final float[] mModelMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    // Projection matrix is set in onSurfaceChanged()
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];
    // Rotations for our touch movements
    public final float[] mAccumulatedRotation = new float[16];
    private final float[] mCurrentRotation = new float[16];
    public volatile float mDeltaX;
    public volatile float mDeltaY;
    public volatile float scale = 1;
    private InputStream plyInput;

    public ObjRenderer(Context context, InputStream input) {
        mContext = context;
        plyInput = input;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Initialize the accumulated rotation matrix
        Matrix.setIdentityM(mAccumulatedRotation, 0);
        try {
            mesh = new Mesh(plyInput);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mesh.createProgram();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.mWindowWidth = width;
        this.mWindowHeight = height;
        float ratio = (float) width / height;
        GLES20.glViewport(0, 0, this.mWindowWidth, this.mWindowHeight);
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 1, 100);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        float[] mTemporaryMatrix = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        // Model, View, and Projection
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.scaleM(mModelMatrix, 0, scale, scale, scale);
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        // Set a matrix that contains the current rotation.
        // Code below adapted from http://www.learnopengles.com/rotating-an-object-with-touch-events/
        Matrix.setIdentityM(mCurrentRotation, 0);
        Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(mCurrentRotation, 0, mDeltaY, -1.0f, 0.0f, 0.0f);
        mDeltaX = 0.0f;
        mDeltaY = 0.0f;
        // Multiply the current rotation by the accumulated rotation,
        // and then set the accumulated rotation to the result.
        Matrix.multiplyMM(mTemporaryMatrix, 0,
                mCurrentRotation, 0,
                mAccumulatedRotation, 0);
        System.arraycopy(mTemporaryMatrix, 0,
                mAccumulatedRotation, 0, 16);
        // Rotate the cube taking the overall rotation into account.
        Matrix.multiplyMM(mTemporaryMatrix, 0,
                mModelMatrix, 0,
                mAccumulatedRotation, 0);
        System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16);
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mTemporaryMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mTemporaryMatrix, 0);
        // Draw shape
        mesh.draw(mMVPMatrix);
    }

}
