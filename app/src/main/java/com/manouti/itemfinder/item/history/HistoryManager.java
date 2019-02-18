package com.manouti.itemfinder.item.history;

import android.support.annotation.NonNull;

import com.google.zxing.client.android.result.ResultHandler;
import com.manouti.itemfinder.model.item.Item;

import java.util.List;


public interface HistoryManager {

    void getWeightSortedHistoryItems(CompletionListener completionListener);

    List<HistoryItem> buildHistoryItems();

    void deleteHistoryItem(String itemId, HistoryEventType eventType);

    boolean hasHistoryItems();

    void clearHistory();

    void addHistoryItem(Item item, HistoryEventType eventType, ResultHandler resultHandler);

    interface CompletionListener {
        void onSuccess(@NonNull List<HistoryItem> items);
        void onError(Throwable error);
    }
}
