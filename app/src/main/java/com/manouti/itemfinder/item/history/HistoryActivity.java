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

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.manouti.itemfinder.BaseNavigationActivity;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.detail.ItemDetailActivity;
import com.manouti.itemfinder.item.history.local.HistoryItemAdapter;
import com.manouti.itemfinder.util.DialogUtils;

public final class HistoryActivity extends BaseNavigationActivity implements AdapterView.OnItemClickListener {

  private static final String TAG = HistoryActivity.class.getSimpleName();

  private HistoryManager historyManager;
  private ArrayAdapter<HistoryItem> adapter;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    this.historyManager = HistoryManagerFactory.makeHistoryManager(this);
    adapter = new HistoryItemAdapter(this);
    ListView listView = (ListView) findViewById(android.R.id.list);
    listView.setAdapter(adapter);
    registerForContextMenu(listView);
    listView.setOnItemClickListener(this);
  }

  @Override
  protected int getCurrentNavMenuItemId() {
    return R.id.nav_history;
  }

  @Override
  protected int getLayoutResourceId() {
    return R.layout.activity_history;
  }

  @Override
  protected void onResume() {
    super.onResume();
    reloadHistoryItems();
  }

  private void reloadHistoryItems() {
    Iterable<HistoryItem> items = historyManager.buildHistoryItems();
    adapter.clear();
    for (HistoryItem item : items) {
      adapter.add(item);
    }
    if (adapter.isEmpty()) {
      adapter.add(new HistoryItem(null, null, null, null, -1, -1, null));
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu,
                                  View v,
                                  ContextMenu.ContextMenuInfo menuInfo) {
    int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
    if (position >= adapter.getCount() || adapter.getItem(position).getItemId() != null) {
      menu.add(Menu.NONE, position, position, R.string.history_clear_one_history_text);
    } // else it's just that dummy "Empty" message
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    int position = item.getItemId();
    HistoryItem historyItem = adapter.getItem(position);
    historyManager.deleteHistoryItem(historyItem.getItemId(), historyItem.getEventType());
    reloadHistoryItems();
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (historyManager.hasHistoryItems()) {
      MenuInflater menuInflater = getMenuInflater();
      menuInflater.inflate(R.menu.history, menu);
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_history_clear_text:
        DialogUtils.runAfterConfirm(this, new DialogUtils.OnConfirmOperation() {
          @Override
          public void runOperation() {
            historyManager.clearHistory();
          }
        });
        break;
      default:
        return super.onOptionsItemSelected(item);
    }
    return true;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    String itemId = adapter.getItem(position).getItemId();
    if (itemId != null) {
        Intent itemDetailIntent = new Intent(this, ItemDetailActivity.class);
        itemDetailIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_ID_INPUT, itemId);
        startActivity(itemDetailIntent);
    }
  }
}
