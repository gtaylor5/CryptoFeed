package Utilities;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Gerard on 11/17/2017.
 */

public class DateFormatter implements IAxisValueFormatter{

    private String type = "";

    public DateFormatter(String type){
        this.type = type;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        long val = (long)value;
        Date date = new Date(val*1000L);
        SimpleDateFormat sdf;
        if(type.equalsIgnoreCase("minutely chart")){
            sdf = new SimpleDateFormat("H:mm", Locale.US);
            return sdf.format(date);
        }else {
            sdf = new SimpleDateFormat("M/d", Locale.US);
            return sdf.format(date);
        }
    }
}
