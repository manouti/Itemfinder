package com.manouti.itemfinder.item.additem;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.additem.fragment.BaseFragment;
import com.manouti.itemfinder.item.additem.fragment.ItemFragment;
import com.manouti.itemfinder.item.additem.fragment.PlaceFragment;
import com.manouti.itemfinder.item.additem.fragment.ReviewItemPlaceFragment;


public class AddItemFragmentPagerAdapter extends FragmentPagerAdapter {
    private static final int NUM_ITEMS = 3;

    private Context mContext;
    private ItemFragment mItemFragment;
    private PlaceFragment mPlaceFragment;
    private ReviewItemPlaceFragment mReviewItemPlaceFragment;

    public AddItemFragmentPagerAdapter(FragmentManager fragmentManager, Context context) {
        super(fragmentManager);
        this.mContext = context;

        createFragmentObjects();
    }

    private void createFragmentObjects() {
        mItemFragment = new ItemFragment();
        mPlaceFragment = new PlaceFragment();
        mReviewItemPlaceFragment = new ReviewItemPlaceFragment();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch(position) {
            case 0:
                return mContext.getResources().getString(R.string.tab_add_select_item);
            case 1:
                return mContext.getResources().getString(R.string.tab_add_select_place);
            case 2:
                return mContext.getResources().getString(R.string.tab_review_add_item);
            default:
                return null;
        }
    }

    @Override
    public BaseFragment getItem(int position) {
        switch(position) {
            case 0:
                return mItemFragment;
            case 1:
                return mPlaceFragment;
            case 2:
                return mReviewItemPlaceFragment;
            default:
                return mItemFragment;
        }
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

}
