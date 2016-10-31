package com.stratuvare.navhandler;

import android.support.v4.app.Fragment;

import com.stratuvare.fragment.PlanFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class PlanNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return PlanFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new PlanFragment();
    }

}
