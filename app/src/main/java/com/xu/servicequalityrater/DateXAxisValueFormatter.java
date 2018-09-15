package com.xu.servicequalityrater;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Need to add int the hour date, according to the guide online
 */

public class DateXAxisValueFormatter implements IAxisValueFormatter {

    private SimpleDateFormat mFormat = new SimpleDateFormat("dd/mm/yyyy");

    public int getDecimalDigits() {
        return -1;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        //long millis = TimeUnit.HOURS.toMillis((long) value);
        //return mFormat.format(tVals);

        String formattedDate = null;

        //TODO all formatting should be done externally
        //TODO 2 the only purpose of getFormattedValue is to call ArrayList<String> Dates .get(i)
        long unixSeconds = (long)value;
        Date date = new Date(unixSeconds*1000L);

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        formattedDate = sdf.format(date);


        return formattedDate;


    }

}
