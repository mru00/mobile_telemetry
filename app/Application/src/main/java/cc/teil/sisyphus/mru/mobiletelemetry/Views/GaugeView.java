package cc.teil.sisyphus.mru.mobiletelemetry.Views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import cc.teil.sisyphus.mru.mobiletelemetry.R;

/**
 * TODO: document your custom view class.
 */
public class GaugeView extends View {

    private final RectF mRect = new RectF();
    private final Paint mPaintGauge = new Paint();
    private final Paint mPaintValue = new Paint();
    int mStrokeWidth = 10;
    int mStrokeColor = Color.RED;
    int mPointStartColor = Color.BLUE;
    int mPointEndColor = Color.GRAY;
    private String gaugeTitle = "hi"; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;


    public GaugeView(Context context) {
        super(context);
        init(null, 0);
    }

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public GaugeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GaugeView, defStyle, 0);

        gaugeTitle = a.getString(R.styleable.GaugeView_gaugeTitle);

        mExampleColor = a.getColor(
                R.styleable.GaugeView_gaugeColor,
                mExampleColor);

        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.GaugeView_gaugeDimension,
                mExampleDimension);

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();


        mPaintGauge.setColor(mStrokeColor);
        mPaintGauge.setStrokeWidth(mStrokeWidth);
        mPaintGauge.setAntiAlias(true);
        mPaintGauge.setStrokeCap(Paint.Cap.ROUND);
        mPaintGauge.setStyle(Paint.Style.STROKE);

        mPaintGauge.setColor(mStrokeColor);
        mPaintGauge.setShader(null);

        mPaintValue.setColor(mStrokeColor);
        mPaintValue.setStrokeWidth(mStrokeWidth);
        mPaintValue.setAntiAlias(true);
        mPaintValue.setStrokeCap(Paint.Cap.ROUND);
        mPaintValue.setStyle(Paint.Style.STROKE);

        mPaintValue.setColor(mPointStartColor);
        mPaintValue.setShader(new LinearGradient(0, 0, 1, 100, mPointEndColor, mPointStartColor, android.graphics.Shader.TileMode.MIRROR));
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(gaugeTitle);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
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

        // Draw the text.
        canvas.drawText(gaugeTitle,
                paddingLeft + (contentWidth - mTextWidth) / 2,
                paddingTop + (contentHeight + mTextHeight) / 2,
                mTextPaint);


        super.onDraw(canvas);

        int width = getWidth() - (paddingLeft + paddingRight) - 2 * mStrokeWidth;
        int height = getHeight() - (paddingTop + paddingBottom) - 2 * mStrokeWidth;

        int radius = (width > height ? height / 2 : width / 2) - 2 * mStrokeWidth;

        int mRectLeft = width / 2 - radius + paddingLeft;
        int mRectTop = height / 2 - radius + paddingTop;
        int mRectRight = width / 2 + radius;
        int mRectBottom = height / 2 + radius;


        RectF rect = new RectF(mRectLeft, mRectTop, mRectRight, mRectBottom);

        int freeAngle = 20;
        int mStartAngel = 90 + freeAngle;
        int mSweepAngel = 360 - 2 * freeAngle;

        int mPointSize = 6;
        int value = 40;

        canvas.drawArc(rect, mStartAngel, mSweepAngel, false, mPaintGauge);
        canvas.drawArc(rect, Math.max(mStartAngel, mStartAngel + value - mPointSize / 2), mPointSize, false, mPaintValue);
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return gaugeTitle;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        gaugeTitle = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }
}
