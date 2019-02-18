package com.manouti.itemfinder.item.additem.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.additem.AddNewItemActivity;
import com.manouti.itemfinder.item.barcode.ItemBarcodeLookup;
import com.manouti.itemfinder.item.barcode.ItemBarcodeLookupFactory;
import com.manouti.itemfinder.item.history.HistoryManager;
import com.manouti.itemfinder.item.history.HistoryManagerFactory;
import com.manouti.itemfinder.item.history.HistoryItem;
import com.manouti.itemfinder.item.history.local.HistoryItemAdapter;
import com.manouti.itemfinder.model.item.ISBNItem;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.item.ItemType;
import com.manouti.itemfinder.model.item.Product;
import com.manouti.itemfinder.model.item.VINItem;
import com.manouti.itemfinder.result.ParcelableBarcodeResult;
import com.manouti.itemfinder.search.SearchableItemActivity;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.NetworkUtils;

import org.apache.commons.lang3.StringUtils;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class ItemFragment extends BaseFragment implements View.OnClickListener,
                                                        ActivityCompat.OnRequestPermissionsResultCallback,
                                                        FragmentLifecycle,
                                                        AdapterView.OnItemClickListener {

    private static final String TAG = ItemFragment.class.getSimpleName();

    private static final int REQUEST_ADD_FROM_SCAN = 1234;
    private static final int REQUEST_ADD_NEW_ITEM = 1235;

    private RelativeLayout mItemDescriptionLayout;
    private TextView mItemSummaryTextView;
    private TextView mItemDescriptionTextView;

    private ImageButton mNextButton;

    private ViewPager mViewPager;
    private HistoryManager mHistoryManager;
    private ArrayAdapter<HistoryItem> mRecentItemsAdapter;

    private DatabaseReference mDatabaseReference;

    public ItemFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated properly.
        View rootView = inflater.inflate(R.layout.fragment_item, container, false);

        configureSearchView(rootView);

        ImageButton scanButton = (ImageButton) rootView.findViewById(R.id.button_scan);
        scanButton.setOnClickListener(this);

        ItemsPagerAdapter itemsPagerAdapter = new ItemsPagerAdapter(rootView);
        mViewPager = (ViewPager) rootView.findViewById(R.id.items_view_pager);
        mViewPager.setAdapter(itemsPagerAdapter);

        TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.items_tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mHistoryManager = HistoryManagerFactory.makeHistoryManager(mAddItemPlaceActivity);
        mRecentItemsAdapter = new HistoryItemAdapter(mAddItemPlaceActivity);
        ListView recentItemsListView = (ListView) rootView.findViewById(R.id.recent_items_list_view);
        recentItemsListView.setAdapter(mRecentItemsAdapter);
        recentItemsListView.setOnItemClickListener(this);

        mNextButton = (ImageButton) rootView.findViewById(R.id.button_next);
        setNavigationButtonEnabled(false, mNextButton, R.drawable.ic_navigate_next_black_48dp);
        mNextButton.setOnClickListener(this);

        mItemDescriptionLayout = (RelativeLayout) rootView.findViewById(R.id.item_description_layout);
        mItemSummaryTextView = (TextView) rootView.findViewById(R.id.item_summary_text_view);
        mItemDescriptionTextView = (TextView) rootView.findViewById(R.id.item_description_text_view);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        return rootView;
    }

    private void configureSearchView(View rootView) {
        SearchView searchView = (SearchView) rootView.findViewById(R.id.item_search_view);
        int magId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
        ImageView magImage = (ImageView) searchView.findViewById(magId);
        if(magImage != null) {
            magImage.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        }
        int searchPlateId = getResources().getIdentifier("android:id/search_plate", null, null);
        View searchPlateView = searchView.findViewById(searchPlateId);
        if (searchPlateView != null) {
            searchPlateView.setBackgroundColor(ContextCompat.getColor(mAddItemPlaceActivity, R.color.colorMainBackground));
        }
        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(getContext(), SearchableItemActivity.class)));
        searchView.setQueryRefinementEnabled(true);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (StringUtils.isNotBlank(newText)) {
                    int currentPage = mViewPager.getCurrentItem();
                    if (currentPage == 0) {
                        mRecentItemsAdapter.getFilter().filter(newText);
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().requestFocus();

        reloadHistoryItems();
        if(!NetworkUtils.isNetworkAvailable(mAddItemPlaceActivity)) {
            mAddItemPlaceActivity.showSnackbar(R.string.network_connection_required);
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        Item item = mAddItemPlaceActivity.getItem();
        if(item != null) {
            mItemDescriptionLayout.setVisibility(View.VISIBLE);
            mItemSummaryTextView.setText(item.getS());
            mItemDescriptionTextView.setText(item.getDesc());
        }
    }

    private void reloadHistoryItems() {
        Iterable<HistoryItem> items = mHistoryManager.buildHistoryItems();
        mRecentItemsAdapter.clear();
        for (HistoryItem item : items) {
            mRecentItemsAdapter.add(item);
        }
        if (mRecentItemsAdapter.isEmpty()) {
            mRecentItemsAdapter.add(new HistoryItem(null, null, null, null, -1, -1, null));
        }
    }

    public void startCapture() {
        Intent captureIntent = new Intent(getActivity(), CaptureActivity.class);
        captureIntent.putExtra(Intents.RETURN_CAPTURED_ITEM_TO_ACTIVITY, true);
        startActivityForResult(captureIntent, REQUEST_ADD_FROM_SCAN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ADD_FROM_SCAN:
                if (resultCode == Activity.RESULT_OK) {
                    Item item = data.getParcelableExtra(Intents.RETURNED_ITEM_INFO_FROM_SCAN);
                    if(item != null) {
                        handleItem(item);
                    } else {
                        // Item is not in the database, query external sources (e.g. Google Books) using the scanned barcode, but first validate it.
                        mAddItemPlaceActivity.showLoadingDialog(com.firebase.ui.auth.R.string.progress_dialog_loading);
                        final ParcelableBarcodeResult parcelableBarcodeResult = data.getParcelableExtra(Intents.SCANNED_ITEM_PARCELABLE_BARCODE);
                        Result rawResult = parcelableBarcodeResult.getRawResult();
                        final ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(getActivity(), rawResult);
                        ParsedResult parsedResult = resultHandler.getResult();
                        ParsedResultType resultType = parsedResult.getType();
                        if(resultType != ParsedResultType.PRODUCT
                                && resultType != ParsedResultType.ISBN
                                && resultType != ParsedResultType.VIN) {
                            mAddItemPlaceActivity.dismissDialog();
                            DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.unsupported_item_type), null);
                        } else {
                            lookupItemBarcode(resultHandler, resultType, new ItemBarcodeLookup.BarcodeLookupCompletionListener() {
                                @Override
                                public void onLookupSuccess(Item item) {
                                    mAddItemPlaceActivity.dismissDialog();
                                    if (item != null) {
                                        handleItem(item);
                                    } else {
                                        // Item not found, allow user to add it.
                                        Intent addItemIntent = new Intent(getActivity(), AddNewItemActivity.class);
                                        addItemIntent.putExtra(Intents.NEW_ITEM_PARCELABLE_BARCODE, parcelableBarcodeResult);
                                        startActivityForResult(addItemIntent, REQUEST_ADD_NEW_ITEM);
                                    }
                                }

                                @Override
                                public void onLookupError(Throwable error) {
                                    Log.e(TAG, "error when looking up barcode for " + resultHandler.getDisplayContents());
                                    FirebaseCrash.report(error);
                                    mAddItemPlaceActivity.dismissDialog();
                                    mAddItemPlaceActivity.showSnackbar(R.string.barcode_lookup_error);
                                }
                            });
                        }
                    }
                }
                break;
            case REQUEST_ADD_NEW_ITEM:
                if(resultCode == Activity.RESULT_OK) {
                    Item newItem = data.getParcelableExtra(Intents.NEW_ITEM_RETURNED_INFO);
                    mAddItemPlaceActivity.updateItem(newItem);
                    mItemDescriptionLayout.setVisibility(View.VISIBLE);
                    mItemSummaryTextView.setText(newItem.getS());
                    mItemDescriptionTextView.setText(newItem.getDesc());

                    setNavigationButtonEnabled(true, mNextButton, R.drawable.ic_navigate_next_black_48dp);
                    scrollNext();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void lookupItemBarcode(ResultHandler resultHandler, ParsedResultType resultType, ItemBarcodeLookup.BarcodeLookupCompletionListener completionListener) {
        ItemBarcodeLookup itemBarcodeLookup = ItemBarcodeLookupFactory.getItemBarcodeLookup(resultType);
        if(itemBarcodeLookup != null) {
            String itemId = resultHandler.getDisplayContents();
            itemBarcodeLookup.lookupItem(itemId, completionListener);
        }
    }

    private void handleItem(Item item) {
        mAddItemPlaceActivity.updateItem(item);
        mItemDescriptionLayout.setVisibility(View.VISIBLE);
        mItemSummaryTextView.setText(item.getS());
        mItemDescriptionTextView.setText(item.getDesc());

        setNavigationButtonEnabled(true, mNextButton, R.drawable.ic_navigate_next_black_48dp);
        scrollNext();
    }

    private void handleItem(final String itemId) {
        final SweetAlertDialog progressDialog = DialogUtils.showProgressDialog(mAddItemPlaceActivity, getString(R.string.progress_loading_item), null);

        if (!NetworkUtils.isNetworkAvailable(mAddItemPlaceActivity)) {
            progressDialog.dismiss();
            DialogUtils.showErrorDialog(mAddItemPlaceActivity, getString(R.string.network_connection_required),
                    getString(R.string.add_item_connection_explanation));
            return;
        }

        mDatabaseReference.child("items").child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Item item;
                    ItemType type = ItemType.valueOf(dataSnapshot.child("type").getValue(String.class));
                    switch (type) {
                        case PRODUCT:
                            item = dataSnapshot.getValue(Product.class);
                            break;
                        case ISBN:
                            item = dataSnapshot.getValue(ISBNItem.class);
                            break;
                        case VIN:
                            item = dataSnapshot.getValue(VINItem.class);
                            break;
                        default:
                            item = dataSnapshot.getValue(Item.class);
                    }
                    item.setId(itemId);

                    mAddItemPlaceActivity.updateItem(item);
                    mItemDescriptionLayout.setVisibility(View.VISIBLE);
                    mItemSummaryTextView.setText(item.getS());
                    mItemDescriptionTextView.setText(item.getDesc());

                    setNavigationButtonEnabled(true, mNextButton, R.drawable.ic_navigate_next_black_48dp);
                    scrollNext();
                } else {
                    // TODO this case should not normally occur because we don't delete items from the database
                    // Item is not in the database, allow user to add it.
                    Intent addItemIntent = new Intent(getActivity(), AddNewItemActivity.class);
                    addItemIntent.putExtra(Intents.NEW_ITEM_ID_INPUT, itemId);
                    startActivityForResult(addItemIntent, REQUEST_ADD_NEW_ITEM);
                }

                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getItem:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_scan:
                startCapture();
                break;
            case R.id.button_next:
                scrollNext();
                break;
            default:
                // do nothing
        }
    }

    @Override
    public void clearInput() {
        mItemSummaryTextView.setText("");
        mItemDescriptionTextView.setText("");
        mItemDescriptionLayout.setVisibility(View.GONE);

        setNavigationButtonEnabled(false, mNextButton, R.drawable.ic_navigate_next_black_48dp);
        mAddItemPlaceActivity.disableNextTabs();
        mAddItemPlaceActivity.updateItem(null);
    }

    @Override
    public void onPausePagerFragment() {
    }

    @Override
    public void onResumePagerFragment() {
        if(!NetworkUtils.isNetworkAvailable(mAddItemPlaceActivity)) {
            mAddItemPlaceActivity.showSnackbar(R.string.network_connection_required);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String itemId = mRecentItemsAdapter.getItem(position).getItemId();
        if (itemId != null) {
            handleItem(itemId);
        }
    }

    private class ItemsPagerAdapter extends PagerAdapter {

        private View fragmentRootView;

        public ItemsPagerAdapter(View fragmentRootView) {
            this.fragmentRootView = fragmentRootView;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            int resId = 0;
            switch (position) {
                case 0:
                    resId = R.id.recent_items_layout;
                    break;
                case 1:
                    resId = R.id.favorite_items_layout;
                    break;
            }
            View view = fragmentRootView.findViewById(resId);
            container.addView(view, 0);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0:
                    return getResources().getString(R.string.tab_recent_items);
                case 1:
                    return getResources().getString(R.string.tab_favorite_items);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
