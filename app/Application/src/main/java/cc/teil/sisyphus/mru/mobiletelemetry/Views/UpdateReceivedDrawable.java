package cc.teil.sisyphus.mru.mobiletelemetry.Views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.AnimationUtils;

/**
 * TODO: document your custom view class.
 */
public class UpdateReceivedDrawable extends Drawable {

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
  //              postDelayed(this, 50);
            }
    //        ();
        }
    };
    private int[] colorMap = new int[colorCount];
    private int colorNormal = Color.GREEN;
    private int colorUpdated = Color.YELLOW;

    public UpdateReceivedDrawable() {

        for (int i = 0; i < colorCount; i++) {
            colorMap[i] = getColorFromIndex(i);
        }
    }


    private int getColorFromIndex(int index) {
        HsvEvaluator e = new HsvEvaluator();
        return (Integer) e.evaluate(index / (float) colorCount, colorNormal, colorUpdated);
    }

    public void updateReceived() {

        if (colorIdx > 0) {
            colorIdx = 10;
        } else {
//            postDelayed(animator, 10);
        }
    }


    @Override
    public void draw(Canvas canvas) {
        Rect r = getBounds();
        p.setColor(colorMap[colorIdx]);
        canvas.drawCircle(r.centerX(), r.centerY(), 5, p);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return 10;
    }

    @Override
    public int getIntrinsicHeight() {
        return 10;
    }
}
