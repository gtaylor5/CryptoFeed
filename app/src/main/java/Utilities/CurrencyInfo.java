package Utilities; /**
 * Created by Gerard on 10/29/2017.
 */

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cryptoinc.cryptofeed.BR;

import org.json.JSONArray;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;

public class CurrencyInfo extends BaseObservable implements Comparable<CurrencyInfo>{

    private double High;
    private double High24Hr;
    private double Low;
    private double Low24Hr;
    private double Last;
    private double PrevDay;
    private double Volume;
    private double Volume24Hr;
    private double PercentageChange;

    private int OpenBuyOrders;
    private int OpenSellOrders;

    private String Created;
    private String TimeStamp;
    private String Name;
    private String Symbol;


    public CurrencyInfo (){}

    public void calculatePercentageChange(){
        double last = getLast();
        double prevDay = getPrevDay();
        double percentageChange = 0;
        if(last >= prevDay){
            double delta = last-prevDay;
            delta = delta / prevDay;
            delta *= 100;
            percentageChange = delta;
        } else {
            double delta = prevDay - last;
            delta = delta / prevDay;
            delta *= -100;
            percentageChange = delta;
        }
        setPercentageChange(percentageChange);
    }

    // Getters and Setters

    @Bindable
    public double getHigh24Hr() {
        return High24Hr;
    }

    public void setHigh24Hr(double high24Hr) {
        High24Hr = high24Hr;
    }

    @Bindable
    public double getLow24Hr() {
        return Low24Hr;
    }

    public void setLow24Hr(double low24Hr) {
        Low24Hr = low24Hr;
    }

    @Bindable
    public double getVolume24Hr() {
        return Volume24Hr;
    }

    public void setVolume24Hr(double volume24Hr) {
        Volume24Hr = volume24Hr;
    }

    @Bindable
    public double getHigh() {
        return High;
    }

    public void setHigh(double high) {
        High = high;
        notifyPropertyChanged(BR.high);
    }

    @Bindable
    public double getLast() {
        return Last;
    }

    public void setLast(double last) {
        Last = last;
    }

    @Bindable
    public double getLow() {
        return Low;
    }

    public void setLow(double low) {
        Low = low;
        notifyPropertyChanged(BR.low);
    }

    @Bindable
    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
        notifyPropertyChanged(BR.name);
    }

    @Bindable
    public String getSymbol() {
        return Symbol;
    }

    public void setSymbol(String symbol) {
        Symbol = symbol;
        notifyPropertyChanged(BR.symbol);
    }
    @Bindable
    public double getVolume() {
        return Volume;
    }

    public void setVolume(double volume) {
        Volume = volume;
    }

    @Bindable
    public double getPercentageChange() {
        calculatePercentageChange();
        return PercentageChange;
    }

    public void setPercentageChange(double percentageChange) {
        PercentageChange = percentageChange;
        notifyPropertyChanged(BR.percentageChange);
    }

    public String getCreated() {
        return Created;
    }

    public void setCreated(String created) {
        Created = created;
    }

    public int getOpenBuyOrders() {
        return OpenBuyOrders;
    }

    public void setOpenBuyOrders(int openBuyOrders) {
        OpenBuyOrders = openBuyOrders;
    }

    public int getOpenSellOrders() {
        return OpenSellOrders;
    }

    public void setOpenSellOrders(int openSellOrders) {
        OpenSellOrders = openSellOrders;
    }

    public double getPrevDay() {
        return PrevDay;
    }

    public void setPrevDay(double prevDay) {
        PrevDay = prevDay;
    }

    public String getTimeStamp() {
        return TimeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        TimeStamp = timeStamp;
    }


    @Override
    public String toString() {
        return "Utilities.CurrencyInfo{" +
                ", High=" + High +
                ", Low=" + Low +
                ", Last=" + Last +
                ", PrevDay=" + PrevDay +
                ", Volume=" + Volume +
                ", PercentageChange=" + PercentageChange +
                ", OpenBuyOrders=" + OpenBuyOrders +
                ", OpenSellOrders=" + OpenSellOrders +
                ", Created='" + Created + '\'' +
                ", TimeStamp='" + TimeStamp + '\'' +
                ", Name='" + Name + '\'' +
                ", Symbol='" + Symbol + '\'' +
                '}';
    }




    @Override
    public int compareTo(CurrencyInfo o) {
        if(o == this){
            return 0;
        }
        return Double.compare( o.getPercentageChange(), this.getPercentageChange());
    }
}
