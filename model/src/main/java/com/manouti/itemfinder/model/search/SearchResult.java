package com.manouti.itemfinder.model.search;

import com.google.firebase.database.IgnoreExtraProperties;


@IgnoreExtraProperties
public class SearchResult {
    private long took;
    private boolean timed_out;
    private SearchHits searchHits;

    public SearchResult() {
    }

    public long getTook() {
        return took;
    }

    public void setTook(long took) {
        this.took = took;
    }

    public boolean isTimed_out() {
        return timed_out;
    }

    public void setTimed_out(boolean timed_out) {
        this.timed_out = timed_out;
    }

    public SearchHits getHits() {
        return searchHits;
    }

    public void setHits(SearchHits searchHits) {
        this.searchHits = searchHits;
    }
}
