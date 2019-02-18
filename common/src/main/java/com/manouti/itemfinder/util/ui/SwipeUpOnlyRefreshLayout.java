package com.manouti.itemfinder.util.ui;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class SwipeUpOnlyRefreshLayout extends SwipeRefreshLayout {

    private float initialY;
    private boolean scrolledDown;

    public SwipeUpOnlyRefreshLayout(Context context) {
        this(context, null);
    }

    public SwipeUpOnlyRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        switch(action) {
            case MotionEvent.ACTION_DOWN:
                initialY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float y2 = ev.getY();
                float dy = y2 - initialY;
                if(dy < 0) {
                    scrolledDown = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                scrolledDown = false;
                break;
        }
        return !scrolledDown && super.onInterceptTouchEvent(ev);
    }
}
