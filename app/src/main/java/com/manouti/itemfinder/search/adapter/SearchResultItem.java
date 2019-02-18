package com.manouti.itemfinder.search.adapter;


public class SearchResultItem {

    private String itemId;
    private String summary;
    private String description;

    public SearchResultItem(String itemId, String summary, String description) {
        this.itemId = itemId;
        this.summary = summary;
        this.description = description;
    }

    public String getItemId() {
        return itemId;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

}
