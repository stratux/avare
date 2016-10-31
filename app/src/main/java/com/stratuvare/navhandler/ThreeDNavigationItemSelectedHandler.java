package com.stratuvare.navhandler;

import android.support.v4.app.Fragment;

import com.stratuvare.R;
import com.stratuvare.fragment.LocationFragment;
import com.stratuvare.fragment.ThreeDFragment;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by arabbani on 7/9/16.
 */
public class ThreeDNavigationItemSelectedHandler extends NavigationItemSelectedHandler {

    @Override
    protected Set<String> getTagsToRemove() {
        Set<String> fragmentTags = new HashSet<>();
        fragmentTags.addAll(super.getTagsToRemove());
        fragmentTags.add(LocationFragment.TAG);
        return fragmentTags;
    }

    @Override
    protected String getFragmentTag() {
        return ThreeDFragment.TAG;
    }

    @Override
    protected Fragment getNewFragment() {
        return new ThreeDFragment();
    }

    @Override
    protected int getNavigationMenuGroupId() {
        return R.id.nav_menu_threed_actions_group;
    }

}
