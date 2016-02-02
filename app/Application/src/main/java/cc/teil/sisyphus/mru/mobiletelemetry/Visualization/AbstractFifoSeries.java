package cc.teil.sisyphus.mru.mobiletelemetry.Visualization;

import android.graphics.Canvas;
import android.os.Parcelable;
import android.util.Log;
import android.view.animation.AnimationUtils;

import com.androidplot.Plot;
import com.androidplot.PlotListener;
import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mru on 10.01.16.
 */
public abstract class AbstractFifoSeries<DataAdapter extends AbstractPlotDataAdapter> implements PlotListener, Parcelable {

    private final static String TAG = AbstractFifoSeries.class.getSimpleName();
    private double plotTime;
    private double timeLimit = 12e4;
    private List<DataAdapter> entries = new ArrayList<>();


    protected AbstractFifoSeries() {
    }

    public void add(DataAdapter data) {
        double now = AnimationUtils.currentAnimationTimeMillis();
        data.timeStamp = now;
        getEntries().add(data);
        trim(now);
    }

    public void trim(double now) {
        while (getEntries().size() > 1) {
            DataAdapter first = getEntries().get(1);
            if (now - first.timeStamp > getTimeLimit()) {
                Log.d(TAG, "trimming on element");
                getEntries().remove(0);
            } else {
                break;
            }
        }

        while (getEntries().size() > 0) {
            DataAdapter first = getEntries().get(0);
            if (now - first.timeStamp > 2 * getTimeLimit()) {
                Log.d(TAG, "trimming on element");
                getEntries().remove(0);
            } else {
                break;
            }
        }
    }

    @Override
    public void onBeforeDraw(Plot source, Canvas canvas) {
        plotTime = AnimationUtils.currentAnimationTimeMillis();
        trim(plotTime);
    }

    @Override
    public void onAfterDraw(Plot source, Canvas canvas) {
    }

    public double getTimeLimit() { return timeLimit; }
    public double getMaxSeconds() { return timeLimit/1000.0; }

    public void setTimeLimit(double timeLimit) {
        this.timeLimit = timeLimit;
    }

    protected DataAdapter getEntry(int index) {
        return getEntries().get(index);
    }

    protected List<DataAdapter> getEntries() {
        return entries;
    }

    protected void setEntries(List<DataAdapter> entries) {
        this.entries = entries;
    }

    protected abstract class AbstractXYSeries implements XYSeries {
        @Override
        public int size() {
            return getEntries().size();
        }

        @Override
        public Number getX(int index) {
            return (getEntries().get(index).timeStamp - plotTime) / 1e3;
        }

        @Override
        public Number getY(int index) {
            return getData(getEntries().get(index));
        }

        abstract public Number getData(DataAdapter dataAdapter);
        abstract public String getUnit();

    }

}

