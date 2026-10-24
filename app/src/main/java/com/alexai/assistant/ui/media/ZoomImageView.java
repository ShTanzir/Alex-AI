package com.alexai.assistant.ui.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/** Image view with pinch zoom, drag, double-tap zoom, and zoom in / out / fit controls. */
public class ZoomImageView extends AppCompatImageView {
    private static final float MAX_FACTOR = 6f;

    private final Matrix matrix = new Matrix();
    private final float[] values = new float[9];
    private final RectF rect = new RectF();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private float fitScale = 1f;
    private float lastX, lastY;

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector d) {
                zoomBy(d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(MotionEvent e) {
                if (currentScale() > fitScale * 1.05f) resetZoom();
                else zoomBy(2.5f, e.getX(), e.getY());
                return true;
            }
        });
    }

    @Override public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        resetZoom();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetZoom();
    }

    public void zoomIn() { zoomBy(1.5f, getWidth() / 2f, getHeight() / 2f); }

    public void zoomOut() { zoomBy(1f / 1.5f, getWidth() / 2f, getHeight() / 2f); }

    /** Fits the whole image on screen. */
    public void resetZoom() {
        Drawable d = getDrawable();
        if (d == null || getWidth() == 0 || getHeight() == 0) return;
        float dw = d.getIntrinsicWidth(), dh = d.getIntrinsicHeight();
        if (dw <= 0 || dh <= 0) return;
        fitScale = Math.min(getWidth() / dw, getHeight() / dh);
        matrix.reset();
        matrix.postScale(fitScale, fitScale);
        matrix.postTranslate((getWidth() - dw * fitScale) / 2f, (getHeight() - dh * fitScale) / 2f);
        setImageMatrix(matrix);
    }

    private float currentScale() {
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private void zoomBy(float factor, float px, float py) {
        if (getDrawable() == null) return;
        float target = currentScale() * factor;
        float min = fitScale, max = fitScale * MAX_FACTOR;
        if (target < min) factor = min / currentScale();
        else if (target > max) factor = max / currentScale();
        matrix.postScale(factor, factor, px, py);
        fixTranslation();
        setImageMatrix(matrix);
    }

    private void fixTranslation() {
        Drawable d = getDrawable();
        if (d == null) return;
        rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(rect);
        float dx = 0, dy = 0;
        if (rect.width() <= getWidth()) dx = (getWidth() - rect.width()) / 2f - rect.left;
        else if (rect.left > 0) dx = -rect.left;
        else if (rect.right < getWidth()) dx = getWidth() - rect.right;
        if (rect.height() <= getHeight()) dy = (getHeight() - rect.height()) / 2f - rect.top;
        else if (rect.top > 0) dy = -rect.top;
        else if (rect.bottom < getHeight()) dy = getHeight() - rect.bottom;
        matrix.postTranslate(dx, dy);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        gestureDetector.onTouchEvent(e);
        if (!scaleDetector.isInProgress() && e.getPointerCount() == 1) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = e.getX();
                    lastY = e.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    matrix.postTranslate(e.getX() - lastX, e.getY() - lastY);
                    lastX = e.getX();
                    lastY = e.getY();
                    fixTranslation();
                    setImageMatrix(matrix);
                    break;
                default:
                    break;
            }
        } else if (e.getPointerCount() > 1) {
            lastX = e.getX();
            lastY = e.getY();
        }
        return true;
    }
}
