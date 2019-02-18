/*
 * Copyright 2012 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.manouti.itemfinder.item.history;

import java.util.List;

public final class HistoryItem {

    private final String itemId;
    private final HistoryEventType eventType;
    private final String itemSummary;
    private final String itemDescription;
    private final int eventCount;
    private final long lastTimestamp;
    private final List<String> itemCategories;

    private int weight;

    public HistoryItem(String itemId, HistoryEventType eventType, String itemSummary, String itemDescription, int eventCount, long lastTimestamp, List<String> itemCategories) {
        this.itemId = itemId;
        this.eventType = eventType;
        this.itemSummary = itemSummary;
        this.itemDescription = itemDescription;
        this.eventCount = eventCount;
        this.lastTimestamp = lastTimestamp;
        this.itemCategories = itemCategories;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemSummary() {
        return itemSummary;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public HistoryEventType getEventType() {
        return eventType;
    }

    public int getEventCount() {
        return eventCount;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public List<String> getCategories() {
        return itemCategories;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

}
