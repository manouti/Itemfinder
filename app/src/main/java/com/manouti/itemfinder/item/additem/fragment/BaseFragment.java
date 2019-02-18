package com.manouti.itemfinder.item.additem.fragment;


import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.widget.ImageButton;

import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.additem.AddItemPlaceActivity;


public abstract class BaseFragment extends Fragment {
    protected AddItemPlaceActivity mAddItemPlaceActivity;

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mAddItemPlaceActivity = (AddItemPlaceActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AddItemPlaceActivity");
        }
    }

    protected void scrollNext() {
        mAddItemPlaceActivity.scrollViewPager(mAddItemPlaceActivity.getCurrentPage() + 1);
    }

    protected void scrollPrevious() {
        mAddItemPlaceActivity.scrollViewPager(mAddItemPlaceActivity.getCurrentPage() - 1);
    }

    protected void scrollFirst() {
        mAddItemPlaceActivity.scrollViewPager(0);
    }

    protected void setNavigationButtonEnabled(boolean enabled, ImageButton item, int drawableResId) {
        item.setEnabled(enabled);
        Drawable originalDrawable = ContextCompat.getDrawable(getContext(), drawableResId);
        Drawable drawable = enabled ? originalDrawable : convertDrawableToGrayScale(originalDrawable);
        item.setImageDrawable(drawable);
    }

    private Drawable convertDrawableToGrayScale(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Drawable res = drawable.mutate();
        res.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorDisabledButton), PorterDuff.Mode.SRC_IN);
        return res;
    }

    public abstract void clearInput();
}
