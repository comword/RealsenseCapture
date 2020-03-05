package org.gtdev.tridomhcapture.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.GLRsSurfaceView;

import org.gtdev.tridomhcapture.PlaybackActivity;
import org.gtdev.tridomhcapture.R;
import org.gtdev.tridomhcapture.rs.Streamer;
import org.gtdev.tridomhcapture.viewmodel.PlaybackListViewModel;

import java.io.File;

public class PlaybackFragment extends Fragment {
    private final String TAG = "PlaybackFragment";

    private GLRsSurfaceView mGLSurfaceView;
    private ImageView mMask;
    private String mFilePath;

    private Streamer mStreamer;
    private int regularSystemUiVisibility;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.playback_frag, container, false);
        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        AppCompatActivity aca = ((AppCompatActivity)getActivity());
        aca.setSupportActionBar(toolbar);
        aca.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        aca.setTitle(R.string.title_playback);
        setHasOptionsMenu(true);

        mGLSurfaceView = rootView.findViewById(R.id.glSurfaceView);
        mMask = rootView.findViewById(R.id.mask);
        Bundle bundle = getArguments();
        mFilePath = bundle.getString("file");

        return rootView;
    }

    void setFullscreen(boolean full) {
        if(full) {
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getActivity().getWindow().getDecorView().setSystemUiVisibility(regularSystemUiVisibility);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        regularSystemUiVisibility = getActivity().getWindow().getDecorView().getSystemUiVisibility();
        setFullscreen(true);
        if(mFilePath == null)
            return;

        mStreamer = new Streamer(getContext(),false, new Streamer.Listener() {
            @Override
            public void config(Config config) {
                config.enableAllStreams();
                config.enableDeviceFromFile(mFilePath);
            }

            @Override
            public void onFrameset(FrameSet frameSet) {
                mGLSurfaceView.upload(frameSet);
            }
        });
        try {
            mMask.setVisibility(View.GONE);
            mGLSurfaceView.clear();
            mStreamer.start();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMask.setVisibility(View.VISIBLE);

        if(mStreamer != null)
            mStreamer.stop();
        if(mGLSurfaceView != null)
            mGLSurfaceView.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setFullscreen(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:
                getFragmentManager().popBackStack();
                return true;
            case R.id.action_delete:
                AlertDialog.Builder diag = PlaybackActivity.createDialog(getContext(), getString(R.string.dialog_confirm),
                        getString(R.string.dialog_delete_msg));
                diag.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    PlaybackListViewModel playbackListViewModel = ViewModelProviders.of(getActivity()).get(PlaybackListViewModel.class);
                    PlaybackListViewModel.RSFile rsFile = playbackListViewModel.getRSFileByPath(mFilePath);
                    rsFile.getFile().delete();
                    playbackListViewModel.getData().remove(rsFile);
                    playbackListViewModel.getAdapter().updateData(playbackListViewModel.getData());
                    getFragmentManager().popBackStack();
                    dialog.dismiss();
                });
                diag.create().show();
                return true;

            case R.id.action_upload:

                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.playback_frag_menu, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

}
