package com.manouti.itemfinder.item.history;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.manouti.itemfinder.item.history.local.DBHistoryManager;


public final class HistoryManagerFactory {

    private HistoryManagerFactory() {
    }

    public static HistoryManager makeHistoryManager(Context context) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null) {
            return new FirebaseHistoryManager(currentUser.getUid());
        } else {
            return new DBHistoryManager(context);
        }
    }

}
