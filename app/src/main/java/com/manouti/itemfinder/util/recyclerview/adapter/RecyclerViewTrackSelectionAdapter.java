package com.manouti.itemfinder.util.recyclerview.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class RecyclerViewTrackSelectionAdapter<VH extends RecyclerViewTrackSelectionAdapter.ViewHolder> extends RecyclerView.Adapter<VH> {
    // Start with first item selected
    private int focusedItem = 0;
    private RecyclerView mRecyclerView;

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        // Set selected state; use a state list drawable to style the view
        viewHolder.itemView.setSelected(focusedItem == position);
    }

    public abstract void clear();

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private RecyclerViewTrackSelectionAdapter<? extends ViewHolder> mAdapter;
        protected Context mContext;

        public ViewHolder(Context context, RecyclerViewTrackSelectionAdapter<? extends ViewHolder> adapter, View itemView) {
            super(itemView);
            this.mContext = context;
            this.mAdapter = adapter;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // Handle item click and set the selection -- redraw the old selection and the new
            mAdapter.notifyItemChanged(mAdapter.focusedItem);
            mAdapter.focusedItem = mAdapter.mRecyclerView.getChildAdapterPosition(v);
            mAdapter.notifyItemChanged(mAdapter.focusedItem);
        }

        @Override
        public boolean onLongClick(View v) {
            return true;
        }
    }
}