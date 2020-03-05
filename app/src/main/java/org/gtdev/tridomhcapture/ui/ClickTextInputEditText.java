package org.gtdev.tridomhcapture.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.textfield.TextInputEditText;

public class ClickTextInputEditText extends TextInputEditText {
    public ClickTextInputEditText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setInputType(0);
        setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                v.setFocusableInTouchMode(false);
            }
        });
    }

    public final void setOnClickListener(OnClickListener onClickListener) {
        super.setOnClickListener(v -> {
            v.setFocusableInTouchMode(true);
            v.requestFocus();
            onClickListener.onClick(v);
        });
    }
}
