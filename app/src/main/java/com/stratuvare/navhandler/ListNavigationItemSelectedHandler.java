package com.stratuvare.navhandler;

import android.support.v4.app.Fragment;

import com.stratuvare.fragment.ChecklistFragment;

/**
 * Created by arabbani on 7/9/16.
 */
public class ListNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected String getFragmentTag() {
        return ChecklistFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new ChecklistFragment();
    }

}
