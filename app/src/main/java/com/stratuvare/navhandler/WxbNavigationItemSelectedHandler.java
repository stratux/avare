package com.stratuvare.navhandler;

import android.support.v4.app.Fragment;

import com.stratuvare.fragment.WeatherFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class WxbNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return WeatherFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new WeatherFragment();
    }

}
