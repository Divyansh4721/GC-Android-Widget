package com.gcjewellers.rateswidget;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class SwipeGestureDetector implements View.OnTouchListener {
    private static final String TAG = "SwipeGestureDetector";
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public static final int SWIPE_UP = 1;
    public static final int SWIPE_DOWN = 2;
    public static final int SWIPE_LEFT = 3;
    public static final int SWIPE_RIGHT = 4;

    private final GestureDetector gestureDetector;
    private final SwipeListener swipeListener;

    public interface SwipeListener {
        boolean onSwipe(int direction);
    }

    public SwipeGestureDetector(Context context, SwipeListener listener) {
        this.swipeListener = listener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    if (e1 == null || e2 == null) {
                        return false;
                    }

                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();

                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        // Horizontal swipe
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                return swipeListener.onSwipe(SWIPE_RIGHT);
                            } else {
                                return swipeListener.onSwipe(SWIPE_LEFT);
                            }
                        }
                    } else {
                        // Vertical swipe
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffY > 0) {
                                return swipeListener.onSwipe(SWIPE_DOWN);
                            } else {
                                return swipeListener.onSwipe(SWIPE_UP);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing swipe gesture", e);
                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
}