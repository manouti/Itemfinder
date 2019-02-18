package com.manouti.itemfinder.home.fragment;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Html;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.manouti.itemfinder.home.MainActivity;


public abstract class BaseHomeFragment extends Fragment {

    protected MainActivity mMainActivity;

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            mMainActivity = (MainActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainActivity");
        }
    }

    protected void showSnackbar(@StringRes int errorMessageRes) {
        showSnackbar(getResources().getString(errorMessageRes));
    }

    protected void showSnackbar(String errorMessage) {
        Snackbar.make(getView(), Html.fromHtml("<font color=\"#ffffff\">" + errorMessage + "</font>"), Snackbar.LENGTH_LONG).show();
    }

    protected FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public abstract void handleRefresh();
}
