package com.stratuvare.navhandler;

import android.support.v4.app.Fragment;

import com.stratuvare.fragment.AirportFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class AfdNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return AirportFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new AirportFragment();
    }

}
