// References: https://github.com/bminortx/Android-PLY-Reader/blob/master/OpenGLGui/app/src/main/java/com/graphics/openglgui/GLView.java
package org.gtdev.tridomhcapture;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import org.gtdev.tridomhcapture.ui.gl.GLView;
import org.gtdev.tridomhcapture.ui.gl.ObjRenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class PreviewActivity extends AppCompatActivity {

    private static final String TAG = "PreviewActivity";
    GLView mGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        Bundle data = getIntent().getExtras();
        if(data == null) {
            Log.e(TAG, "Bundle data null.");
            return;
        }
        String fileName = data.getString("fileName");
        if(fileName == null) {
            Log.e(TAG, "File name does not exist in bundle data.");
            return;
        }
        String pathName = getExternalFilesDir(null).getAbsolutePath() + File.separator +
                "models" +  File.separator + fileName;
        File plyFile = new File(pathName);
        mGLView = findViewById(R.id.surface_view);
        try {
            mGLView.setRenderer(new ObjRenderer(this, new FileInputStream(plyFile)));
            mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Cannot find file from given path: " + pathName);
            Log.e(TAG, e.toString());
        }
    }

}
