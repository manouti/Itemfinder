package com.manouti.itemfinder.item.additem.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.item.ItemType;
import com.manouti.itemfinder.model.place.Place;
import com.manouti.itemfinder.model.place.PlaceItem;
import com.manouti.itemfinder.model.place.PlaceItem.ItemAcceptanceStatus;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.NetworkUtils;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;
import me.kiip.sdk.Kiip;
import me.kiip.sdk.Poptart;


public class ReviewItemPlaceFragment extends BaseFragment implements View.OnClickListener, FragmentLifecycle {

    private static final String TAG = ReviewItemPlaceFragment.class.getSimpleName();
    private static final String KIIP_TAG = TAG + ":Kiip";

    private DatabaseReference mDatabaseReference;
    private GeoFire mPlacesGeoFire;

    private TextView mItemSummaryTextView;
    private TextView mItemDescriptionTextView;
    private TextView mIdTextView;
    private TextView mTypeTextView;
    private TextView mTimeTextView;
    private TextView mPlaceTitleTextView;
    private TextView mPlaceDescriptionTextView;
    private Button mAddItemButton;

    private TextView mNoConnectionTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated properly.
        View rootView = inflater.inflate(R.layout.fragment_review_add_item, container, false);

        ImageButton previousButton = (ImageButton) rootView.findViewById(R.id.button_previous);
        previousButton.setOnClickListener(this);

        mAddItemButton = (Button) rootView.findViewById(R.id.button_add_item);
        mAddItemButton.setOnClickListener(this);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mItemSummaryTextView = (TextView) rootView.findViewById(R.id.item_summary_text_view);
        mItemDescriptionTextView = (TextView) rootView.findViewById(R.id.item_description_text_view);
        mIdTextView = (TextView) rootView.findViewById(R.id.id_text_view);
        mTypeTextView = (TextView) rootView.findViewById(R.id.type_text_view);
        mTimeTextView = (TextView) rootView.findViewById(R.id.time_text_view);
        mPlaceTitleTextView = (TextView) rootView.findViewById(R.id.place_title_text_view);
        mPlaceDescriptionTextView = (TextView) rootView.findViewById(R.id.place_description_text_view);

        mNoConnectionTextView = (TextView) rootView.findViewById(R.id.no_connection_text_view);

        mPlacesGeoFire = new GeoFire(mDatabaseReference.child("places-geo"));

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_add_item:
                // Prevent clicking the button more than once
                mAddItemButton.setEnabled(false);
                addItemPlace(mAddItemPlaceActivity.getItem(), mAddItemPlaceActivity.getPlace());
                break;
            case R.id.button_previous:
                scrollPrevious();
                break;
            default:
                // do nothing
        }
    }

    private void addItemPlace(final Item item, final com.google.android.gms.location.places.Place googlePlace) {
        final SweetAlertDialog progressDialog = DialogUtils.showProgressDialog(mAddItemPlaceActivity, getString(R.string.progress_adding_item), null);
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) {
            Log.w(TAG, "Current user is unexpectedly null");
            DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
            scrollFirst();
            progressDialog.dismiss();
            return;
        }

        // Update Geofire location
        // then locations at /items/$itemId and /places/$placeId/items/$itemId simultaneously
        final String itemId = item.getId();
        final String placeId = googlePlace.getId();
        final LatLng latLng = googlePlace.getLatLng();

        DatabaseReference userAddedItemsRef = mDatabaseReference.child("user-addeditems").child(currentUser.getUid());
        mAddItemPlaceActivity.syncDatabaseReference(userAddedItemsRef);
        userAddedItemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final boolean firstProposedItem = dataSnapshot.getChildrenCount() == 0;

                mDatabaseReference.child("places").child(placeId).runTransaction(new Transaction.Handler() {
                    @Override // TODO I don't see how the transaction is being used here
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        Map<String, Object> updateMap = new HashMap<>();

                        Place place = new Place(placeId, googlePlace.getName(), googlePlace.getAddress(),
                                googlePlace.getPhoneNumber(), googlePlace.getWebsiteUri() != null ? googlePlace.getWebsiteUri().toString() : null);
                        mDatabaseReference.child("places").child(placeId).updateChildren(place.toMap());
                        mDatabaseReference.child("items").child(itemId).updateChildren(item.toMap());

                        // Insert or update place location in GeoFire
                        mPlacesGeoFire.setLocation(placeId, new GeoLocation(latLng.latitude, latLng.longitude));

                        PlaceItem placeItem = new PlaceItem(itemId, item.getS(), item.getDesc(), currentUser.getUid(), 0, 0);

                        updateMap.put("/proposed-items/" + placeId + "/items/" + itemId, placeItem.toMap());
                        updateMap.put("/user-addeditems/" + currentUser.getUid() + "/" + placeId + "/placeName", place.getName());
                        updateMap.put("/user-addeditems/" + currentUser.getUid() + "/" + placeId + "/items/" + itemId + "/itemS", item.getS());
                        updateMap.put("/user-addeditems/" + currentUser.getUid() + "/" + placeId + "/items/" + itemId + "/status", ItemAcceptanceStatus.PROPOSED.toString());
                        mDatabaseReference.updateChildren(updateMap);

                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                        progressDialog.dismiss();
                        if (committed) {
                            DialogUtils.showSuccessDialog(mAddItemPlaceActivity, getString(R.string.notice_item_added), null, new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    if(firstProposedItem) {
                                        sendRewardToUser();
                                    } else {
                                        mAddItemPlaceActivity.finish();
                                    }
                                }
                            });
                        } else {
                            if (databaseError != null) {
                                Log.e(TAG, getString(R.string.error_adding_item) + ": " + databaseError);
                                DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
                                scrollFirst();
                            }
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.log("user-addeditems:" + currentUser.getUid() + ":onCancelled");
                FirebaseCrash.report(databaseError.toException());
            }
        });
    }

    @Override
    public void onPausePagerFragment() {
    }

    @Override
    public void onResumePagerFragment() {
        if(NetworkUtils.isNetworkAvailable(mAddItemPlaceActivity)) {
            mNoConnectionTextView.setVisibility(View.GONE);
            mAddItemButton.setEnabled(true);
        } else {
            mNoConnectionTextView.setVisibility(View.VISIBLE);
            mAddItemButton.setEnabled(false);
            return;
        }

        Item item = mAddItemPlaceActivity.getItem();
        if(item == null) {
            Log.w(TAG, "Item is unexpectedly null");
            DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
            scrollFirst();
            return;
        }

        final com.google.android.gms.location.places.Place googlePlace = mAddItemPlaceActivity.getPlace();
        if(googlePlace == null) {
            Log.w(TAG, "Place is unexpectedly null");
            DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
            scrollFirst();
            return;
        }

        displaySummary(item, googlePlace);
    }

    private void displaySummary(Item item, com.google.android.gms.location.places.Place googlePlace) {
        mItemSummaryTextView.setText(item.getS());
        mItemDescriptionTextView.setText(item.getDesc());

        String itemId = item.getId();
        mIdTextView.setText(itemId);

        ItemType itemType = item.getItemType();
        if (itemType != null) {
            mTypeTextView.setText(itemType.toString());
        }

        long timestamp = item.getTimestamp();
        if (timestamp >= 0) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            mTimeTextView.setText(formatter.format(new Date(timestamp)));
        }

        final CharSequence name = googlePlace.getName();
        final CharSequence address = googlePlace.getAddress();
        final CharSequence phone = StringUtils.defaultIfBlank(googlePlace.getPhoneNumber(), getString(R.string.not_available));
        final CharSequence attributions = ObjectUtils.defaultIfNull(googlePlace.getAttributions(), "");

        mPlaceTitleTextView.setText(name.toString());
        mPlaceDescriptionTextView.setText(getString(R.string.google_place_detail_text, address, phone, attributions).trim());
    }

    private void sendRewardToUser() {
        Kiip.getInstance().saveMoment(getString(R.string.kiip_first_proposed_item_moment_id), new Kiip.Callback() {

            @Override
            public void onFinished(Kiip kiip, Poptart reward) {
                if (reward == null) {
                    FirebaseCrash.log("Successful moment but no reward to give.");
                    Log.d(KIIP_TAG, "Successful moment but no reward to give.");
                } else {
                    mAddItemPlaceActivity.onPoptart(reward);
                }
                mAddItemPlaceActivity.finish();
            }

            @Override
            public void onFailed(Kiip kiip, Exception exception) {
                FirebaseCrash.report(exception);
                mAddItemPlaceActivity.finish();
            }
        });
    }

    @Override
    public void clearInput() {
        mItemSummaryTextView.setText("");
        mItemDescriptionTextView.setText("");
        mIdTextView.setText("");
        mTypeTextView.setText("");
        mTimeTextView.setText("");
        mPlaceTitleTextView.setText("");
        mPlaceDescriptionTextView.setText("");
    }
}
