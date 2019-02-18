package com.manouti.itemfinder.user;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.auth.model.User;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.BaseNavigationActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.viewholder.ItemViewHolder;
import com.manouti.itemfinder.user.items.UserAddedItem;
import com.manouti.itemfinder.user.items.adapter.AddedItemsRecyclerViewAdapter;
import com.manouti.itemfinder.util.firebase.FirebaseImageLoader;
import com.manouti.itemfinder.util.firebase.FirebaseStorageUtil;
import com.manouti.itemfinder.util.recyclerview.DividerItemDecoration;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;

import org.apache.commons.lang3.StringUtils;

public class UserProfileActivity extends BaseNavigationActivity implements RecyclerViewAdapterEventListener<ItemViewHolder, UserAddedItem> {

    private static final String TAG = UserProfileActivity.class.getSimpleName();

    private static final int EDIT_PROFILE_MENU_ITEM_ID = 114;
    private static final int REQUEST_EDIT_PROFILE = 214;

    private DatabaseReference mDatabaseReference;
    private StorageReference mPhotoStorageReference;

    private String mUid;
    private boolean mViewingCurrentUserProfile;

    private TextView mItemsAddedTextView;
    private RecyclerView mAddedItemsRecyclerView;

    private AddedItemsRecyclerViewAdapter addedItemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = getCurrentUser();
        mUid = getIntent().getStringExtra(Intents.USER_PROFILE_ACTIVITY_USER_ID);

        if(mUid != null) {
            mViewingCurrentUserProfile = currentUser != null && currentUser.getUid().equals(mUid);
        } else {
            if(currentUser == null) {
                Log.w(TAG, "Current user is unexpectedly null");
                finish();
                return;
            }
            mViewingCurrentUserProfile = true;
            mUid = currentUser.getUid();
        }

        mPhotoStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseStorageUtil.STORAGE_URL)
                .child(FirebaseStorageUtil.IMAGES_PATH)
                .child(FirebaseStorageUtil.USERS_PATH)
                .child(mUid);

        mItemsAddedTextView = (TextView) findViewById(R.id.items_added_count_text_view);
        setTitle(mViewingCurrentUserProfile ? R.string.title_activity_current_user_profile : R.string.title_activity_user_profile);

        populateProfile(null);

        // Get added items for this user
        addedItemsAdapter = new AddedItemsRecyclerViewAdapter(this, mUid, this);
        mAddedItemsRecyclerView.setAdapter(addedItemsAdapter);
        mAddedItemsRecyclerView.setNestedScrollingEnabled(false);
        addedItemsAdapter.queryItems();
    }

    @Override
    protected int getCurrentNavMenuItemId() {
        return -1;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_user_profile;
    }

    private void populateProfile(final Uri newPhotoUri) {
        final TextView displayNameTextView = (TextView) findViewById(R.id.user_display_name);
        final TextView reputationTextView = (TextView) findViewById(R.id.reputation_points_text_view);
        final TextView aboutUserTextView = (TextView) findViewById(R.id.about_user_text_view);

        mDatabaseReference.child("users").child(mUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                displayNameTextView.setText(user.getDN());
                if (mViewingCurrentUserProfile) {
                    TextView emailTextView = (TextView) findViewById(R.id.user_email);
                    emailTextView.setText(getCurrentUser().getEmail());
                    emailTextView.setVisibility(View.VISIBLE);
                }

                reputationTextView.setText(String.valueOf(user.getRep()));
                String aboutUser = user.getAboutUser();
                if (StringUtils.isNotBlank(aboutUser)) {
                    aboutUserTextView.setText(aboutUser);
                }

                if(newPhotoUri != null) {
                    populatePhoto(newPhotoUri);
                } else {
                    populatePhoto();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "populateProfile:onCancelled", databaseError.toException());
            }

        });

        mAddedItemsRecyclerView = (RecyclerView) findViewById(R.id.recycler_used_added_items);
        mAddedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAddedItemsRecyclerView.addItemDecoration(new DividerItemDecoration(this));
    }

    private void populatePhoto() {
        ImageView profilePictureImageView = (ImageView) findViewById(R.id.user_profile_picture);
        Glide.with(this)
                .using(new FirebaseImageLoader())
                .load(mPhotoStorageReference)
                .fitCenter()
				.skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(profilePictureImageView);
    }

    private void populatePhoto(@NonNull Uri photoUri) {
        ImageView profilePictureImageView = (ImageView) findViewById(R.id.user_profile_picture);
        Glide.with(this)
                .load(photoUri)
                .fitCenter()
                .into(profilePictureImageView);
    }

//    private void addProfilePictureClickListener(ImageView profilePictureImageView) {
//        profilePictureImageView.setClickable(true);
//        profilePictureImageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(UserProfileActivity.this, UserImageDialog.class);
//                intent.putExtra(Intents.USER_PROFILE_PHOTO_URI, photoUri);
//                startActivity(intent);
//            }
//        });
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mViewingCurrentUserProfile) {
            menu.add(Menu.NONE, EDIT_PROFILE_MENU_ITEM_ID, 0, R.string.action_edit_profile)
                    .setIcon(R.drawable.ic_mode_edit_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == EDIT_PROFILE_MENU_ITEM_ID) {
            Intent editProfileIntent = new Intent(this, EditProfileActivity.class);
            startActivityForResult(editProfileIntent, REQUEST_EDIT_PROFILE);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_EDIT_PROFILE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri newPhotoUri = data.getParcelableExtra(Intents.EDIT_PROFILE_NEW_PHOTO_URI);
                    populateProfile(newPhotoUri);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onItemCountReady(long itemCount) {
        mItemsAddedTextView.setText(String.valueOf(itemCount));
    }

    @Override
    public void onError(Exception ex) {
        if(ex != null) {
            FirebaseCrash.report(ex);
        }
        showSnackbar(R.string.unexpected_error_try_later);
    }

    @Override
    public ItemViewHolder onCreateAddedItemViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.item_summary, parent, false);

        ItemViewHolder itemViewHolder = new ItemViewHolder(this, addedItemsAdapter, view);
        itemViewHolder.itemSummaryView = (TextView) view.findViewById(R.id.item_summary);
        itemViewHolder.itemPlaceNameView = (TextView) view.findViewById(R.id.item_place_name);
        return itemViewHolder;
    }

    @Override
    public void onBindItem(ItemViewHolder viewHolder, UserAddedItem addedItem) {
        viewHolder.itemId = addedItem.getItemId();
        viewHolder.itemSummaryView.setText(addedItem.getItemSummary());
        viewHolder.itemPlaceNameView.setText(addedItem.getPlaceName());
    }
}
