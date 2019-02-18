package com.manouti.itemfinder.model.search;

import com.google.firebase.database.IgnoreExtraProperties;


@IgnoreExtraProperties
public class SearchHit {
    private String _id;
    private double _score;
    private SearchHitSource _source;

    public SearchHit() {
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public double get_score() {
        return _score;
    }

    public void set_score(double _score) {
        this._score = _score;
    }

    public SearchHitSource get_source() {
        return _source;
    }

    public void set_source(SearchHitSource _source) {
        this._source = _source;
    }

    @IgnoreExtraProperties
    public static class SearchHitSource {
        private String summary;
        private String description;

        public SearchHitSource() {
        }

        public String getS() {
            return summary;
        }

        public void setS(String summary) {
            this.summary = summary;
        }

        public String getDesc() {
            return description;
        }

        public void setDesc(String description) {
            this.description = description;
        }
    }
}
