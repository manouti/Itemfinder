/*
 * Copyright (C) 2009 ZXing authors
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

package com.manouti.itemfinder.item.history.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.zxing.client.android.result.ResultHandler;
import com.manouti.itemfinder.item.history.HistoryEventType;
import com.manouti.itemfinder.item.history.HistoryItem;
import com.manouti.itemfinder.item.history.HistoryManager;
import com.manouti.itemfinder.model.item.Item;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Saves usage history locally.</p>
 *
 */
public final class DBHistoryManager implements HistoryManager {

    private static final String TAG = DBHistoryManager.class.getSimpleName();

    private static final int MAX_ITEMS = 2000;

    private static final String[] COLUMNS = {
            DBHelper.ID_COL,
            DBHelper.EVENT_TYPE_COL,
            DBHelper.SUMMARY_COL,
            DBHelper.DESCRIPTION_COL,
            DBHelper.EVENT_COUNT_COL,
            DBHelper.LAST_TIMESTAMP_COL,
            DBHelper.CATEGORIES_COL,
    };

    private static final String[] COUNT_COLUMN = { "COUNT(1)" };

    private static final String[] ID_COL_PROJECTION = { DBHelper.ID_COL };

    private final Context context;

    public DBHistoryManager(Context context) {
        this.context = context;
        trimHistory();
    }

    public boolean hasHistoryItems() {
        SQLiteOpenHelper helper = new DBHelper(context);
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = helper.getReadableDatabase();
            cursor = db.query(DBHelper.TABLE_NAME, COUNT_COLUMN, null, null, null, null, null);
            cursor.moveToFirst();
            return cursor.getInt(0) > 0;
        } finally {
            close(cursor, db);
        }
    }

    public List<HistoryItem> buildHistoryItems() {
        SQLiteOpenHelper helper = new DBHelper(context);
        List<HistoryItem> items = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = helper.getReadableDatabase();
            cursor = db.query(DBHelper.TABLE_NAME, COLUMNS, null, null, null, null, DBHelper.LAST_TIMESTAMP_COL + " DESC");
            while (cursor.moveToNext()) {
                String itemId = cursor.getString(0);
                HistoryEventType eventType = HistoryEventType.valueOf(cursor.getString(1));
                String itemSummary = cursor.getString(2);
                String itemDescription = cursor.getString(3);
                int eventCount = cursor.getInt(4);
                long lastTimestamp = cursor.getLong(5);
                List<String> categories = null;
                String categoriesString = cursor.getString(6);
                if(StringUtils.isNotBlank(categoriesString)) {
                    categories = Arrays.asList(StringUtils.split(categoriesString, ','));
                }
                items.add(new HistoryItem(itemId, eventType, itemSummary, itemDescription, eventCount, lastTimestamp, categories));
            }
        } finally {
            close(cursor, db);
        }
        return items;
    }

    public void deleteHistoryItem(String itemId, HistoryEventType eventType) {
        SQLiteOpenHelper helper = new DBHelper(context);
        SQLiteDatabase db = null;

        Cursor cursor = null;
        try {
            db = helper.getWritableDatabase();
            cursor = db.query(DBHelper.TABLE_NAME, ID_COL_PROJECTION,
                    DBHelper.ID_COL + "=? and " + DBHelper.EVENT_TYPE_COL + "=?",
                    new String[] { itemId, eventType.name() },
                    null, null, null);
            if(cursor.moveToFirst()) {
                db.delete(DBHelper.TABLE_NAME, DBHelper.ID_COL + '=' + cursor.getString(0), null);
            }
        } finally {
            close(cursor, db);
        }
    }

    public void addHistoryItem(Item item, HistoryEventType eventType, ResultHandler handler) {
        // Do not save this item to the history if the contents are
        // considered secure.
        if (handler.areContentsSecure()) {
            return;
        }

        // Remove previous entry so that we don't duplicate.
        int oldEventCount = deletePrevious(item.getId(), eventType);

        ContentValues values = new ContentValues();
        values.put(DBHelper.ID_COL, item.getId());
        values.put(DBHelper.EVENT_TYPE_COL, eventType.name());
        values.put(DBHelper.SUMMARY_COL, item.getS());
        values.put(DBHelper.DESCRIPTION_COL, item.getDesc());
        values.put(DBHelper.EVENT_COUNT_COL, oldEventCount + 1);
        values.put(DBHelper.LAST_TIMESTAMP_COL, System.currentTimeMillis());
        if(item.getCategories() != null) {
            values.put(DBHelper.CATEGORIES_COL, StringUtils.join(item.getCategories(), ','));
        }

        SQLiteOpenHelper helper = new DBHelper(context);
        SQLiteDatabase db = null;
        try {
            db = helper.getWritableDatabase();
            // Insert the new entry into the DB.
            db.insert(DBHelper.TABLE_NAME, DBHelper.LAST_TIMESTAMP_COL, values);
        } finally {
            close(null, db);
        }
    }

    private int deletePrevious(String itemId, HistoryEventType eventType) {
        int oldEventCount = 0;

        SQLiteOpenHelper helper = new DBHelper(context);
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = helper.getWritableDatabase();
            cursor = db.query(DBHelper.TABLE_NAME, new String[]{ DBHelper.EVENT_COUNT_COL },
                    DBHelper.ID_COL + "=? and " + DBHelper.EVENT_TYPE_COL + "=?",
                    new String[] { itemId, eventType.name() },
                    null, null, null);
            if(cursor.moveToFirst()) {
                oldEventCount = cursor.getInt(0);
            }
        } finally {
            close(cursor, null);
        }
        try {
            db.delete(DBHelper.TABLE_NAME, DBHelper.ID_COL + "=? and " + DBHelper.EVENT_TYPE_COL + "=?",
                    new String[]{itemId, eventType.name()});
        } finally {
            close(null, db);
        }

        return oldEventCount;
    }

    public void trimHistory() {
        SQLiteOpenHelper helper = new DBHelper(context);
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = helper.getWritableDatabase();
            cursor = db.query(DBHelper.TABLE_NAME,
                    ID_COL_PROJECTION,
                    null, null, null, null,
                    DBHelper.LAST_TIMESTAMP_COL + " DESC");
            cursor.move(MAX_ITEMS);
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                Log.i(TAG, "Deleting usage history ID " + id);
                db.delete(DBHelper.TABLE_NAME, DBHelper.ID_COL + '=' + id, null);
            }
        } catch (SQLiteException sqle) {
            // We're seeing an error here when called in CaptureActivity.onCreate() in rare cases
            // and don't understand it. First theory is that it's transient so can be safely ignored.
            Log.w(TAG, sqle);
            // continue
        } finally {
            close(cursor, db);
        }
    }

    @Override
    public void getWeightSortedHistoryItems(CompletionListener completionListener) {

    }

    public void clearHistory() {
        SQLiteOpenHelper helper = new DBHelper(context);
        SQLiteDatabase db = null;
        try {
            db = helper.getWritableDatabase();
            db.delete(DBHelper.TABLE_NAME, null, null);
        } finally {
            close(null, db);
        }
    }

    private static void close(Cursor cursor, SQLiteDatabase database) {
        if (cursor != null) {
            cursor.close();
        }
        if (database != null) {
            database.close();
        }
    }

}
