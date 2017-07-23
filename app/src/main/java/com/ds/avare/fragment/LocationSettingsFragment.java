package com.ds.avare.fragment;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.ds.avare.R;

/**
 * Created by roleary on 11/27/2016.
 */

public class LocationSettingsFragment extends PreferenceFragmentCompat {

    public static final String TAG = "LocationPreferencesFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.location_settings, rootKey);
    }

}
