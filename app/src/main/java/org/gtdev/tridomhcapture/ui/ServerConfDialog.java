package org.gtdev.tridomhcapture.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.gtdev.tridomhcapture.R;

public class ServerConfDialog extends DialogPreference {

    String server_addr = "";

    public ServerConfDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public String getServerAddr() {
        return server_addr;
    }

    public void setServerAddr(String s) {
        server_addr = s;
        persistString(s);
        callChangeListener(s);
        //String[] sp = s.split(":");
        setSummary(s);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, @Nullable Object defaultValue) {
        setServerAddr(restorePersistedValue ? getPersistedString(server_addr) : (String) defaultValue);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.dialog_server_addr;
    }

    public static class ServerConfDialogFragmentCompat extends PreferenceDialogFragmentCompat {

        EditText serverAddr;
        EditText serverPort;

        public static ServerConfDialogFragmentCompat newInstance(String key) {
            
            Bundle args = new Bundle();
            ServerConfDialogFragmentCompat fragment = new ServerConfDialogFragmentCompat();
            args.putString(ARG_KEY, key);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);
            serverAddr = view.findViewById(R.id.server_addr);
            serverPort = view.findViewById(R.id.server_port);
            if(serverPort == null || serverAddr ==null)
                throw new IllegalStateException("ServerConfDialog with wrong layout.");
            DialogPreference preference = getPreference();
            String saved = null;
            if(preference instanceof ServerConfDialog)
                saved = ((ServerConfDialog) preference).getServerAddr();
            if(saved != null) {
                String[] sp = saved.split(":");
                if(sp.length==2){
                    serverAddr.setText(sp[0]);
                    serverPort.setText(sp[1]);
                }
            }

        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                DialogPreference preference = getPreference();
                String res = serverAddr.getText()+":"+serverPort.getText();
                if(preference instanceof ServerConfDialog) {
                    if(preference.callChangeListener(res))
                        ((ServerConfDialog) preference).setServerAddr(res);
                }
            }
        }
    }
}
