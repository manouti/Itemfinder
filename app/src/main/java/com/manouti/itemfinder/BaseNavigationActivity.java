package com.manouti.itemfinder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.manouti.itemfinder.item.history.HistoryActivity;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;
import com.manouti.itemfinder.home.MainActivity;
import com.manouti.itemfinder.user.UserProfileActivity;
import com.manouti.itemfinder.user.items.FavoriteItemsActivity;
import com.manouti.itemfinder.user.items.ReviewAddedItemsActivity;
import com.manouti.itemfinder.user.items.UserAddedItemsActivity;
import com.manouti.itemfinder.user.locations.SavedLocationsActivity;

import java.util.ArrayList;


public abstract class BaseNavigationActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private TextView mNavHeaderLoginTextView;
    private TextView mNavHeaderEmailTextView;
    private MenuItem mReviewItemsNavItem;
    private MenuItem mFavoriteItemsNavItem;

    private MenuItem mSavedLocationsNavItem;
    private MenuItem mAddedItemsNavItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        View headerView = mNavigationView.getHeaderView(0);
        mNavHeaderLoginTextView = (TextView) headerView.findViewById(R.id.loginTextView);
        mNavHeaderEmailTextView = (TextView) headerView.findViewById(R.id.emailTextView);

        mReviewItemsNavItem = mNavigationView.getMenu().findItem(R.id.nav_rate_review_added_items);
        mFavoriteItemsNavItem = mNavigationView.getMenu().findItem(R.id.nav_favorite_items);
        mSavedLocationsNavItem = mNavigationView.getMenu().findItem(R.id.nav_saved_locations);
        mAddedItemsNavItem = mNavigationView.getMenu().findItem(R.id.nav_added_items);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateNavigationDrawer();
    }

    protected abstract int getCurrentNavMenuItemId();

    protected void logIn(int requestCode) {
        ArrayList<String> selectedProviders = new ArrayList<>();
        selectedProviders.add(AuthUI.EMAIL_PROVIDER);
        selectedProviders.add(AuthUI.GOOGLE_PROVIDER);

        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setProviders(selectedProviders.toArray(new String[selectedProviders.size()]))
                        .setTheme(R.style.AppTheme_NoActionBar)
                        .build(),
                requestCode);
    }

    protected void logOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            recreate();
                        } else {
                            showSnackbar(R.string.log_out_failed);
                        }
                    }
                });
    }

    public void showMyProfile(View view) {
        startActivity(UserProfileActivity.class);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id != getCurrentNavMenuItemId()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            boolean finishCurrent = this.getClass() != MainActivity.class;

            switch (id) {
                case R.id.nav_home:
                    intent.setClassName(this, MainActivity.class.getName());
                    break;
                case R.id.nav_history:
                    intent.setClassName(this, HistoryActivity.class.getName());
                    intent.putExtra(Intents.OPENING_HISTORY_FROM_NAV_DRAWER, true);
                    break;
                case R.id.nav_added_items:
                    intent.setClassName(this, UserAddedItemsActivity.class.getName());
                    break;
                case R.id.nav_rate_review_added_items:
                    intent.setClassName(this, ReviewAddedItemsActivity.class.getName());
                    break;
                case R.id.nav_favorite_items:
                    intent.setClassName(this, FavoriteItemsActivity.class.getName());
                    break;
                case R.id.nav_saved_locations:
                    intent.setClassName(this, SavedLocationsActivity.class.getName());
                    break;
                case R.id.nav_share:
                    // intent.setClassName(this, ShareActivity.class.getName());
                    break;
                case R.id.nav_settings:
                    intent.setClassName(this, ItemfinderPreferencesActivity.class.getName());
                    finishCurrent = false;
                    break;
                case R.id.nav_help_center:
                    intent.setClassName(this, HelpActivity.class.getName());
                    finishCurrent = false;
                    break;
                default:
                    return true;
            }

            startActivity(intent);

            mDrawerLayout.closeDrawer(GravityCompat.START);
            if(finishCurrent) {
                finish();
            }
            return true;
        } else {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return false;
        }
    }

    private void updateNavigationDrawer() {
        FirebaseUser currentUser = getCurrentUser();

        mNavHeaderLoginTextView.setVisibility(currentUser != null ? View.GONE : View.VISIBLE);
        mNavHeaderEmailTextView.setVisibility(currentUser != null ? View.VISIBLE : View.GONE);

        if(currentUser != null) {
            mNavHeaderEmailTextView.setText(currentUser.getEmail());
        }

        mReviewItemsNavItem.setVisible(currentUser != null);
        mFavoriteItemsNavItem.setVisible(currentUser != null);
        mSavedLocationsNavItem.setVisible(currentUser != null);
        mAddedItemsNavItem.setVisible(currentUser != null);

        int currentNavMenuItemId = getCurrentNavMenuItemId();
        if(currentNavMenuItemId > 0) {
            mNavigationView.setCheckedItem(currentNavMenuItemId);
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

}
