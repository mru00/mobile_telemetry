// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc
 
package cc.teil.sisyphus.mru.mobiletelemetry.Visualization;

import android.graphics.Canvas;
import android.os.Parcelable;
import android.util.Log;

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
    private double plotTimeMilli;
    private double timeLimitMilli = 12e4;
    private List<DataAdapter> entries = new ArrayList<>();

    private static double t0 = System.nanoTime();

    private double getNowMilli() {
        final double now = 1e-6*(System.nanoTime()-t0);
//        Log.w(TAG, "now:" + now);
        return now;
    }

    protected AbstractFifoSeries() {}

    public void add(DataAdapter data) {
        final double now = getNowMilli();
        data.timeStampMilli = now;
        getEntries().add(data);
        trim(now);
    }

    public void copyFrom(AbstractFifoSeries<DataAdapter> other) {
        //Log.w(TAG, "copy from " + other.getClass().getSimpleName() + " " + other.getEntries().size());
        setEntries(other.getEntries());
        //timeLimitMilli = other.timeLimitMilli;
    }

    public void trim(double now) {
            while (getEntries().size() > 1) {
                DataAdapter first = getEntry(1);
                if (now - first.timeStampMilli > getTimeLimitMilli()) {
                    //Log.w(TAG, "trimming on element");
                    getEntries().remove(0);
                } else {
                    break;
                }
            }

            while (getEntries().size() > 0) {
                DataAdapter first = getEntry(0);
                if (now - first.timeStampMilli > 2 * getTimeLimitMilli()) {
                    //Log.w(TAG, "trimming on element");
                    getEntries().remove(0);
                } else {
                    break;
                }
            }

    }

    @Override
    public void onBeforeDraw(Plot source, Canvas canvas) {
        plotTimeMilli = getNowMilli();
        //trim(plotTimeMilli);
    }

    @Override
    public void onAfterDraw(Plot source, Canvas canvas) {
    }

    public double getTimeLimitMilli() { return timeLimitMilli; }
    public double getMaxSeconds() { return timeLimitMilli /1000.0; }

    public void setTimeLimitMilli(double timeLimitMilli) {
        this.timeLimitMilli = timeLimitMilli;
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
            return (getEntry(index).timeStampMilli - plotTimeMilli) / 1e3;
        }

        @Override
        public Number getY(int index) {
            return getData(getEntry(index));
        }

        abstract public Number getData(DataAdapter dataAdapter);
    }

}

