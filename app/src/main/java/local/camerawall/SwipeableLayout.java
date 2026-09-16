package local.camerawall;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/** Intercepts clear horizontal gestures so taps on camera tiles remain intact. */
final class SwipeableLayout extends LinearLayout {
    interface Listener {
        void onSwipe(boolean nextPage);
    }

    private final int touchSlop;
    private final Listener listener;
    private float downX;
    private float downY;
    private boolean swiping;

    SwipeableLayout(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) return swiping;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                swiping = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getX() - downX;
                float deltaY = event.getY() - downY;
                if (Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY)) {
                    swiping = true;
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return swiping;
            default:
                return swiping;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float deltaX = event.getX() - downX;
            if (swiping && listener != null) listener.onSwipe(deltaX < 0);
            else performClick();
            swiping = false;
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            swiping = false;
            return true;
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
