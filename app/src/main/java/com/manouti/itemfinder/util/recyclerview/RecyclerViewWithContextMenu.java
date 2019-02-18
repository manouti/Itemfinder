package com.manouti.itemfinder.util.recyclerview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;


public class RecyclerViewWithContextMenu extends RecyclerView {

    private RecyclerViewContextMenuInfo mContextMenuInfo;

    public RecyclerViewWithContextMenu(Context context) {
        super(context);
    }

    public RecyclerViewWithContextMenu(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerViewWithContextMenu(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getChildAdapterPosition(originalView);
        if (longPressPosition >= 0) {
            final long longPressId = getAdapter().getItemId(longPressPosition);
            mContextMenuInfo = new RecyclerViewContextMenuInfo(longPressPosition, longPressId);
            return super.showContextMenuForChild(originalView);
        }
        return false;
    }

    public static class RecyclerViewContextMenuInfo implements ContextMenu.ContextMenuInfo {

        final private int position;
        final private long id;

        public RecyclerViewContextMenuInfo(int position, long id) {
            this.position = position;
            this.id = id;
        }

        public int getPosition() {
            return position;
        }

        public long getId() {
            return id;
        }
    }
}