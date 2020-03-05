package org.gtdev.tridomhcapture.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.DialogFragment;

import org.gtdev.tridomhcapture.R;

public class UploadingDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        LayoutInflater inflater = activity.getLayoutInflater();
        View fragmentView = inflater.inflate(R.layout.file_upload_diag, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(fragmentView);
        AlertDialog rv = builder.create();
        return rv;
    }
}
