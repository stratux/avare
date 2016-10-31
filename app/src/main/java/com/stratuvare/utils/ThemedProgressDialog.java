package com.stratuvare.utils;

import android.app.ProgressDialog;
import android.content.Context;

import com.stratuvare.R;

public class ThemedProgressDialog extends ProgressDialog {

    public ThemedProgressDialog(Context context) {
        super(context, R.style.Theme_Dialog);
    }

}
