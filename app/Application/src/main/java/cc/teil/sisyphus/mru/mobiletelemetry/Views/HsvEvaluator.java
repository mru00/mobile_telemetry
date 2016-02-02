package cc.teil.sisyphus.mru.mobiletelemetry.Views;

import android.animation.TypeEvaluator;
import android.graphics.Color;

/**
 * Created by mru on 10.01.16.
 */
public class HsvEvaluator implements TypeEvaluator {
    private static final HsvEvaluator sInstance = new HsvEvaluator();

    public static HsvEvaluator getInstance() {
        return sInstance;
    }

    private float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }

    /**
     * Returns an interpoloated color, between <code>a</code> and <code>b</code>
     */
    private int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }

    public Object evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int endInt = (Integer) endValue;

        return interpolateColor(startInt, endInt, fraction);

    }
}
