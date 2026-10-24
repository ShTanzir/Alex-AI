package com.alexai.assistant.ui.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.ContextCompat;

import com.alexai.assistant.R;

/** The animated assistant button. Animates only while active; stops when idle or detached. */
public class AlexFab extends View {
    public enum State { IDLE, LISTENING, THINKING, SPEAKING, EXECUTING, ERROR }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Drawable mic;
    private ValueAnimator animator;
    private float phase;
    private State state = State.IDLE;

    public AlexFab(Context context, AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(3 * d);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(4 * d);
        arc.setStrokeCap(Paint.Cap.ROUND);
        mic = ContextCompat.getDrawable(context, R.drawable.ic_mic).mutate();
        mic.setTint(Color.WHITE);
        setClickable(true);
        setFocusable(true);
        setContentDescription("Talk to Alex");
    }

    public State getState() { return state; }

    public void setState(State s) {
        if (s == state) return;
        state = s;
        setContentDescription(describe(s));
        updateAnimation();
        invalidate();
    }

    private static String describe(State s) {
        switch (s) {
            case LISTENING: return "Alex is listening. Tap to stop.";
            case THINKING: return "Alex is thinking. Tap to cancel.";
            case SPEAKING: return "Alex is speaking. Tap to stop.";
            case EXECUTING: return "Alex is working.";
            case ERROR: return "Something went wrong. Tap to talk to Alex.";
            default: return "Talk to Alex";
        }
    }

    private int colorFor(State s) {
        int res = s == State.ERROR ? R.color.alex_error : s == State.EXECUTING ? R.color.alex_ok : R.color.alex_accent;
        return ContextCompat.getColor(getContext(), res);
    }

    private void updateAnimation() {
        stopAnimation();
        if (state == State.IDLE || state == State.ERROR || !isAttachedToWindow()) return;
        boolean spin = state == State.THINKING || state == State.EXECUTING;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(spin ? 900 : 1400);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                phase = (float) a.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    private void stopAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        phase = 0f;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float max = Math.min(getWidth(), getHeight()) / 2f - 4;
        float r = max * 0.68f;
        int color = colorFor(state);

        if (state == State.LISTENING || state == State.SPEAKING) {
            ring.setColor(color);
            ring.setAlpha((int) (150 * (1f - phase)));
            canvas.drawCircle(cx, cy, r + (max - r) * phase, ring);
        }
        fill.setColor(color);
        canvas.drawCircle(cx, cy, r, fill);

        if (state == State.THINKING || state == State.EXECUTING) {
            float ar = r + 8;
            oval.set(cx - ar, cy - ar, cx + ar, cy + ar);
            arc.setColor(color);
            canvas.drawArc(oval, phase * 360f, 100f, false, arc);
        }

        int half = (int) (r * 0.45f);
        mic.setBounds((int) cx - half, (int) cy - half, (int) cx + half, (int) cy + half);
        mic.draw(canvas);
    }
}
