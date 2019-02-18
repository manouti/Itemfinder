package com.manouti.itemfinder.user.items;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.ui.auth.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;
import com.manouti.itemfinder.BaseActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.detail.ItemDetailArrayAdapter;
import com.manouti.itemfinder.item.score.LocationItemScoreEvaluator;
import com.manouti.itemfinder.item.score.ScoreRankedItem;
import com.manouti.itemfinder.location.provider.AbstractLocationProvider.LocationResultCallback;
import com.manouti.itemfinder.location.provider.CurrentLocationProvider;
import com.manouti.itemfinder.location.provider.UserLocationProvider;
import com.manouti.itemfinder.model.item.ISBNItem;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.item.ItemType;
import com.manouti.itemfinder.model.item.Product;
import com.manouti.itemfinder.model.item.VINItem;
import com.manouti.itemfinder.model.place.Place;
import com.manouti.itemfinder.model.place.PlaceItem;
import com.manouti.itemfinder.model.place.PlaceItem.ItemAcceptanceStatus;
import com.manouti.itemfinder.model.user.reward.UserReward;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.NetworkUtils;
import com.manouti.itemfinder.util.PermissionUtils;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ReviewProposedItemActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = ReviewProposedItemActivity.class.getSimpleName();

    private static final int REQUEST_LOCATION_PERMISSION = 1231;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1232;

    private static final int VOTE_DIFFERENCE_NEEDED_TO_ACCEPT_OR_REJECT = 5;
    private static final int APPROVE_REPUTATION_INCREASE = 50;
    private static final int REJECT_REPUTATION_DECREASE = 20;

    private static final int NUMBER_OF_ADDED_ITEMS_NEEDED_FOR_NEXT_REWARD = 1; //TODO restore to 5 after testing

    private TextView mItemSummaryTextView;
    private TextView mItemDescriptionTextView;
    private TextView mPlaceNameTextView;
    private TextView mPlaceDetailsTextView;

    private CardView mItemDetailsCardView;
    private ListView mItemDetailsListView;

    private Button mApproveButton;
    private Button mRejectButton;

    private ToolTipRelativeLayout mToolTipRelativeLayout;
    private ToolTipView mToolTipView;

    private DatabaseReference mDatabaseReference;
    private CurrentLocationProvider mCurrentLocationProvider;
    private UserLocationProvider mUserLocationProvider;

    private Item mItem;
    private String mItemId;
    private String mPlaceId;
    private Place mPlace;
    private String mUserId;

    private String mCurrentUserId;
    private SweetAlertDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mItemSummaryTextView = (TextView) findViewById(R.id.item_summary_text_view);
        mItemDescriptionTextView = (TextView) findViewById(R.id.item_description_text_view);
        mPlaceNameTextView = (TextView) findViewById(R.id.place_name_text_view);
        mPlaceDetailsTextView = (TextView) findViewById(R.id.place_details_text_view);

        mItemDetailsCardView = (CardView) findViewById(R.id.new_item_details_card_view);
        mItemDetailsListView = (ListView) findViewById(R.id.new_item_details_listview);

        mApproveButton = (Button) findViewById(R.id.button_approve);
        mRejectButton = (Button) findViewById(R.id.button_reject);

        mToolTipRelativeLayout = (ToolTipRelativeLayout) findViewById(R.id.tooltip_relative_layout);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) {
            Log.w(TAG, "Current user is unexpectedly null");
            finish();
            return;
        }
        mCurrentUserId = currentUser.getUid();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_proposed_item;
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.mItemId = getIntent().getStringExtra(Intents.PROPOSED_ITEM_ID);
        mDatabaseReference.child("items").child(mItemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot itemSnapshot) {
                ItemType type = ItemType.valueOf(itemSnapshot.child("type").getValue(String.class));
                switch (type) {
                    case PRODUCT:
                        mItem = itemSnapshot.getValue(Product.class);
                        break;
                    case ISBN:
                        mItem = itemSnapshot.getValue(ISBNItem.class);
                        break;
                    case VIN:
                        mItem = itemSnapshot.getValue(VINItem.class);
                        break;
                    default:
                        mItem = itemSnapshot.getValue(Item.class);
                }
                if (mItem != null) {
                    mItemSummaryTextView.setText(mItem.getS());
                    mItemDescriptionTextView.setText(mItem.getDesc());
                    mItemDetailsCardView.setVisibility(View.VISIBLE);
                    mItemDetailsListView.setAdapter(new ItemDetailArrayAdapter(ReviewProposedItemActivity.this, mItem));

                    mApproveButton.setEnabled(true);
                    mApproveButton.setOnClickListener(ReviewProposedItemActivity.this);
                    mRejectButton.setEnabled(true);
                    mRejectButton.setOnClickListener(ReviewProposedItemActivity.this);
                } else {
                    FirebaseCrash.log("Proposed item was null when retrieving it from " + mItemId);
                    showSnackbar(R.string.error_viewing_item);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getItemById:onCancelled", databaseError.toException());
            }
        });

        mPlaceId = getIntent().getStringExtra(Intents.PROPOSED_ITEM_PLACE_ID);
        mDatabaseReference.child("places").child(mPlaceId).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot placeSnapshot) {
                mPlace = placeSnapshot.getValue(Place.class);
                if(mPlace != null) {
                    mPlace.setId(mPlaceId);
                    mPlaceNameTextView.setText(mPlace.getName());
                    mPlaceDetailsTextView.setText(getString(R.string.place_detail_text, mPlace.getAddr(), StringUtils.defaultIfBlank(mPlace.getPhone(), getString(R.string.not_available))));
                } else {
                    FirebaseCrash.log("Proposed item's place was null when retrieving it from " + mPlaceId);
                    showSnackbar(R.string.error_viewing_item);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getPlaceById:onCancelled", databaseError.toException());
            }
        });

        mUserId = getIntent().getStringExtra(Intents.PROPOSED_ITEM_USER_ID);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showHelp(View view) {
        if(mToolTipView == null) {
            ToolTip toolTip = new ToolTip()
                    .withText(Html.fromHtml(getString(R.string.review_help_text)))
                    .withColor(ContextCompat.getColor(this, R.color.tooltip_color))
                    .withShadow()
                    .withAnimationType(ToolTip.AnimationType.FROM_TOP);
            mToolTipView = mToolTipRelativeLayout.showToolTipForView(toolTip, findViewById(R.id.help_button));
            mToolTipView.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
                @Override
                public void onToolTipViewClicked(ToolTipView toolTipView) {
                    mToolTipView = null;
                }
            });
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(mToolTipView != null) {
            Rect rect = new Rect();
            mToolTipView.getHitRect(rect);
            if (!rect.contains((int) event.getX(), (int) event.getY())) {
                mToolTipView.remove();
                mToolTipView = null;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_approve:
                approveOrReject(true);
                break;
            case R.id.button_reject:
                approveOrReject(true);
                break;
            default:
                // do nothing
        }
    }

    private void approveOrReject(boolean approve) {
        mProgressDialog = DialogUtils.showProgressDialog(this, getString(R.string.progress_retrieving_next_proposed_item), null);
        if(approve) {
            approve();
        } else {
            reject();
        }

        mApproveButton.setEnabled(false);
        mRejectButton.setEnabled(false);

        reviewNextItem();
    }

    private void approve() {
        mDatabaseReference.child("places").child(mPlaceId).child("items").child(mItemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final PlaceItem placeItem = dataSnapshot.getValue(PlaceItem.class);
                if (placeItem != null) {
                    final Map<String, Object> updateMap = new HashMap<>();

                    placeItem.setUpvote(placeItem.getUpvote() + 1);
                    if (placeItem.getUpvote() - placeItem.getDownvote() >= VOTE_DIFFERENCE_NEEDED_TO_ACCEPT_OR_REJECT) {
                        updateMap.put("/user-addeditems/" + mUserId + "/" + mPlaceId + "/items/" + mItemId + "/status", ItemAcceptanceStatus.APPROVED.toString());
                        updateMap.put("/item-places/" + mItemId + "/" + mPlaceId, true);
                        updateMap.put("/proposed-items/" + mPlaceId + "/items/" + mItemId, null);
                        updateMap.put("/places/" + mPlaceId + "/items/" + mItemId, placeItem.toMap());

                        List<String> categories = mItem.getCategories();
                        if(categories != null) {
                            for(String category : categories) {
                                updateMap.put("/places/" + mPlaceId + "/itemsCat/" + category + "/" + mItemId, mItem.getRating());
                            }
                        }
                        mDatabaseReference.child("users").child(mUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                final User user = dataSnapshot.getValue(User.class);
                                final long newReputation = user.getRep() + APPROVE_REPUTATION_INCREASE;
                                // Update user reputation
                                updateMap.put("/users/" + mUserId + "/rep", newReputation);
                                // Update last reviewed timestamp for this user
                                updateMap.put("/user-revieweditems/" + mCurrentUserId + "/" + placeItem.getUid()
                                        + "/lastRev", ServerValue.TIMESTAMP);
                                updateMap.put("/user-revieweditems/" + mCurrentUserId + "/" + placeItem.getUid()
                                        + "/" + mItemId, true);

                                mDatabaseReference.updateChildren(updateMap, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        computeAndSendReward(databaseReference, placeItem.getItemSummary(), newReputation);
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(TAG, "approveProposedItem:getUser:onCancelled", databaseError.toException());
                                DialogUtils.showErrorDialog(ReviewProposedItemActivity.this, getString(R.string.error_approving_item), null);
                            }
                        });
                    } else {
                        updateMap.put("/places/" + mPlaceId + "/items/" + mItemId, placeItem.toMap());
                        // Update last reviewed timestamp for this user
                        updateMap.put("/user-revieweditems/" + mCurrentUserId + "/" + placeItem.getUid()
                                + "/lastRev", ServerValue.TIMESTAMP);
                        updateMap.put("/user-revieweditems/" + mCurrentUserId + "/" + placeItem.getUid()
                                + "/" + mItemId, true);
                        mDatabaseReference.updateChildren(updateMap);
                    }
                } else {
                    FirebaseCrash.log("Proposed item " + mItemId + " at place " + mPlaceId + " was unexpectedly null.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "approveProposedItem:onCancelled", databaseError.toException());
                DialogUtils.showErrorDialog(ReviewProposedItemActivity.this, getString(R.string.error_approving_item), null);
            }
        });
    }

    private void computeAndSendReward(final DatabaseReference databaseReference, final String itemSummary, final long newReputation) {
        databaseReference.child("user-addeditems").child(mUserId).child(mPlaceId).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int approvedItemCount = 0;
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    if (ItemAcceptanceStatus.APPROVED.toString().equals(item.child("status").getValue())) {
                        approvedItemCount++;
                    }
                }
                if (approvedItemCount % NUMBER_OF_ADDED_ITEMS_NEEDED_FOR_NEXT_REWARD == 0) {
                    UserReward userReward = new UserReward();
                    userReward.setItemSummary(itemSummary);
                    userReward.setPlaceName(mPlace.getName());
                    userReward.setMoment(getString(R.string.kiip_added_5_new_items_moment_id));
                    userReward.setNewRep(newReputation);
                    DatabaseReference rewardReference = mDatabaseReference.child("user-rewards").child(mUserId).push();
                    rewardReference.setValue(userReward.toMap());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        });
    }

    private void reject() {
        mDatabaseReference.child("places").child(mPlaceId).child("items").child(mItemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final PlaceItem placeItem = dataSnapshot.getValue(PlaceItem.class);
                if (placeItem != null) {
                    final Map<String, Object> updateMap = new HashMap<>();

                    placeItem.setDownvote(placeItem.getDownvote() + 1);
                    if (placeItem.getDownvote() - placeItem.getUpvote() >= VOTE_DIFFERENCE_NEEDED_TO_ACCEPT_OR_REJECT) {
                        updateMap.put("/user-addeditems/" + mUserId + "/" + mPlaceId + "/items/" + mItemId + "/status", ItemAcceptanceStatus.REJECTED.toString());
                        updateMap.put("/proposed-items/" + mPlaceId + "/items/" + mItemId, null);
                        updateMap.put("/rejected-items/" + mPlaceId + "/items/" + mItemId, placeItem.toMap());
                        mDatabaseReference.child("users").child(mUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                final User user = dataSnapshot.getValue(User.class);
                                //TODO block user from adding item if rep decreases for consecutive times
                                long newReputation = Math.max(user.getRep() - REJECT_REPUTATION_DECREASE, 0);
                                updateMap.put("/users/" + mUserId + "/rep", newReputation);
                                updateMap.put("/user-revieweditems/" + mCurrentUserId + "/" + placeItem.getUid()
                                        + "/lastRev", ServerValue.TIMESTAMP);
                                updateMap.put("/user-revieweditems/" + mCurrentUserId + "/" + placeItem.getUid()
                                        + "/" + mItemId, false);
                                mDatabaseReference.updateChildren(updateMap);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(TAG, "rejectProposedItem:getUser:onCancelled", databaseError.toException());
                                DialogUtils.showErrorDialog(ReviewProposedItemActivity.this, getString(R.string.error_approving_item), null);
                            }
                        });
                    } else {
                        updateMap.put("/places/" + mPlaceId + "/items/" + mItemId, placeItem.toMap());
                        updateMap.put("/user-revieweditems/" + mCurrentUserId + "/" + placeItem.getUid()
                                + "/" + mItemId, false);
                        mDatabaseReference.updateChildren(updateMap);
                    }
                } else {
                    FirebaseCrash.log("Proposed item " + mItemId + " at place " + mPlaceId + " was unexpectedly null.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "rejectProposedItem:onCancelled", databaseError.toException());
                DialogUtils.showErrorDialog(ReviewProposedItemActivity.this, getString(R.string.error_approving_item), null);
            }
        });
    }

    /**
     * Finishes this activity instance and starts another instance for the next proposed item. If no item is available for review,
     * it displays a message to the user.
     */
    private void reviewNextItem() {
        // Current location provider requires permission to access fine location, ask for permission first.
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, REQUEST_LOCATION_PERMISSION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else {
            // Access to the location has been granted to the app.
            if(mCurrentLocationProvider == null) {
                mCurrentLocationProvider = createCurrentLocationProvider();
                mCurrentLocationProvider.start();
            } else {
                mCurrentLocationProvider.restart();
            }
        }
    }

    private CurrentLocationProvider createCurrentLocationProvider() {
        return new CurrentLocationProvider(this, REQUEST_GOOGLE_PLAY_SERVICES, new LocationResultCallback() {
            @Override
            public void onLocationReceived(@NonNull Location location) {
                queryLocationForNextItem(location);
            }

            @Override
            public void onLocationNotAvailable(String failureMessage) {
                showSnackbar(failureMessage);
            }

            @Override
            public void onLocationSettingsNotAvailable() {
                refreshFeaturedItemsAdapterBasedOnUserLocation();
            }
        });
    }

    private void refreshFeaturedItemsAdapterBasedOnUserLocation() {
        // If current location is not available, use offline registered location
        if(mUserLocationProvider == null) {
            mUserLocationProvider = new UserLocationProvider(this, new LocationResultCallback() {
                @Override
                public void onLocationReceived(@NonNull Location location) {
                    queryLocationForNextItem(location);
                }

                @Override
                public void onLocationNotAvailable(String failureMessage) {
                    if(mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                    if(!NetworkUtils.isNetworkAvailable(ReviewProposedItemActivity.this)) {
                        failureMessage = getString(R.string.no_internet_connection);
                    }
                    DialogUtils.showErrorDialog(ReviewProposedItemActivity.this, failureMessage, null, new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            finish();
                        }
                    });
                }

                @Override
                public void onLocationSettingsNotAvailable() {
                    // this is not called by the user location provider, so do nothing
                }
            });
            mUserLocationProvider.start();
        } else {
            mUserLocationProvider.restart();
        }
    }

    private void queryLocationForNextItem(Location location) {
        final Set<ScoreRankedItem> scoreRankedItems = new HashSet<>();
        final LocationItemScoreEvaluator itemScoreEvaluator = new LocationItemScoreEvaluator(location);

        GeoFire placesGeoFire = new GeoFire(mDatabaseReference.child("places-geo"));
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float radius = Float.parseFloat(prefs.getString(ItemfinderPreferencesActivity.KEY_LOCATION_RADIUS,
                getString(R.string.preferences_default_nearby_fav_item_proximity))); // in kilometers

        GeoQuery geoQuery = placesGeoFire.queryAtLocation(
                new GeoLocation(location.getLatitude(), location.getLongitude()), radius);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(final String placeId, final GeoLocation location) {
                mDatabaseReference.child("places").child(placeId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final Place place = dataSnapshot.getValue(Place.class);
                        if(place != null) {
                            place.setId(placeId);
                            place.setLatitude(location.latitude);
                            place.setLongitude(location.longitude);
                            mDatabaseReference.child("proposed-items").child(placeId).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    final Iterable<DataSnapshot> items = dataSnapshot.getChildren();
                                    for(DataSnapshot itemSnapshot : items) {
                                        final PlaceItem placeItem = itemSnapshot.getValue(PlaceItem.class);
                                        final String itemId = itemSnapshot.getKey();
                                        placeItem.setItemId(itemId);
                                        if(!StringUtils.equals(placeItem.getUid(), mCurrentUserId)) {
                                            mDatabaseReference.child("items").child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    final Item item = dataSnapshot.getValue(Item.class);
                                                    item.setId(itemId);
                                                    final double itemScore = itemScoreEvaluator.getItemScore(place.getLatitude(), place.getLongitude());
                                                    mDatabaseReference.child("users").child(placeItem.getUid()).child("dn").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                            String userDisplayName = dataSnapshot.getValue(String.class);
                                                            ScoreRankedItem rankedItem = new ScoreRankedItem(item, place.getId(), place.getName(), placeItem.getUid(), userDisplayName, itemScore);
                                                            scoreRankedItems.add(rankedItem);
                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {
                                                            Log.w(TAG, "getUserDisplayName:onCancelled", databaseError.toException());
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                    Log.w(TAG, "getItemDesc:onCancelled", databaseError.toException());
                                                }
                                            });
                                        }
                                    }

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.w(TAG, "getPlaceItems:onCancelled", databaseError.toException());
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getPlace:onCancelled", databaseError.toException());
                    }
                });

            }

            @Override
            public void onKeyExited(final String placeId) {
                // Do nothing
            }

            @Override
            public void onKeyMoved(String placeId, GeoLocation location) {
                // Do nothing
            }

            @Override
            public void onGeoQueryReady() {
                if(mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }

                if(!scoreRankedItems.isEmpty()) {
                    Iterator<ScoreRankedItem> iterator = scoreRankedItems.iterator();
                    ScoreRankedItem maxScoredItem = iterator.next();
                    while(iterator.hasNext()) {
                        ScoreRankedItem item = iterator.next();
                        if(item.getScore() > maxScoredItem.getScore()) {
                            maxScoredItem = item;
                        }
                    }
                    Intent proposedItemIntent = new Intent(ReviewProposedItemActivity.this, ReviewProposedItemActivity.class);
                    proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_ID, maxScoredItem.getItemId());
                    proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_PLACE_ID, maxScoredItem.getPlaceId());
                    proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_USER_ID, maxScoredItem.getUserId());
                    startActivity(proposedItemIntent);
                }
                finish();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                DialogUtils.showErrorDialog(ReviewProposedItemActivity.this, getString(R.string.error_geo_query), null, new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        finish();
                    }
                });
            }
        });
    }

    @Override
    @SuppressWarnings({"ResourceType"})
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Start current location provider if the permission has been granted.
            if (mCurrentLocationProvider != null) {
                mCurrentLocationProvider.restart();
            } else {
                mCurrentLocationProvider = createCurrentLocationProvider();
                mCurrentLocationProvider.start();
            }
        } else {
            PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getSupportFragmentManager(), "dialog");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                // The user has resolved the error that occurred connecting to Google Play Services
                if (resultCode == Activity.RESULT_OK) {
                    mCurrentLocationProvider.restart();
                }
                break;
            case CurrentLocationProvider.LOCATION_REQUEST_CHECK_SETTINGS:
                // The user has enabled appropriate location settings needed for Google location requests
                if (resultCode == Activity.RESULT_OK) {
                    mCurrentLocationProvider.restart();
                } else {
                    showSnackbar(R.string.location_settings_unavailable_using_offline_locations);
                    refreshFeaturedItemsAdapterBasedOnUserLocation();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
