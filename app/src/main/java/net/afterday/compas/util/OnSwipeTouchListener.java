package net.afterday.compas.util;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public class OnSwipeTouchListener implements View.OnTouchListener {
    private final GestureDetector gestureDetector;

    public OnSwipeTouchListener(Context context) {
        this.gestureDetector = new GestureDetector(context, new GestureListener(this, null));
    }

    public void onSwipeLeft() {
    }

    public void onSwipeRight() {
    }

    public void onSwipeUp() {
    }

    public void onSwipeDown() {
    }

    @Override // android.view.View.OnTouchListener
    public boolean onTouch(View v, MotionEvent event) {
        return this.gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_DISTANCE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        private GestureListener() {
        }

        /* synthetic */ GestureListener(OnSwipeTouchListener x0, Object x1) {
            this();
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = e2.getX() - e1.getX();
            float distanceY = e2.getY() - e1.getY();
            if (java.lang.Math.abs(distanceX) > java.lang.Math.abs(distanceY)) {
                if (java.lang.Math.abs(distanceX) <= 100.0f || java.lang.Math.abs(velocityX) <= 100.0f) {
                    return false;
                }
                if (distanceX > 0.0f) {
                    OnSwipeTouchListener.this.onSwipeRight();
                } else {
                    OnSwipeTouchListener.this.onSwipeLeft();
                }
                return true;
            }
            if (java.lang.Math.abs(distanceY) <= 100.0f || java.lang.Math.abs(velocityY) <= 100.0f) {
                return false;
            }
            if (distanceY > 0.0f) {
                OnSwipeTouchListener.this.onSwipeDown();
            } else {
                OnSwipeTouchListener.this.onSwipeUp();
            }
            return true;
        }
    }
}
