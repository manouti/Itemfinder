package com.manouti.itemfinder.user.rewards;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.manouti.itemfinder.BaseActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.model.user.reward.UserReward;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;
import com.manouti.itemfinder.util.ui.SwipeUpOnlyRefreshLayout;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

import me.kiip.sdk.Kiip;
import me.kiip.sdk.Poptart;

public class UserRewardsActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener,
        RecyclerViewAdapterEventListener<RewardViewHolder, UserReward> {

    private static final String TAG = UserRewardsActivity.class.getSimpleName();
    private static final String KIIP_TAG = TAG + ":Kiip";

    private static final int REFRESH_MENU_ITEM_ID = 116;

    private SwipeUpOnlyRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRewardsRecyclerView;
    private RewardsRecyclerViewAdapter mAdapter;
    private DatabaseReference mUserRewardsReference;
    private ProgressBar mProgressBar;
    private ViewStub mNoRewardsViewStub;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) {
            Log.w(TAG, "Current user is unexpectedly null");
            finish();
            return;
        }
        final String userId = currentUser.getUid();
        mUserRewardsReference = FirebaseDatabase.getInstance().getReference()
                .child("user-rewards").child(userId);
        syncDatabaseReference(mUserRewardsReference);

        mSwipeRefreshLayout = (SwipeUpOnlyRefreshLayout) findViewById(R.id.swiperefresh_user_rewards);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mRewardsRecyclerView = (RecyclerView) findViewById(R.id.recycler_user_rewards);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRewardsRecyclerView.setLayoutManager(layoutManager);
        mRewardsRecyclerView.setNestedScrollingEnabled(false);
        mRewardsRecyclerView.setItemAnimator(new SlideInLeftAnimator());

        mProgressBar = (ProgressBar) findViewById(R.id.user_rewards_progress_bar);
        mNoRewardsViewStub = (ViewStub) findViewById(R.id.no_rewards_stub);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_user_rewards;
    }

    @Override
    protected void onStart() {
        super.onStart();
        queryRewards();
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.cleanUpListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, REFRESH_MENU_ITEM_ID, 0, R.string.action_refresh)
                .setIcon(R.drawable.ic_refresh_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
            case REFRESH_MENU_ITEM_ID:
                mSwipeRefreshLayout.setRefreshing(true);
                queryRewards();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void queryRewards() {
        // Listen for saved locations
        if(mAdapter == null) {
            mAdapter = new RewardsRecyclerViewAdapter(mUserRewardsReference, this, this);
            mRewardsRecyclerView.setAdapter(mAdapter);
        }
        mAdapter.queryRewards();
    }

    @Override
    public void onRefresh() {
        queryRewards();
    }

    @Override
    public void onItemCountReady(long itemCount) {
        mSwipeRefreshLayout.setRefreshing(false);
        mProgressBar.setVisibility(View.GONE);
        if(itemCount == 0) {
            if (mNoRewardsViewStub.getParent() != null) {
                mNoRewardsViewStub.inflate();
            } else {
                mNoRewardsViewStub.setVisibility(View.VISIBLE);
            }
        } else {
            mNoRewardsViewStub.setVisibility(View.GONE);
        }
    }

    @Override
    public void onError(Exception ex) {
        if(ex != null) {
            FirebaseCrash.report(ex);
        }
        showSnackbar(R.string.unexpected_error_try_later);
    }

    @Override
    public RewardViewHolder onCreateAddedItemViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.reward, parent, false);

        RewardViewHolder rewardViewHolder = new RewardViewHolder(this, mAdapter, view) {

            @Override
            public void onClick(View v) {
                super.onClick(v);

                mAdapter.removeItem(this.getAdapterPosition(), new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError == null) {
                            sendReward(moment);
                        } else {
                            FirebaseCrash.report(databaseError.toException());
                            showSnackbar(R.string.unexpected_error_try_later);
                        }
                    }
                });
            }

        };
        rewardViewHolder.itemSummaryTextView = (TextView) view.findViewById(R.id.item_summary_text_view);
        rewardViewHolder.placeTextView = (TextView) view.findViewById(R.id.place_text_view);
        rewardViewHolder.newRepTextView = (TextView) view.findViewById(R.id.new_rep_text_view);

        return rewardViewHolder;
    }

    @Override
    public void onBindItem(final RewardViewHolder viewHolder, UserReward userReward) {
        viewHolder.itemSummaryTextView.setText(userReward.getItemSummary());
        viewHolder.placeTextView.setText(userReward.getPlaceName());
        viewHolder.newRepTextView.setText(Long.toString(userReward.getNewRep()));
        viewHolder.moment = userReward.getMoment();
    }

    private void sendReward(String moment) {
        if(moment == null) {
            showSnackbar(R.string.unexpected_error_try_later);
            FirebaseCrash.log("sendReward:moment was null");
            return;
        }
        Kiip.getInstance().saveMoment(moment, new Kiip.Callback() {

            @Override
            public void onFinished(Kiip kiip, Poptart reward) {
                if (reward == null) {
                    FirebaseCrash.log("Successful moment but no reward to give.");
                    Log.d(KIIP_TAG, "Successful moment but no reward to give.");
                } else {
                    onPoptart(reward);
                }
            }

            @Override
            public void onFailed(Kiip kiip, Exception exception) {
                FirebaseCrash.report(exception);
            }
        });
    }

}
