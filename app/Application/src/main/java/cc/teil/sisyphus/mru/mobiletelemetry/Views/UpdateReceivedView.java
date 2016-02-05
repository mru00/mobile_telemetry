// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

package cc.teil.sisyphus.mru.mobiletelemetry.Views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import cc.teil.sisyphus.mru.mobiletelemetry.R;

/**
 * TODO: document your custom view class.
 */
public class UpdateReceivedView extends View {

    private final int colorCount = 100;
    Paint p = new Paint();
    private int colorIdx = 0;
    private final Runnable animator = new Runnable() {
        @Override
        public void run() {
            boolean scheduleAgain = false;
            long now = AnimationUtils.currentAnimationTimeMillis();
            if (colorIdx > 0) {
                scheduleAgain = true;
                colorIdx -= 1;
            }
            if (scheduleAgain) {
                postDelayed(this, 50);
            }
            invalidate();
        }
    };
    private int[] colorMap = new int[colorCount];
    private int colorNormal = Color.GREEN;
    private int colorUpdated = Color.YELLOW;

    public UpdateReceivedView(Context context) {
        super(context);
        init(null, 0);

        for (int i = 0; i < colorCount; i++) {
            colorMap[i] = getColorFromIndex(i);
        }
    }

    public UpdateReceivedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public UpdateReceivedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private int getColorFromIndex(int index) {
        HsvEvaluator e = new HsvEvaluator();
        return (Integer) e.evaluate(index / (float) colorCount, colorNormal, colorUpdated);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.UpdateReceivedView, defStyle, 0);
        colorNormal = a.getColor(R.styleable.UpdateReceivedView_urcColor, colorNormal);
        a.recycle();
    }

    public void updateReceived() {

        if (colorIdx > 0) {
            colorIdx = 10;
        } else {
            postDelayed(animator, 10);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        p.setColor(colorMap[colorIdx]);
        canvas.drawCircle(contentWidth / 2, contentHeight / 2, 5, p);
    }
}
