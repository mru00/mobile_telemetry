// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.teil.sisyphus.mru.mobiletelemetry.Activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.FixedTableModel;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import cc.teil.sisyphus.mru.mobiletelemetry.MeasurementService;
import cc.teil.sisyphus.mru.mobiletelemetry.Parcels.ConfigParcel;
import cc.teil.sisyphus.mru.mobiletelemetry.Parcels.MeasurementParcel;
import cc.teil.sisyphus.mru.mobiletelemetry.R;
import cc.teil.sisyphus.mru.mobiletelemetry.UUIDs;
import cc.teil.sisyphus.mru.mobiletelemetry.Visualization.MeasurementSeries;
import cc.teil.sisyphus.mru.mobiletelemetry.Visualization.RssiHistorySeries;
import cc.teil.sisyphus.mru.mobiletelemetry.Visualization.TimeLabelFormat;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code MeasurementService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class ViewDataActivity extends Activity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final static String TAG = ViewDataActivity.class.getSimpleName();


    private TextView viewConnectionState;
    private TextView viewVoltageCell1;
    private TextView viewVoltageCell2;
    private TextView viewVoltageCellDiff;
    private TextView viewCurrent;
    private TextView viewRssi;


    private MeasurementSeries measurementSeries = new MeasurementSeries();
    private RssiHistorySeries rssiHistorySeries = new RssiHistorySeries();

    private XYPlot plotCellVoltage;
    private XYPlot plotRssi;
    private XYPlot plotCurrent;
    private XYPlot plotAccelerometer;

    private String mDeviceName;
    private String mDeviceAddress;
    private MeasurementService mMeasurementService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mMeasurementService = ((MeasurementService.LocalBinder) service).getService();
            if (!mMeasurementService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mMeasurementService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMeasurementService = null;
        }
    };

    private boolean mConnected = false;

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            if (MeasurementService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;


                updateConnectionState(cc.teil.sisyphus.mru.mobiletelemetry.R.string.connected);
                invalidateOptionsMenu();

                Toast.makeText(ViewDataActivity.this, "Connected", Toast.LENGTH_SHORT).show();

            } else if (MeasurementService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;


                updateConnectionState(cc.teil.sisyphus.mru.mobiletelemetry.R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();

                Toast.makeText(ViewDataActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();

            } else if (MeasurementService.ACTION_RSSI_UPDATE.equals(action)) {

                final int rssi = intent.getIntExtra("rssi", 0);
                viewRssi.setText(String.format("%d dB", rssi));
                rssiHistorySeries.copyFrom((RssiHistorySeries)intent.getParcelableExtra(MeasurementService.EXTRA_RSSI_SERIES));

            } else if (MeasurementService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                // What to do here?

            } else if (MeasurementService.ACTION_DATA_AVAILABLE.equals(action)) {

                UUID charUuid = UUID.fromString(intent.getStringExtra(MeasurementService.EXTRA_UUID));

                if (charUuid.equals(UUIDs.RCMON_CHAR_MEASUREMENT_UUID)) {
                    MeasurementParcel data = intent.getParcelableExtra(MeasurementService.EXTRA_DATA);

                    final double cell1 = data.getVoltage(0);
                    final double cell2 = data.getVoltage(1);
                    final double current = data.getCurrent();
                    viewVoltageCell1.setText(String.format("%.3f V", cell1));
                    viewVoltageCell2.setText(String.format("%.3f V", cell2));
                    viewVoltageCellDiff.setText(String.format("%.0f mV", (cell2 - cell1) * 1000.0f));
                    viewCurrent.setText(String.format("%.3f A", current));

                    measurementSeries.copyFrom((MeasurementSeries)intent.getParcelableExtra(MeasurementService.EXTRA_MEASUREMENT_SERIES));

                } else if (charUuid.equals(UUIDs.RCMON_CHAR_CONFIG_UUID)) {
                    ConfigParcel data = intent.getParcelableExtra(MeasurementService.EXTRA_DATA);
                    final int cellCount = data.getCellCount();
                    Log.i(TAG, "Cell count update=" + cellCount);

                    plotAccelerometer.setVisibility(data.getHasAccelerometer() ? View.VISIBLE : View.GONE);
                } else {
                    Log.e(TAG, "Unknown EXTRA_DATA");
                }
            }
        }
    };
    private final Timer plotRedrawTimer = new Timer(true);

    private Runnable makeUpdateRunnable(final XYPlot p) {
        return new Runnable() {
            @Override
            public void run() {
                p.redraw();
            }
        };
    }

    private final TimerTask plotUpdate = new TimerTask() {
        @Override
        public void run() {
            plotCellVoltage.post(makeUpdateRunnable(plotCellVoltage));
            plotRssi.post(makeUpdateRunnable(plotRssi));
            plotAccelerometer.post(makeUpdateRunnable(plotAccelerometer));
            plotCurrent.post(makeUpdateRunnable(plotCurrent));
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MeasurementService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(MeasurementService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(MeasurementService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(MeasurementService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(MeasurementService.ACTION_RSSI_UPDATE);

        return intentFilter;
    }

    private void clearUI() {
        viewConnectionState.setText("");
        viewVoltageCell1.setText("");
        viewVoltageCell2.setText("");
        viewVoltageCellDiff.setText("");
        viewCurrent.setText("");
        viewRssi.setText("");
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.w(TAG, "onRestore");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putParcelable("measurements", measurementSeries);
        //outState.putParcelable("rssi", rssiHistorySeries);
        Log.w(TAG, "onSave");
    }

    private void loadInstanceState(Bundle savedInstanceState) {
        try {
            if (savedInstanceState != null) {
                if (savedInstanceState.containsKey("measurements")) {
                    //measurementSeries = savedInstanceState.getParcelable("measurements");
                }
                if (savedInstanceState.containsKey("rssi")) {
                    //rssiHistorySeries = savedInstanceState.getParcelable("rssi");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loadInstanceState", e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate");
        loadInstanceState(savedInstanceState);


        setContentView(cc.teil.sisyphus.mru.mobiletelemetry.R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(cc.teil.sisyphus.mru.mobiletelemetry.R.id.device_address)).setText(mDeviceAddress);


        viewConnectionState = (TextView) findViewById(cc.teil.sisyphus.mru.mobiletelemetry.R.id.connection_state);

        viewVoltageCell1 = (TextView) findViewById(cc.teil.sisyphus.mru.mobiletelemetry.R.id.voltage);
        viewVoltageCell2 = (TextView) findViewById(cc.teil.sisyphus.mru.mobiletelemetry.R.id.voltageCell2);
        viewVoltageCellDiff = (TextView) findViewById(cc.teil.sisyphus.mru.mobiletelemetry.R.id.voltageDifference);
        viewCurrent = (TextView) findViewById(R.id.current);
        viewRssi = (TextView) findViewById(R.id.rssi);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, MeasurementService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        measurementSeries.setTimeLimitMilli(48e3);
        rssiHistorySeries.setTimeLimitMilli(30e3);

        createPlotCellVoltage();
        createPlotRssi();
        createPlotAccelerometer();
        createPlotCurrent();

        plotRedrawTimer.scheduleAtFixedRate(plotUpdate, 200, 200);

    }

    final DecimalFormat plotTimeAxisFormat = new TimeLabelFormat();

    private void setCommonPlotOptions(XYPlot plot, double maxSeconds) {
        plot.setDomainLabel("[sec]");
        plot.setDomainValueFormat(plotTimeAxisFormat);
        //plot.getGraphWidget().setPaddingLeft(20);
        plot.getGraphWidget().setPaddingBottom(20);
        plot.getGraphWidget().setPaddingTop(10);
        plot.setDomainBoundaries(-maxSeconds, 0, BoundaryMode.FIXED);

        plot.setUserDomainOrigin(0);
        plot.setUserRangeOrigin(0);


        final float textSize = 20;
        plot.getDomainLabelWidget().getLabelPaint().setTextSize(textSize);
        plot.getDomainLabelWidget().setWidth(100);

        //plot.getDomainLabelWidget().setPaddingBottom(20);
        plot.getRangeLabelWidget().getLabelPaint().setTextSize(textSize);
        plot.getRangeLabelWidget().setHeight(150);
        plot.getLegendWidget().getTextPaint().setTextSize(textSize);

        plot.getGraphWidget().getDomainTickLabelPaint().setTextSize(textSize);
        plot.getGraphWidget().getRangeTickLabelPaint().setTextSize(textSize);
        plot.getGraphWidget().getDomainOriginTickLabelPaint().setTextSize(textSize);
        plot.getGraphWidget().getRangeOriginTickLabelPaint().setTextSize(textSize);


        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.GRAY);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAlpha(140);
        plot.getLegendWidget().setBackgroundPaint(bgPaint);

        final int numberOfLegendEntries = plot.getSeriesRegistry().size();
        // to draw the new legend grid:
        plot.getLegendWidget().setSize(new Size(30 * numberOfLegendEntries, SizeLayoutType.ABSOLUTE, 100, SizeLayoutType.ABSOLUTE));

        plot.getLegendWidget().setTableModel(new DynamicTableModel(1, numberOfLegendEntries));
        plot.getLegendWidget().position(95,
                XLayoutStyle.ABSOLUTE_FROM_LEFT,
                45,
                YLayoutStyle.ABSOLUTE_FROM_TOP,
                AnchorPosition.LEFT_TOP);
    }

    private void createPlotCellVoltage() {
        XYPlot plot = plotCellVoltage = (XYPlot) findViewById(R.id.plotCellVoltage);

        plot.setTitle("Cell Voltage");

        LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.RED, Color.TRANSPARENT, null);
        LineAndPointFormatter series2Format = new LineAndPointFormatter(Color.BLUE, Color.BLUE, Color.TRANSPARENT, null);

        //series1Format.setInterpolationParams(new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));
        //series2Format.setInterpolationParams(new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));


        plot.setRangeBoundaries(-0.5, 5, BoundaryMode.FIXED);

        plot.setRangeLabel("[V]");

        plot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        plot.setDomainStepValue(50);

        plot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        plot.setRangeStepValue(1);
        plot.setTicksPerRangeLabel(1);

        plot.setRangeValueFormat(new DecimalFormat("###"));

        plot.addSeries(measurementSeries.getCellVoltage(0), series1Format);
        plot.addSeries(measurementSeries.getCellVoltage(1), series2Format);
        plot.addListener(measurementSeries);

        setCommonPlotOptions(plot, measurementSeries.getMaxSeconds());
    }


    private void createPlotCurrent() {
        XYPlot plot = plotCurrent = (XYPlot) findViewById(R.id.plotCurrent);

        plot.setTitle("Current");

        LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.RED, Color.TRANSPARENT, null);

        //series1Format.setInterpolationParams(new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));
        //series2Format.setInterpolationParams(new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

        plot.setRangeBoundaries(-0.5, 60, BoundaryMode.FIXED);

        plot.setRangeLabel("[A]");

        plot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        plot.setRangeStepValue(2);

        plot.setRangeValueFormat(new DecimalFormat("###"));

        plot.addSeries(measurementSeries.getCurrent(), series1Format);
        plot.addListener(measurementSeries);

        plot.setTicksPerRangeLabel(3);
        setCommonPlotOptions(plot, measurementSeries.getMaxSeconds());
    }

    private void createPlotAccelerometer() {
        XYPlot plot = plotAccelerometer = (XYPlot) findViewById(R.id.plotAccelerometer);

        plot.setTitle("Acceleration");

        LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.RED, Color.TRANSPARENT, null);
        LineAndPointFormatter series2Format = new LineAndPointFormatter(Color.BLUE, Color.BLUE, Color.TRANSPARENT, null);
        LineAndPointFormatter series3Format = new LineAndPointFormatter(Color.GREEN, Color.GREEN, Color.TRANSPARENT, null);

        //series1Format.setInterpolationParams(new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));
        //series2Format.setInterpolationParams(new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

        // uncomment this line to freeze the range boundaries:
        plot.setRangeBoundaries(-5, 5, BoundaryMode.FIXED);

        plot.setRangeLabel("[g]");


        plot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        plot.setRangeStepValue(1);

        plot.setRangeValueFormat(new DecimalFormat("###"));

        // add a new series' to the xyplot:
        plot.addSeries(measurementSeries.getAccelerometer(0), series1Format);
        plot.addSeries(measurementSeries.getAccelerometer(1), series2Format);
        plot.addSeries(measurementSeries.getAccelerometer(2), series3Format);
        plot.addListener(measurementSeries);

        // reduce the number of range labels
        plot.setTicksPerRangeLabel(1);

        setCommonPlotOptions(plot, measurementSeries.getMaxSeconds());
    }


    private void createPlotRssi() {
        final XYPlot plot = plotRssi = (XYPlot) findViewById(R.id.plotRssi);

        plot.setTitle("RSSI");

        LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.RED, Color.TRANSPARENT, null);

        plot.setRangeBoundaries(-120, 0, BoundaryMode.FIXED);

        plot.setRangeLabel("[dB]");

        plot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        plot.setDomainStepValue(5);

        plot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        plot.setRangeStepValue(10);

        plot.setRangeValueFormat(new DecimalFormat("###"));

        plot.addSeries(rssiHistorySeries.getRssi(), series1Format);
        plot.addListener(rssiHistorySeries);

        plot.setTicksPerRangeLabel(3);

        setCommonPlotOptions(plot, rssiHistorySeries.getMaxSeconds());
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mMeasurementService != null) {
            final boolean result = mMeasurementService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);

            Toast.makeText(ViewDataActivity.this, result ? "Failed to connect to service": "Connected to service", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mMeasurementService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(cc.teil.sisyphus.mru.mobiletelemetry.R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(cc.teil.sisyphus.mru.mobiletelemetry.R.id.menu_connect).setVisible(false);
            menu.findItem(cc.teil.sisyphus.mru.mobiletelemetry.R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(cc.teil.sisyphus.mru.mobiletelemetry.R.id.menu_connect).setVisible(true);
            menu.findItem(cc.teil.sisyphus.mru.mobiletelemetry.R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case cc.teil.sisyphus.mru.mobiletelemetry.R.id.menu_connect:
                mMeasurementService.connect(mDeviceAddress);
                return true;
            case cc.teil.sisyphus.mru.mobiletelemetry.R.id.menu_disconnect:
                mMeasurementService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewConnectionState.setText(resourceId);
            }
        });
    }
}
