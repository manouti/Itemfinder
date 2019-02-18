package com.manouti.itemfinder.item.additem;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.location.places.Place;
import com.manouti.itemfinder.AddPlaceActivity;
import com.manouti.itemfinder.BaseActivity;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.additem.fragment.BaseFragment;
import com.manouti.itemfinder.item.additem.fragment.FragmentLifecycle;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.viewpager.ViewPagerScrollListener;

public class AddItemPlaceActivity extends BaseActivity implements ViewPagerScrollListener {

    private static final int CLEAR_MENU_ITEM_ID = 111;
    private static final int ADD_PLACE_MENU_ITEM_ID = 121;

    private ViewPager mViewPager;
    private AddItemFragmentPagerAdapter mFragmentPagerAdapter;
    private LinearLayout mTabStrip;

    private Item mItem;
    private Place mPlace;

    private ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
        int currentPosition = 0;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int newPosition) {
            FragmentLifecycle fragmentToHide = (FragmentLifecycle) mFragmentPagerAdapter.getItem(currentPosition);
            fragmentToHide.onPausePagerFragment();

            FragmentLifecycle fragmentToShow = (FragmentLifecycle) mFragmentPagerAdapter.getItem(newPosition);
            fragmentToShow.onResumePagerFragment();

            currentPosition = newPosition;
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFragmentPagerAdapter = new AddItemFragmentPagerAdapter(getSupportFragmentManager(), this);
        mViewPager = (ViewPager) findViewById(R.id.main_pager);
        mViewPager.setAdapter(mFragmentPagerAdapter);
        mViewPager.addOnPageChangeListener(mPageChangeListener);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.main_tabs);
        tabLayout.setEnabled(false);
        mTabStrip = (LinearLayout) tabLayout.getChildAt(0);
        mTabStrip.setEnabled(false);
        for(int i = 0; i < mTabStrip.getChildCount(); i++) {
            mTabStrip.getChildAt(i).setClickable(false);
        }
        tabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_add_item_place;
    }

    @Override
    public int getCurrentPage() {
        return mViewPager.getCurrentItem();
    }

    @Override
    public void scrollViewPager(int page) {
        mViewPager.setCurrentItem(page, true);

        for(int i = 0; i <= page; i++) {
            mTabStrip.getChildAt(i).setClickable(true);
        }
    }

    @Override
    public void onBackPressed() {
        int currentItem = mViewPager.getCurrentItem();
        if (currentItem > 0) {
            mViewPager.setCurrentItem(currentItem - 1, true);
        } else {
            super.onBackPressed();
        }
    }

    public void updateItem(Item item) {
        this.mItem = item;
    }

    public void updatePlace(Place place) {
        this.mPlace = place;
    }

    public Item getItem() {
        return mItem;
    }

    public Place getPlace() {
        return mPlace;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, ADD_PLACE_MENU_ITEM_ID, Menu.NONE, R.string.action_request_place)
                .setIcon(R.drawable.ic_add_location_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(Menu.NONE, CLEAR_MENU_ITEM_ID, Menu.NONE, R.string.action_clear_add_item)
                .setIcon(R.drawable.ic_clear_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CLEAR_MENU_ITEM_ID:
                clearAllInput();
                return true;
            case ADD_PLACE_MENU_ITEM_ID:
                Intent addPlaceIntent = new Intent(this, AddPlaceActivity.class);
                if(mPlace != null) {
                    addPlaceIntent.putExtra(Intents.PLACE_LOCATION_TO_ADD, mPlace.getLatLng());
                }
                startActivity(addPlaceIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void clearAllInput() {
        DialogUtils.runAfterConfirm(this, new DialogUtils.OnConfirmOperation() {
            @Override
            public void runOperation() {
                for(int i = 0; i < mFragmentPagerAdapter.getCount(); i++) {
                    BaseFragment fragment = mFragmentPagerAdapter.getItem(i);
                    if(fragment.getView() != null) {
                        fragment.clearInput();
                    }
                }
                for(int i = 0; i < mTabStrip.getChildCount(); i++) {
                    mTabStrip.getChildAt(i).setClickable(false);
                }
                scrollViewPager(0);
            }
        }, R.string.confirm_clear_add_item);
    }

    public void disableNextTabs() {
        for(int i = getCurrentPage(); i < mTabStrip.getChildCount(); i++) {
            View child = mTabStrip.getChildAt(i);
            child.setClickable(false);
        }
    }
}
