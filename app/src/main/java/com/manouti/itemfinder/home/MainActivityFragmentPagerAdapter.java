package com.manouti.itemfinder.home;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.manouti.itemfinder.R;
import com.manouti.itemfinder.home.fragment.BaseHomeFragment;
import com.manouti.itemfinder.home.fragment.RecommendedItemsFragment;
import com.manouti.itemfinder.home.fragment.NearbyItemsFragment;
import com.manouti.itemfinder.home.fragment.TopRatedItemsFragment;


public class MainActivityFragmentPagerAdapter extends FragmentStatePagerAdapter {

    private static final int NUM_ITEMS = 3;

    private Context mContext;
    private RecommendedItemsFragment mRecommendedItemsFragment;
    private NearbyItemsFragment mNearbyItemsFragment;
    private TopRatedItemsFragment mTopRatedItemsFragment;

    public MainActivityFragmentPagerAdapter(FragmentManager fragmentManager, Context context) {
        super(fragmentManager);
        this.mContext = context;

        createFragmentObjects();
    }

    private void createFragmentObjects() {
        mRecommendedItemsFragment = new RecommendedItemsFragment();
        mNearbyItemsFragment = new NearbyItemsFragment();
        mTopRatedItemsFragment = new TopRatedItemsFragment();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch(position) {
            case 0:
                return mContext.getResources().getString(R.string.tab_home);
            case 1:
                return mContext.getResources().getString(R.string.tab_nearby_items);
            case 2:
                return mContext.getResources().getString(R.string.tab_top_rated_items);
            default:
                return null;
        }
    }

    @Override
    public BaseHomeFragment getItem(int position) {
        switch(position) {
            case 0:
                return mRecommendedItemsFragment;
            case 1:
                return mNearbyItemsFragment;
            case 2:
                return mTopRatedItemsFragment;
            default:
                return mTopRatedItemsFragment;
        }
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    @NonNull
    public BaseHomeFragment[] getLocationAwareFragments() {
        return new BaseHomeFragment[] { mNearbyItemsFragment };
    }
}
