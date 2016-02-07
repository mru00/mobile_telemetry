// Copyright (C) 2015 - 2016 mru@sisyphus.teil.cc

package cc.teil.sisyphus.mru.mobiletelemetry.Visualization;

import java.text.DecimalFormat;
import java.text.FieldPosition;

/**
 * Created by mru on 17.01.16.
 */
public class TimeLabelFormat extends DecimalFormat {
    public TimeLabelFormat() {
        super("#");
    }

    @Override
    public StringBuffer format(double value, StringBuffer buffer, FieldPosition position) {
        return super.format(-value, buffer, position);
    }
}
