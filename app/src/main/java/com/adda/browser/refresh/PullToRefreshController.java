package com.adda.browser.refresh;

import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

/** Shared pull-down refresh gesture for every browser tab. */
public final class PullToRefreshController {
    private final int triggerDistancePx;

    public PullToRefreshController(int triggerDistancePx) {
        if (triggerDistancePx <= 0) throw new IllegalArgumentException("triggerDistancePx must be positive");
        this.triggerDistancePx = triggerDistancePx;
    }

    public View.OnTouchListener createListener(WebView target) {
        final float[] startY = new float[1];
        return (view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startY[0] = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                float distance = event.getY() - startY[0];
                if (target.getScrollY() == 0 && distance > triggerDistancePx) {
                    target.reload();
                }
            }
            return false;
        };
    }
}