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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DBHelper extends SQLiteOpenHelper {

  private static final int DB_VERSION = 6;
  private static final String DB_NAME = "itemfinder_history.db";
  public static final String TABLE_NAME = "history";
  public static final String ID_COL = "id";
  public static final String EVENT_TYPE_COL = "event_type";
  public static final String SUMMARY_COL = "summary";
  public static final String DESCRIPTION_COL = "description";
  public static final String EVENT_COUNT_COL = "count";
  public static final String LAST_TIMESTAMP_COL = "last_timestamp";
  public static final String CATEGORIES_COL = "categories";

  DBHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase sqLiteDatabase) {
    sqLiteDatabase.execSQL(
            "CREATE TABLE " + TABLE_NAME + " (" +
            ID_COL + " INTEGER PRIMARY KEY, " +
            EVENT_TYPE_COL + " TEXT, " +
            SUMMARY_COL + " TEXT, " +
            DESCRIPTION_COL + " TEXT, " +
            EVENT_COUNT_COL + " INTEGER, " +
            LAST_TIMESTAMP_COL + " INTEGER, " +
            CATEGORIES_COL + " TEXT);");
  }

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    onCreate(sqLiteDatabase);
  }

}
