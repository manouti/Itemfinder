package com.manouti.itemfinder.model.search;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;


public class SearchRequest {
    private String index;
    private String type;
    private String q;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("index", index);
        result.put("type", type);
        result.put("q", q);

        return result;
    }
}
