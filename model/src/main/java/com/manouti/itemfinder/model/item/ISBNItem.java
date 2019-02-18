package com.manouti.itemfinder.model.item;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;
import com.google.zxing.client.result.ISBNParsedResult;

import java.util.List;
import java.util.Map;


@IgnoreExtraProperties
public class ISBNItem extends Item {

    private List<String> authors;
    private String publisher;
    private String language;
    private String country;
    private String listPrice;

    public ISBNItem() {
        mType = ItemType.ISBN;
    }

    public ISBNItem(ISBNParsedResult result, long timestamp, double rating, int voteCount) {
        this(result.getISBN(), timestamp, rating, voteCount);
    }

    public ISBNItem(String isbn, long timestamp, double rating, int voteCount) {
        super(isbn, timestamp, ItemType.ISBN, rating, voteCount);
    }

    public String getType() {
        // Convert enum to string
        if (mType == null) {
            return null;
        } else {
            return mType.toString();
        }
    }

    public void setType(String type) {
        // Get enum from string
        if (type == null) {
            this.mType = null;
        } else {
            this.mType = ItemType.valueOf(type);
        }
    }

    @PropertyName("a")
    public List<String> getAuthors() {
        return authors;
    }

    @PropertyName("a")
    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    @PropertyName("p")
    public String getPublisher() {
        return publisher;
    }

    @PropertyName("p")
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @PropertyName("l")
    public String getLanguage() {
        return language;
    }

    @PropertyName("l")
    public void setLanguage(String language) {
        this.language = language;
    }

    @PropertyName("c")
    public String getCountry() {
        return country;
    }

    @PropertyName("c")
    public void setCountry(String country) {
        this.country = country;
    }

    @PropertyName("lp")
    public String getListPrice() {
        return listPrice;
    }

    @PropertyName("lp")
    public void setListPrice(String listPrice) {
        this.listPrice = listPrice;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = super.toMap();
        result.put("a", authors);
        result.put("p", publisher);
        result.put("l", language);
        result.put("c", country);
        result.put("lp", listPrice);

        return result;
    }

}
