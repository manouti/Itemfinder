package com.manouti.itemfinder.item.additem.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.PlacedItemInfo;
import com.manouti.itemfinder.item.detail.ItemDetailActivity;
import com.manouti.itemfinder.location.LocationViewHolder;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.place.PlaceItem;
import com.manouti.itemfinder.model.user.UserLocation;
import com.manouti.itemfinder.user.items.ReviewProposedItemActivity;
import com.manouti.itemfinder.user.locations.AddUserLocationActivity;
import com.manouti.itemfinder.user.locations.adapter.LocationRecyclerViewAdapter;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.NetworkUtils;
import com.manouti.itemfinder.util.PermissionUtils;
import com.manouti.itemfinder.util.place.PlaceValidator;
import com.manouti.itemfinder.util.recyclerview.DividerItemDecoration;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class PlaceFragment extends BaseFragment implements View.OnClickListener,
                                                        PlaceSelectionListener,
                                                        FragmentLifecycle, RecyclerViewAdapterEventListener<LocationViewHolder, UserLocation> {

    private static final String TAG = PlaceFragment.class.getSimpleName();

    private static final int REQUEST_LOCATION_PERMISSION = 1231;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1232;
    private static final int REQUEST_PLACE_PICKER = 23;

    private LinearLayout mPlaceDetailsLayout;
    private TextView mPlaceNameTextView;
    private TextView mPlaceDetailsTextView;
    private ImageView mGoogleAttributionLogo;
    private ImageButton mNextButton;

    private GoogleApiClient mGoogleApiClient;
    private SupportPlaceAutocompleteFragment mAutocompleteFragment;

    private RecyclerView mLocationsRecyclerView;
    private LocationRecyclerViewAdapter mAdapter;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mUserLocationsReference;
    private DatabaseReference mPlaceItemReference;

    private ProgressBar mProgressBar;
    private ViewStub mNoLocationViewStub;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_place, container, false);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mPlaceNameTextView.getError() != null) {
                    mPlaceNameTextView.setError(null);
                    return true;
                }
                return false;
            }
        });

        // Retrieve the SupportPlaceAutocompleteFragment.
        mAutocompleteFragment = (SupportPlaceAutocompleteFragment) getChildFragmentManager()
                .findFragmentById(R.id.autocomplete_fragment);
        // Register a listener to receive callbacks when a place has been selected or an error has
        // occurred.
        mAutocompleteFragment.setOnPlaceSelectedListener(this);

        ImageButton pickPlaceButton = (ImageButton) rootView.findViewById(R.id.button_pick_place);
        pickPlaceButton.setOnClickListener(this);

        ImageButton previousButton = (ImageButton) rootView.findViewById(R.id.button_previous);
        previousButton.setOnClickListener(this);

        mNextButton = (ImageButton) rootView.findViewById(R.id.button_next);
        setNavigationButtonEnabled(false, mNextButton, R.drawable.ic_navigate_next_black_48dp);
        mNextButton.setOnClickListener(this);

        mPlaceDetailsLayout = (LinearLayout) rootView.findViewById(R.id.place_details_layout);
        mPlaceNameTextView = (TextView) rootView.findViewById(R.id.place_name_text_view);
        mPlaceDetailsTextView = (TextView) rootView.findViewById(R.id.place_details_text_view);

        mGoogleAttributionLogo = (ImageView) rootView.findViewById(R.id.google_attribution_logo);
        mGoogleAttributionLogo.setVisibility(View.INVISIBLE);

        TextView proposePlaceTextView = (TextView) rootView.findViewById(R.id.propose_place_text_view);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getString(R.string.propose_place_text_view));
        Bitmap addLocationBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_add_location_black_24dp);
        spannableStringBuilder.setSpan(new ImageSpan(mAddItemPlaceActivity, addLocationBitmap), 57, 58, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        proposePlaceTextView.setText(spannableStringBuilder, TextView.BufferType.SPANNABLE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) {
            Log.w(TAG, "Current user is unexpectedly null");
            mAddItemPlaceActivity.finish();
            return null;
        }

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mUserLocationsReference = mDatabaseReference.child("user-locations").child(currentUser.getUid());

        mLocationsRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_saved_locations);
        mLocationsRecyclerView.setLayoutManager(new LinearLayoutManager(mAddItemPlaceActivity));
        mLocationsRecyclerView.addItemDecoration(new DividerItemDecoration(mAddItemPlaceActivity));
        mLocationsRecyclerView.setNestedScrollingEnabled(false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.user_locations_progress_bar);
        mNoLocationViewStub = (ViewStub) rootView.findViewById(R.id.no_saved_location_stub);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        mGoogleApiClient = new GoogleApiClient.Builder(mAddItemPlaceActivity)
                .addApi(Places.GEO_DATA_API)
                .build();
        mGoogleApiClient.connect();

        // Listen for saved locations
        mAdapter = new LocationRecyclerViewAdapter(mUserLocationsReference, this);
        mLocationsRecyclerView.setAdapter(mAdapter);
        mAdapter.queryLocations();
    }

    @Override
    public void onStop() {
        super.onStop();

        if(mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        // Clean up locations listener
        mAdapter.cleanUpListener();

        if(mPlaceItemReference != null) {
            mPlaceItemReference.keepSynced(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_pick_place:
                pickPlace();
                break;
            case R.id.button_previous:
                scrollPrevious();
                break;
            case R.id.button_next:
                if(!NetworkUtils.isNetworkAvailable(mAddItemPlaceActivity)) {
                    DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.network_connection_required),
                            getString(R.string.add_item_connection_explanation2));
                    return;
                }
                scrollNext();
                break;
            default:
                // do nothing
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                // The user has resolved the error that occurred when starting the place picker.
                if (resultCode == Activity.RESULT_OK) {
                    startPlacePicker();
                }
                break;
            case REQUEST_PLACE_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    Place place = PlacePicker.getPlace(getActivity(), data);
                    handleSelectedPlace(place);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void pickPlace() {
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(getActivity(), REQUEST_LOCATION_PERMISSION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else {
            // Access to the location has been granted to the app.
            startPlacePicker();
        }
    }

    private void startPlacePicker() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(getActivity()), REQUEST_PLACE_PICKER);
        } catch (GooglePlayServicesRepairableException e) {
            FirebaseCrash.report(e);
            GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), e.getConnectionStatusCode(), REQUEST_GOOGLE_PLAY_SERVICES).show();
        } catch (GooglePlayServicesNotAvailableException e) {
            FirebaseCrash.report(e);
            String errorMessage = getString(R.string.google_play_services_not_available)
                    + ": " + GoogleApiAvailability.getInstance().getErrorString(e.errorCode);
            mAddItemPlaceActivity.showSnackbar(errorMessage);
        }
    }

    private void displayValidationError(int messageResId) {
        mPlaceNameTextView.requestFocus();
        mPlaceNameTextView.setError(getString(messageResId));
    }

    @Override
    public void onPlaceSelected(Place place) {
        handleSelectedPlace(place);
    }

    @Override
    public void onError(Status status) {
        Log.e(TAG, "onError: Status = " + status.toString());
        mAddItemPlaceActivity.showSnackbar("Place selection failed: " + status.getStatusMessage());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Start place picker if the permission has been granted.
            startPlacePicker();
        } else {
            PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getFragmentManager(), "dialog");
        }
    }

    /**
     * Updates the activity's place, and shows place details in the fragment's text views along with any validation errors.
     * A Place object contains details about that place, such as its name, address
     * and phone number. Extract the name, address, phone number, place ID and place types.
     */
    private void handleSelectedPlace(Place googlePlace) {
        // Update holder activity with the selected place
        mAddItemPlaceActivity.updatePlace(googlePlace);

        mGoogleAttributionLogo.setVisibility(View.VISIBLE);

        final CharSequence name = googlePlace.getName();
        final CharSequence address = googlePlace.getAddress();
        final CharSequence phone = StringUtils.defaultIfBlank(googlePlace.getPhoneNumber(), getString(R.string.not_available));
        final CharSequence attributions = ObjectUtils.defaultIfNull(googlePlace.getAttributions(), "");

        mPlaceDetailsLayout.setVisibility(View.VISIBLE);
        mPlaceNameTextView.setText(name.toString());
        mPlaceDetailsTextView.setText(getString(R.string.google_place_detail_text, address, phone, attributions).trim());

        Item item = mAddItemPlaceActivity.getItem();
        if(item == null) {
            Log.w(TAG, "Item is unexpectedly null");
            DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
            scrollFirst();
            return;
        }

        // Validate place
        int placeValidationCode = PlaceValidator.validatePlace(googlePlace, item.getItemType());
        switch (placeValidationCode) {
            case PlaceValidator.VALID_PLACE_FOR_ITEM:
                setNavigationButtonEnabled(true, mNextButton, R.drawable.ic_navigate_next_black_48dp);
                scrollNext();
                mPlaceNameTextView.setError(null);
                break;
            case PlaceValidator.INVALID_PLACE:
                setNavigationButtonEnabled(false, mNextButton, R.drawable.ic_navigate_next_black_48dp);
                displayValidationError(R.string.invalid_place_msg);
                break;
            case PlaceValidator.BAD_PLACE_TYPE:
                setNavigationButtonEnabled(false, mNextButton, R.drawable.ic_navigate_next_black_48dp);
                displayValidationError(R.string.bad_place_type_msg);
                break;
            case PlaceValidator.BAD_PLACE_TYPE_FOR_ITEM_TYPE:
                setNavigationButtonEnabled(false, mNextButton, R.drawable.ic_navigate_next_black_48dp);
                displayValidationError(R.string.bad_place_type_for_item_msg);
                break;
            default:
                // do nothing
        }

        if(mPlaceItemReference != null) {
            mPlaceItemReference.keepSynced(false);
        }
        mPlaceItemReference = mDatabaseReference.child("places").child(googlePlace.getId()).child("items").child(item.getId());
        mPlaceItemReference.keepSynced(true);
    }

    @Override
    public void clearInput() {
        mPlaceNameTextView.setText("");
        mPlaceDetailsTextView.setText("");
        mPlaceDetailsLayout.setVisibility(View.GONE);

        mAutocompleteFragment.setText("");
        setNavigationButtonEnabled(false, mNextButton, R.drawable.ic_navigate_next_black_48dp);
        mAddItemPlaceActivity.disableNextTabs();
        mAddItemPlaceActivity.updatePlace(null);
    }

    // TODO is it solved? Due to disk persistence, if at this point the item was removed from the database, this method incorrectly detects that
    // it still exists.
    @Override
    protected void scrollNext() {
        final Item item = mAddItemPlaceActivity.getItem();
        if(item == null) {
            Log.w(TAG, "Item is unexpectedly null");
            DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
            scrollFirst();
            return;
        }

        final Place googlePlace = mAddItemPlaceActivity.getPlace();
        verifyNonExistingItem(item, googlePlace);
    }

    private void verifyNonExistingItem(final Item item, final Place googlePlace) {
        final String itemId = item.getId();
        final String placeId = googlePlace.getId();
        DatabaseReference placeItemReference = mDatabaseReference.child("places").child(placeId).child("items").child(itemId);
        mAddItemPlaceActivity.syncDatabaseReference(placeItemReference);
        placeItemReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    verifyNotProposedItem(item, googlePlace);
                } else {
                    PlaceItem placeItem = dataSnapshot.getValue(PlaceItem.class);
                    final String userId = placeItem.getUid();
                    DialogUtils.showMessageDialog(mAddItemPlaceActivity, getString(R.string.item_already_exists_in_place),
                            getString(R.string.item_already_exists_view), getString(R.string.item_already_exists_view_confirm),
                            new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    Intent itemDetailIntent = new Intent(mAddItemPlaceActivity, ItemDetailActivity.class);
                                    PlacedItemInfo placedItemInfo = new PlacedItemInfo(item, placeId, googlePlace.getName().toString(), userId, null);
                                    itemDetailIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_PLACED_INPUT, placedItemInfo);
                                    startActivity(itemDetailIntent);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "placeItemCheckExist:onCancelled", databaseError.toException());
                DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
                scrollFirst();
            }
        });
    }

    private void verifyNotProposedItem(final Item item, final Place googlePlace) {
        final String itemId = item.getId();
        final String placeId = googlePlace.getId();
        DatabaseReference placeItemReference = mDatabaseReference.child("proposed-items").child(placeId).child("items").child(itemId);
        mAddItemPlaceActivity.syncDatabaseReference(placeItemReference);
        placeItemReference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    verifyNotRejectedItem(item, googlePlace);
                } else {
                    final PlaceItem placeItem = dataSnapshot.getValue(PlaceItem.class);
                    final String userId = placeItem.getUid();
                    final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) {
                        Log.w(TAG, "Current user is unexpectedly null");
                        DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
                        scrollFirst();
                        return;
                    }
                    if (!userId.equals(currentUser.getUid())) {
                        DialogUtils.showMessageDialog(mAddItemPlaceActivity, getString(R.string.item_already_exists_in_place_as_proposed),
                                getString(R.string.item_already_submitted_review), getString(R.string.item_already_exists_view_confirm),
                                new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                        Intent proposedItemIntent = new Intent(mAddItemPlaceActivity, ReviewProposedItemActivity.class);
                                        proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_ID, itemId);
                                        proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_PLACE_ID, placeId);
                                        proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_USER_ID, userId);
                                        startActivity(proposedItemIntent);
                                        mAddItemPlaceActivity.finish();
                                    }
                                });
                    } else {
                        DialogUtils.showMessageDialog(mAddItemPlaceActivity, getString(R.string.item_already_proposed_by_current_user),
                                getString(R.string.item_already_exists_view), getString(R.string.item_already_exists_view_confirm),
                                new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                        Intent itemDetailIntent = new Intent(mAddItemPlaceActivity, ItemDetailActivity.class);
                                        PlacedItemInfo placedItemInfo = new PlacedItemInfo(item, placeId, googlePlace.getName().toString(), userId, null);
                                        itemDetailIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_PLACED_INPUT, placedItemInfo);
                                        startActivity(itemDetailIntent);
                                    }
                                });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "placeItemCheckExist:onCancelled", databaseError.toException());
                DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
                scrollFirst();
            }
        });
    }

    private void verifyNotRejectedItem(final Item item, final Place googlePlace) {
        final String itemId = item.getId();
        final String placeId = googlePlace.getId();
        DatabaseReference placeItemReference = mDatabaseReference.child("rejected-items").child(placeId).child("items").child(itemId);
        mAddItemPlaceActivity.syncDatabaseReference(placeItemReference);
        placeItemReference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    PlaceFragment.super.scrollNext();
                } else {
                    final PlaceItem placeItem = dataSnapshot.getValue(PlaceItem.class);
                    final String userId = placeItem.getUid();
                    final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) {
                        Log.w(TAG, "Current user is unexpectedly null");
                        DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
                        scrollFirst();
                        return;
                    }
                    if (!userId.equals(currentUser.getUid())) {
                        DialogUtils.showMessageDialog(mAddItemPlaceActivity, getString(R.string.item_already_exists_in_place_as_proposed),
                                getString(R.string.item_already_submitted_review), getString(R.string.item_already_exists_view_confirm),
                                new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                        Intent proposedItemIntent = new Intent(mAddItemPlaceActivity, ReviewProposedItemActivity.class);
                                        proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_ID, itemId);
                                        proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_PLACE_ID, placeId);
                                        proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_USER_ID, userId);
                                        startActivity(proposedItemIntent);
                                        mAddItemPlaceActivity.finish();
                                    }
                                });
                    } else {
                        DialogUtils.showMessageDialog(mAddItemPlaceActivity, getString(R.string.item_already_proposed_by_current_user),
                                getString(R.string.item_already_exists_view), getString(R.string.item_already_exists_view_confirm),
                                new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                                        Intent itemDetailIntent = new Intent(mAddItemPlaceActivity, ItemDetailActivity.class);
                                        PlacedItemInfo placedItemInfo = new PlacedItemInfo(item, placeId, googlePlace.getName().toString(), userId, null);
                                        itemDetailIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_PLACED_INPUT, placedItemInfo);
                                        startActivity(itemDetailIntent);
                                    }
                                });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "placeItemCheckExist:onCancelled", databaseError.toException());
                DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.error_adding_item), null);
                scrollFirst();
            }
        });
    }

    @Override
    public void onPausePagerFragment() {
    }

    @Override
    public void onResumePagerFragment() {
    }

    @Override
    public void onItemCountReady(long itemCount) {
        mProgressBar.setVisibility(View.GONE);
        if(itemCount == 0) {
            if (mNoLocationViewStub.getParent() != null) {
                View addLocationView = mNoLocationViewStub.inflate();
                addLocationView.findViewById(R.id.button_save_location).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent addUserLocationIntent = new Intent(mAddItemPlaceActivity, AddUserLocationActivity.class);
                        startActivity(addUserLocationIntent);
                    }
                });
            } else {
                mNoLocationViewStub.setVisibility(View.VISIBLE);
            }
        } else {
            mNoLocationViewStub.setVisibility(View.GONE);
        }
    }

    @Override
    public void onError(Exception ex) {
        if(ex != null) {
            FirebaseCrash.report(ex);
        }
        mAddItemPlaceActivity.showSnackbar(R.string.unexpected_error_try_later);
    }

    @Override
    public LocationViewHolder onCreateAddedItemViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mAddItemPlaceActivity);
        View view = inflater.inflate(R.layout.location, parent, false);

        LocationViewHolder locationViewHolder = new LocationViewHolder(mAddItemPlaceActivity, mAdapter, view) {
            @Override
            public void onClick(View v) {
                super.onClick(v);
                int position = mLocationsRecyclerView.getChildAdapterPosition(v);
                String placeId = mAdapter.mLocationPlaceIds.get(position);
                Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId).setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(@NonNull PlaceBuffer placeBuffer) {
                        if (placeBuffer.getStatus().isSuccess() && placeBuffer.getCount() > 0) {
                            final Place place = placeBuffer.get(0);
                            Log.i(TAG, "Place found: " + place.getName());
                            Place frozenPlace = place.freeze();
                            handleSelectedPlace(frozenPlace);
                        } else {
                            Log.e(TAG, "Place not found");
                        }
                        placeBuffer.release();
                    }
                });
            }
        };

        locationViewHolder.titleView = (TextView) view.findViewById(R.id.location_title);
        locationViewHolder.descriptionView = (TextView) view.findViewById(R.id.location_description);
        locationViewHolder.starView = (ImageView) view.findViewById(R.id.location_star);

        return locationViewHolder;
    }

    @Override
    public void onBindItem(LocationViewHolder viewHolder, UserLocation location) {
        viewHolder.titleView.setText(location.getPlaceTitle());
        viewHolder.descriptionView.setText(location.getPlaceDescription());
        if(location.isFavorite()) {
            viewHolder.starView.setImageResource(R.drawable.ic_toggle_star_24);
            viewHolder.starView.setVisibility(View.VISIBLE);
        }
    }
}
