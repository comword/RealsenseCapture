package org.gtdev.tridomhcapture.viewmodel;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

import org.gtdev.tridomhcapture.PlaybackActivity;
import org.gtdev.tridomhcapture.R;
import org.gtdev.tridomhcapture.ui.FileListFragment;
import org.gtdev.tridomhcapture.ui.PlaybackFragment;
import org.gtdev.tridomhcapture.PreviewActivity;
import org.gtdev.tridomhcapture.ui.UploadingDialog;

public class FileListItemViewModel extends ViewModel {

    PlaybackListViewModel.RSFile data;
    private FileListFragment parentFrag;
    public Color backColor;
    public boolean showSelection, show3DView = true;

    FileListItemViewModel(PlaybackListViewModel.RSFile file, FileListFragment frag) {
        data = file;
        parentFrag = frag;
    }

    public String getFileName() {
        return data.title;
    }

    public Drawable getThumb() {
        return parentFrag.getResources().getDrawable(R.drawable.ic_3d_filelist_placeholder);
    }

    public void onClick() {
        PlaybackFragment pf = new PlaybackFragment();
        Bundle b = new Bundle();
        b.putString("file", data.file.getPath());
        pf.setArguments(b);
        FragmentTransaction ft = parentFrag.getFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.anim.slide_in_left, 0,0, android.R.anim.slide_out_right);
        ft.replace(R.id.container, pf, "PlaybackFragment");
        ft.addToBackStack("PlaybackFragment").commit();
    }

    public void onSend() {
        AlertDialog.Builder diag = PlaybackActivity.createDialog(parentFrag.getContext(), parentFrag.getString(R.string.dialog_confirm),
                parentFrag.getString(R.string.dialog_send_msg));
        diag.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
            UploadingDialog updDiag = new UploadingDialog();
            updDiag.setCancelable(false);
            updDiag.show(parentFrag.getChildFragmentManager(), null);
            dialog.dismiss();
        });
        diag.create().show();
    }

    public void onDelete() {
        AlertDialog.Builder diag = PlaybackActivity.createDialog(parentFrag.getContext(), parentFrag.getString(R.string.dialog_confirm),
                parentFrag.getString(R.string.dialog_delete_msg));
        diag.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
            data.file.delete();
            PlaybackListViewModel playbackListViewModel = ViewModelProviders.of(parentFrag.getActivity()).get(PlaybackListViewModel.class);
            playbackListViewModel.getData().remove(data);
            playbackListViewModel.getAdapter().updateData(playbackListViewModel.getData());
            dialog.dismiss();
        });
        diag.create().show();
    }

    public void onView3D() {
        Intent i = new Intent(parentFrag.getContext(), PreviewActivity.class);
        i.putExtra("title", data.title);
        i.putExtra("fileName", "integrated.ply");
        parentFrag.getActivity().startActivity(i);
    }

}