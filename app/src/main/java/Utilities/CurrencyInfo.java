package Utilities; /**
 * Created by Gerard on 10/29/2017.
 */

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class CurrencyInfo implements Parcelable, Comparable<CurrencyInfo>{

    private double Ask;
    private double BaseVolume;
    private double Bid;
    private double High;
    private double Low;
    private double Last;
    private double PrevDay;
    private double Volume;
    private double PercentageChange;
    private double previous;
    private double BTC_USD;

    private int OpenBuyOrders;
    private int OpenSellOrders;

    private String Created;
    private String TimeStamp;
    private String Name;
    private String Symbol;


    public CurrencyInfo (){}


    public double getAsk() {
        return Ask;
    }

    public void setAsk(double ask) {
        Ask = ask;
    }

    public double getBaseVolume() {
        return BaseVolume;
    }

    public void setBaseVolume(double baseVolume) {
        BaseVolume = baseVolume;
    }

    public double getBid() {
        return Bid;
    }

    public void setBid(double bid) {
        Bid = bid;
    }

    public String getCreated() {
        return Created;
    }

    public void setCreated(String created) {
        Created = created;
    }

    public double getHigh() {
        return High;
    }

    public void setHigh(double high) {
        High = high;
    }

    public double getLast() {
        return Last;
    }

    public void setLast(double last) {
        Last = last;
    }

    public double getLow() {
        return Low;
    }

    public void setLow(double low) {
        Low = low;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
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

    public String getSymbol() {
        return Symbol;
    }

    public void setSymbol(String symbol) {
        Symbol = symbol;
    }

    public double getVolume() {
        return Volume;
    }

    public void setVolume(double volume) {
        Volume = volume;
    }

    public double getPercentageChange() {
        calculatePercentageChange();
        return PercentageChange;
    }

    public void setPercentageChange(double percentageChange) {
        PercentageChange = percentageChange;
    }

    public double getBTC_USD() {
        return BTC_USD;
    }

    public void setBTC_USD(double BTC_USD) {
        this.BTC_USD = BTC_USD;
    }

    public double getPrevious() {
        return previous;
    }

    public void setPrevious(double previous) {
        this.previous = previous;
    }

    @Override
    public String toString() {
        return "Utilities.CurrencyInfo{" +
                "Ask=" + Ask +
                ", BaseVolume=" + BaseVolume +
                ", Bid=" + Bid +
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
                ", BTC_USD='" + BTC_USD + '\'' +
                '}';
    }

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

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(getAsk());
        dest.writeDouble(getBid());
        dest.writeDouble(getLast());
        dest.writeDouble(getVolume());
        dest.writeDouble(getBaseVolume());
        dest.writeDouble(getHigh());
        dest.writeDouble(getLow());
        dest.writeDouble(getPrevDay());
        dest.writeDouble(getPercentageChange());
        dest.writeDouble(getPrevious());
        dest.writeDouble(getBTC_USD());
        dest.writeString(getCreated());
        dest.writeString(getName());
        dest.writeString(getSymbol());
        dest.writeString(getTimeStamp());
        dest.writeInt(getOpenBuyOrders());
        dest.writeInt(getOpenSellOrders());
    }

    public static final Creator<CurrencyInfo> CREATOR = new Creator<CurrencyInfo>(){

        @Override
        public CurrencyInfo createFromParcel(Parcel source) {
            CurrencyInfo currencyInfo = new CurrencyInfo();
            currencyInfo.setAsk(source.readDouble());
            currencyInfo.setBid(source.readDouble());
            currencyInfo.setLast(source.readDouble());
            currencyInfo.setVolume(source.readDouble());
            currencyInfo.setBaseVolume(source.readDouble());
            currencyInfo.setHigh(source.readDouble());
            currencyInfo.setLow(source.readDouble());
            currencyInfo.setPrevDay(source.readDouble());
            currencyInfo.setPercentageChange(source.readDouble());
            currencyInfo.setPrevious(source.readDouble());
            currencyInfo.setBTC_USD(source.readDouble());
            currencyInfo.setCreated(source.readString());
            currencyInfo.setName(source.readString());
            currencyInfo.setSymbol(source.readString());
            currencyInfo.setTimeStamp(source.readString());
            currencyInfo.setOpenBuyOrders(source.readInt());
            currencyInfo.setOpenSellOrders(source.readInt());
            return currencyInfo;
        }

        @Override
        public CurrencyInfo[] newArray(int size) {
            return new CurrencyInfo[size];
        }
    };

    @Override
    public int compareTo(CurrencyInfo o) {
        if(o == this){
            return 0;
        }
        return Double.compare( o.getPercentageChange(), this.getPercentageChange());
    }
}
