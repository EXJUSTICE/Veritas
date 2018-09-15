package com.xu.servicequalityrater;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

/**
 * Created by Omistaja on 15/05/2017.
 */

public class DecimalValueFormatter implements IValueFormatter{



    private DecimalFormat mFormat;

    public DecimalValueFormatter() {
        mFormat = new DecimalFormat("###.####"); // use one decimal
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        // write your logic here
        return mFormat.format(value);
    }

}
