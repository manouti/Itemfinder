package com.manouti.itemfinder.model.search;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.List;


@IgnoreExtraProperties
public class SearchHits {
    private List<SearchHit> hits;
    private long total;
    private boolean error;

    public SearchHits() {
    }

    public List<SearchHit> getHits() {
        return hits;
    }

    public void setHits(List<SearchHit> hits) {
        this.hits = hits;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

}
