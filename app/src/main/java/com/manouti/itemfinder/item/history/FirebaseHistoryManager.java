package com.manouti.itemfinder.item.history;

import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.client.android.result.ResultHandler;
import com.manouti.itemfinder.model.item.Item;

import java.util.List;


public class FirebaseHistoryManager implements HistoryManager {

    private static final String TAG = FirebaseHistoryManager.class.getSimpleName();

    private static final String VIEWED_ITEMS_NODE = "viewed";
    private static final String SEARCHED_ITEMS_NODE = "searched";
    private static final String ADDED_ITEMS_NODE = "added";
    private static final String SCANNED_ITEMS_NODE = "scanned";
    private static final String LOOKED_UP_NEARBY_ITEMS_NODE = "looked_nearby";

    private DatabaseReference usageReference;

    public FirebaseHistoryManager(String userId) {
        usageReference = FirebaseDatabase.getInstance().getReference("usage").child(userId);
    }

    @Override
    public void getWeightSortedHistoryItems(CompletionListener completionListener) {
        usageReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> viewedItems = dataSnapshot.child(VIEWED_ITEMS_NODE).getChildren();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                DatabaseException throwable = databaseError.toException();
                Log.w(TAG, "getWeightSortedHistoryItems:onCancelled", throwable);
                FirebaseCrash.report(throwable);
            }
        });
    }

    @Override
    public List<HistoryItem> buildHistoryItems() {
        return null;
    }

    @Override
    public void deleteHistoryItem(String itemId, HistoryEventType eventType) {

    }

    @Override
    public boolean hasHistoryItems() {
        return false;
    }

    @Override
    public void clearHistory() {

    }

    @Override
    public void addHistoryItem(Item item, HistoryEventType eventType, ResultHandler resultHandler) {

    }

}
