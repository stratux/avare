package com.ds.avare.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.ds.avare.R;

/**
 * Created by roleary on 11/27/2016.
 */

public class PlatesSettingsFragment extends PreferenceFragmentCompat {

    public static final String TAG = "PlatesSettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.plates_settings, rootKey);
    }


}
