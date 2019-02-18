package com.manouti.itemfinder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;
import com.manouti.itemfinder.about.AboutActivity;
import com.manouti.itemfinder.search.SearchableItemActivity;
import com.manouti.itemfinder.user.UserProfileActivity;
import com.manouti.itemfinder.user.rewards.UserRewardsActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.kiip.sdk.Kiip;
import me.kiip.sdk.KiipFragmentCompat;
import me.kiip.sdk.Poptart;


public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    private static final int SIGNUP_MENU_ITEM_ID = 110;
    private static final int LOGIN_MENU_ITEM_ID = 111;
    private static final int LOGOUT_MENU_ITEM_ID = 112;
    private static final int PROFILE_MENU_ITEM_ID = 113;
    private static final int REWARDS_MENU_ITEM_ID = 114;

    private static final String KIIP_TAG = "kiip_fragment_tag";

    /**
     * Default login request code.
     */
    private static final int RC_SIGN_IN = 100;

    private View mRootView;
    private MenuItem mSearchItem;
    private Toolbar mToolbar;
    private MenuItem mRewardsMenuItem;
    private TextView mRewardCountTextView;
    private boolean mShowRewardsMenuItem;
    private int mRewardCount;

    private FirebaseAuth mAuth;
    private Map<String, DatabaseReference> mSyncedDatabaseRefs = new HashMap<>();
    private KiipFragmentCompat mKiipFragment;
    private DatabaseReference mUserRewardsReference;
    private ChildEventListener mUserRewardsChildEventListener;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mRootView = findViewById(android.R.id.content);

        mAuth = FirebaseAuth.getInstance();

        // Create or re-use KiipFragment.
        if (savedInstanceState != null) {
            mKiipFragment = (KiipFragmentCompat) getSupportFragmentManager().findFragmentByTag(KIIP_TAG);
        } else {
            mKiipFragment = new KiipFragmentCompat();
            getSupportFragmentManager().beginTransaction().add(mKiipFragment, KIIP_TAG).commit();
        }
    }

    protected abstract int getLayoutResourceId();

    @Override
    protected void onStart() {
        super.onStart();
        invalidateOptionsMenu();

        FirebaseUser currentUser = getCurrentUser();
        if(currentUser != null) {
            Kiip.getInstance().startSession(new Kiip.Callback() {
                @Override
                public void onFailed(Kiip kiip, Exception exception) {
                    // handle failure
                    FirebaseCrash.report(exception);
                }

                @Override
                public void onFinished(Kiip kiip, Poptart poptart) {
                    onPoptart(poptart);
                }
            });

            if (this.getClass() != UserRewardsActivity.class) {
                String userId = currentUser.getUid();
                mUserRewardsReference = FirebaseDatabase.getInstance().getReference().child("user-rewards").child(userId);
                syncDatabaseReference(mUserRewardsReference);

                mUserRewardsChildEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                        mRewardCount++;
                        if (mRewardCount == 1) {
                            mShowRewardsMenuItem = true;
                        }
                        invalidateOptionsMenu();
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        mRewardCount--;
                        if (mRewardCount == 0) {
                            mShowRewardsMenuItem = false;
                        }
                        invalidateOptionsMenu();
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "userRewardsChildEventListener:onCancelled", databaseError.toException());
                        FirebaseCrash.report(databaseError.toException());
                    }
                };
                mUserRewardsReference.addChildEventListener(mUserRewardsChildEventListener);
            }
        }
    }

    public void syncDatabaseReference(DatabaseReference databaseReference) {
        databaseReference.keepSynced(true);
        mSyncedDatabaseRefs.put(databaseReference.toString(), databaseReference);
    }

    @Override
    protected void onStop() {
        for(Map.Entry<String, DatabaseReference> entry : mSyncedDatabaseRefs.entrySet()) {
            entry.getValue().keepSynced(false);
        }

        if(getCurrentUser() != null) {
            Kiip.getInstance().endSession(new Kiip.Callback() {
                @Override
                public void onFailed(Kiip kiip, Exception exception) {
                    // handle failure
                    FirebaseCrash.report(exception);
                }

                @Override
                public void onFinished(Kiip kiip, Poptart poptart) {
                    onPoptart(poptart);
                }
            });

            if (this.getClass() != UserRewardsActivity.class) {
                mUserRewardsReference.removeEventListener(mUserRewardsChildEventListener);
                mShowRewardsMenuItem = false;
                mRewardCount = 0;
            }
        }

        super.onStop();
    }

    protected void startActivity(Class<?> clazz) {
        Intent intent = new Intent(this, clazz);
        startActivity(intent);
    }

    public void logIn(View view) {
        logIn(RC_SIGN_IN);
    }

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

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_item_finder, menu);

        mSearchItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Called when SearchView is collapsing
                if (mSearchItem.isActionViewExpanded()) {
                    animateSearchToolbar(menu, 1, false, false);
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Called when SearchView is expanding
                animateSearchToolbar(menu, 1, true, true);
                return true;
            }
        });
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) mSearchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(getApplicationContext(), SearchableItemActivity.class)));
        searchView.setQueryRefinementEnabled(true);

        FirebaseUser currentUser = getCurrentUser();
        if(currentUser != null) {
            menu.add(Menu.NONE, PROFILE_MENU_ITEM_ID, 0, R.string.action_profile)
                    .setIcon(R.drawable.ic_account_circle_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(Menu.NONE, LOGOUT_MENU_ITEM_ID, 101, R.string.action_logout)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            if(this.getClass() != UserRewardsActivity.class) {
                mRewardsMenuItem = menu.add(Menu.NONE, REWARDS_MENU_ITEM_ID, 0, R.string.action_rewards);
                mRewardsMenuItem.setIcon(R.drawable.ic_card_giftcard_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                MenuItemCompat.setActionView(mRewardsMenuItem, R.layout.rewards_menu_item_layout);

                RelativeLayout badgeLayout = (RelativeLayout) mRewardsMenuItem.getActionView();
                if(mShowRewardsMenuItem) {
                    mRewardsMenuItem.setVisible(true);
                    badgeLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onOptionsItemSelected(mRewardsMenuItem);
                        }
                    });
                    badgeLayout.findViewById(R.id.reward_image_view).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onOptionsItemSelected(mRewardsMenuItem);
                        }
                    });
                } else {
                    mRewardsMenuItem.setVisible(false);
                }

                if(mRewardCountTextView == null) {
                    mRewardCountTextView = (TextView) badgeLayout.findViewById(R.id.reward_count_textview);
                }
                mRewardCountTextView.setText(Integer.toString(mRewardCount));
            }
        } else {
            MenuItem signUpMenuItem = menu.add(Menu.NONE, SIGNUP_MENU_ITEM_ID, Menu.NONE, R.string.action_signup);
            signUpMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(Menu.NONE, LOGIN_MENU_ITEM_ID, Menu.NONE, R.string.action_login)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent = new Intent(Intent.ACTION_VIEW);

        switch (item.getItemId()) {
            case REWARDS_MENU_ITEM_ID:
                intent.setClassName(this, UserRewardsActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.action_settings:
                intent.setClassName(this, ItemfinderPreferencesActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.action_help_center:
                intent.setClassName(this, HelpActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.action_about:
                intent.setClassName(this, AboutActivity.class.getName());
                startActivity(intent);
                break;
            case SIGNUP_MENU_ITEM_ID:
            case LOGIN_MENU_ITEM_ID:
                logIn(RC_SIGN_IN);
                break;
            case PROFILE_MENU_ITEM_ID:
                intent.setClassName(this, UserProfileActivity.class.getName());
                startActivity(intent);
                break;
            case LOGOUT_MENU_ITEM_ID:
                logOut();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void showLoadingDialog(@StringRes int stringResource) {
        showLoadingDialog(getResources().getString(stringResource));
    }

    public void showLoadingDialog(String message) {
        dismissDialog();
        mProgressDialog = ProgressDialog.show(this, "", message, true);
    }

    public void dismissDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public void showSnackbar(@StringRes int errorMessageRes) {
        showSnackbar(getResources().getString(errorMessageRes));
    }

    public void showSnackbar(String errorMessage) {
        Snackbar.make(mRootView, Html.fromHtml("<font color=\"#ffffff\">" + errorMessage + "</font>"), Snackbar.LENGTH_LONG).show();
    }

    public void showQuickSnackbar(@StringRes int errorMessageRes) {
        showQuickSnackbar(getResources().getString(errorMessageRes));
    }

    public void showQuickSnackbar(String errorMessage) {
        Snackbar.make(mRootView, Html.fromHtml("<font color=\"#ffffff\">" + errorMessage + "</font>"), Snackbar.LENGTH_SHORT).show();
    }

    public void onPoptart(Poptart poptart) {
        mKiipFragment.showPoptart(poptart);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                writeNewUser();
                onLoginSucceeded();
            } else if (resultCode == RESULT_CANCELED) {
                Log.i(TAG, "signIn:cancelled");
                showSnackbar(R.string.log_in_cancelled);
            } else {
                // TODO check why is this being called when we tap back from the sign-in activity
                showSnackbar(R.string.unknown_sign_in_response);
            }
        }
    }

    protected void onLoginSucceeded() {
    }

    protected FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    private void writeNewUser() {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser currentUser = getCurrentUser();
        final DatabaseReference userReference = database.child("users").child(currentUser.getUid());
        syncDatabaseReference(userReference);
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // If no record is found for this user, insert a new record.
                if (!dataSnapshot.exists()) {
                    User user = new User(currentUser.getUid(), null, currentUser.getDisplayName(), null);
                    userReference.setValue(user.toMap());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "newUserCheck:onCancelled", databaseError.toException());
            }
        });
    }

    private void animateSearchToolbar(Menu menu, int numberOfMenuIcon, boolean containsOverflow, boolean expandToolbar) {
        if(expandToolbar) {
            hideMenuItems(menu, mSearchItem);
        } else {
            showMenuItems(menu, mShowRewardsMenuItem ? null : mRewardsMenuItem);
        }
        mToolbar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animateSearchToolbar21(numberOfMenuIcon, containsOverflow, expandToolbar);
        } else {
            animateSearchToolbarPre21(expandToolbar);
        }
    }

    private void animateSearchToolbarPre21(boolean show) {
        if (show) {
            TranslateAnimation translateAnimation = new TranslateAnimation(0.0f, 0.0f, (float) (-mToolbar.getHeight()), 0.0f);
            translateAnimation.setDuration(1000);
            mToolbar.clearAnimation();
            mToolbar.startAnimation(translateAnimation);
        } else {
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            Animation translateAnimation = new TranslateAnimation(0.0f, 0.0f, 0.0f, (float) (-mToolbar.getHeight()));
            AnimationSet animationSet = new AnimationSet(true);
            animationSet.addAnimation(alphaAnimation);
            animationSet.addAnimation(translateAnimation);
            animationSet.setDuration(1000);
            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mToolbar.setBackgroundColor(getThemeColor(BaseActivity.this, R.attr.colorPrimary));
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mToolbar.startAnimation(animationSet);
        }
    }

    @TargetApi(21)
    private void animateSearchToolbar21(int numberOfMenuIcon, boolean containsOverflow, boolean show) {
        if (show) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.quantum_grey_600));
            int width = mToolbar.getWidth() -
                    (containsOverflow ? getResources().getDimensionPixelSize(R.dimen.abc_action_button_min_width_overflow_material) : 0) -
                    ((getResources().getDimensionPixelSize(R.dimen.abc_action_button_min_width_material) * numberOfMenuIcon) / 2);
            Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(mToolbar,
                    isRtl(getResources()) ? mToolbar.getWidth() - width : width, mToolbar.getHeight() / 2, 0.0f, (float) width);
            createCircularReveal.setDuration(250);
            createCircularReveal.start();
        } else {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.quantum_grey_600));
            int width = mToolbar.getWidth() -
                    (containsOverflow ? getResources().getDimensionPixelSize(R.dimen.abc_action_button_min_width_overflow_material) : 0) -
                    ((getResources().getDimensionPixelSize(R.dimen.abc_action_button_min_width_material) * numberOfMenuIcon) / 2);
            Animator createCircularReveal = ViewAnimationUtils.createCircularReveal(mToolbar,
                    isRtl(getResources()) ? mToolbar.getWidth() - width : width, mToolbar.getHeight() / 2, (float) width, 0.0f);
            createCircularReveal.setDuration(250);
            createCircularReveal.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mToolbar.setBackgroundColor(getThemeColor(BaseActivity.this, R.attr.colorPrimary));
                    getWindow().setStatusBarColor(ContextCompat.getColor(BaseActivity.this, R.color.colorPrimaryDark));
                }
            });
            createCircularReveal.start();
            getWindow().setStatusBarColor(ContextCompat.getColor(BaseActivity.this, R.color.colorPrimaryDark));
        }
    }

    @TargetApi(21)
    private boolean isRtl(Resources resources) {
        return resources.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    private void showMenuItems(Menu menu, MenuItem exception) {
        setMenuItemsVisibility(menu, true, exception);
    }

    private void hideMenuItems(Menu menu, MenuItem exception) {
        setMenuItemsVisibility(menu, false, exception);
    }

    private void setMenuItemsVisibility(Menu menu, boolean visible, MenuItem exception) {
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            if (item != exception) {
                item.setVisible(visible);
            }
        }
    }

    private int getThemeColor(Context context, int id) {
        Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(new int[]{id});
        int result = a.getColor(0, 0);
        a.recycle();
        return result;
    }

}
