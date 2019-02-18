package com.manouti.itemfinder.search;

import android.content.SearchRecentSuggestionsProvider;


public class ItemSuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = ItemSuggestionProvider.class.getName();
    public final static int MODE = DATABASE_MODE_QUERIES;

    public ItemSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}