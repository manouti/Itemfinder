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

package com.manouti.itemfinder.item.history.local;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.zxing.client.android.R;
import com.manouti.itemfinder.item.history.HistoryItem;

import java.util.ArrayList;

public final class HistoryItemAdapter extends ArrayAdapter<HistoryItem> {

  private final Context activity;

  public HistoryItemAdapter(Context activity) {
    super(activity, R.layout.history_list_item, new ArrayList<HistoryItem>());
    this.activity = activity;
  }

  @Override
  public View getView(int position, View view, ViewGroup viewGroup) {
    View layout;
    if (view instanceof LinearLayout) {
      layout = view;
    } else {
      LayoutInflater factory = LayoutInflater.from(activity);
      layout = factory.inflate(R.layout.history_list_item, viewGroup, false);
    }

    HistoryItem item = getItem(position);
    String itemId = item.getItemId();

    CharSequence title;
    CharSequence detail;
    if (itemId != null) {
      title = item.getItemSummary();
      detail = item.getItemDescription();
    } else {
      Resources resources = getContext().getResources();
      title = resources.getString(R.string.history_empty);
      detail = resources.getString(R.string.history_empty_detail);
    }

    ((TextView) layout.findViewById(R.id.history_title)).setText(title);    
    ((TextView) layout.findViewById(R.id.history_detail)).setText(detail);

    return layout;
  }

}
