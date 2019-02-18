package com.manouti.itemfinder.user.rewards;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.model.user.reward.UserReward;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;

import java.util.ArrayList;
import java.util.List;


public class RewardsRecyclerViewAdapter extends RecyclerViewTrackSelectionAdapter<RewardViewHolder> {

    private static final String TAG = "RewardsRecViewAdapter";

    private DatabaseReference mUserRewardsReference;
    private RecyclerViewAdapterEventListener<RewardViewHolder, UserReward> mAdapterEventListener;
    private Context mContext;
    private List<String> mRewardKeys = new ArrayList<>();
    private List<UserReward> mRewards = new ArrayList<>();

    // Allows to remember the last item shown on screen
    private int lastPosition = -1;

    public RewardsRecyclerViewAdapter(DatabaseReference userRewardsReference,
                                      RecyclerViewAdapterEventListener<RewardViewHolder, UserReward> adapterEventListener,
                                      Context context) {
        this.mUserRewardsReference = userRewardsReference;
        this.mAdapterEventListener = adapterEventListener;
        this.mContext = context;
    }

    @Override
    public void clear() {
    }

    @Override
    public RewardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mAdapterEventListener.onCreateAddedItemViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(RewardViewHolder viewHolder, int position) {
        UserReward reward = mRewards.get(position);
        mAdapterEventListener.onBindItem(viewHolder, reward);

        setAnimation(viewHolder.itemView, position);
    }

    public void removeItem(final int itemPosition, final DatabaseReference.CompletionListener completionListener) {
        mUserRewardsReference.child(mRewardKeys.get(itemPosition)).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    mRewardKeys.remove(itemPosition);
                    mRewards.remove(itemPosition);
                    notifyItemRemoved(itemPosition);
                    notifyItemRangeChanged(itemPosition, mRewards.size());
                }
                completionListener.onComplete(databaseError, databaseReference);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRewards.size();
    }

    public void queryRewards() {
        mRewardKeys.clear();
        mRewards.clear();

        mUserRewardsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot rewardSnapshot : children) {
                    UserReward userReward = rewardSnapshot.getValue(UserReward.class);
                    Log.d(TAG, "onDataChange:userReward:moment:" + userReward.getMoment() + "placeName:" + userReward.getPlaceName() + ", itemSummary:" + userReward.getItemSummary());

                    mRewardKeys.add(rewardSnapshot.getKey());
                    mRewards.add(userReward);
                }

                mAdapterEventListener.onItemCountReady(dataSnapshot.getChildrenCount());
                // Update RecyclerView
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "userLocations:onCancelled", databaseError.toException());
            }
        });
    }

    public void cleanUpListener() {

    }

    private void setAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, android.R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

}
