package com.stratuvare.navhandler;

import android.support.v4.app.Fragment;

import com.stratuvare.fragment.SatelliteFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class ToolsNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return SatelliteFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new SatelliteFragment();
    }

}
