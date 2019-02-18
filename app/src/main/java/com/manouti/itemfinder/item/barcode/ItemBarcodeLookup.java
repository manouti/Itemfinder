package com.manouti.itemfinder.item.barcode;

import com.manouti.itemfinder.model.item.Item;


public interface ItemBarcodeLookup {

    void lookupItem(String barcode, BarcodeLookupCompletionListener completionListener);

    interface BarcodeLookupCompletionListener {
        void onLookupSuccess(Item item);
        void onLookupError(Throwable error);
    }
}
