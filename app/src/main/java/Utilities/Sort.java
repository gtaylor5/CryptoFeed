package Utilities;

import java.util.Comparator;

/**
 * Created by Gerard on 12/15/2017.
 */

public class Sort {

    public static Comparator<CurrencyInfo> sortPercentLowToHigh =  new Comparator<CurrencyInfo>() {
        public int compare(CurrencyInfo c1, CurrencyInfo c2) {
            if(c1.getPercentageChange() < c2.getPercentageChange()){
                return -1;
            } else if (c1.getPercentageChange() > c2.getPercentageChange()){
                return 1;
            } else {
                return 0;
            }
        }
    };

    public static Comparator<CurrencyInfo> sortPercentHighToLow =  new Comparator<CurrencyInfo>() {
        public int compare(CurrencyInfo c1, CurrencyInfo c2) {
            return (int)(c2.getPercentageChange() - c1.getPercentageChange());
        }
    };

    public static Comparator<CurrencyInfo> sortPriceHighToLow =  new Comparator<CurrencyInfo>() {
        public int compare(CurrencyInfo c1, CurrencyInfo c2) {
            if(c2.getLast()*c2.getBTC_USD() < c1.getLast()*c1.getBTC_USD()){
                return -1;
            } else if (c2.getLast()*c2.getBTC_USD() > c1.getLast()*c1.getBTC_USD()){
                return 1;
            } else {
                return 0;
            }
        }
    };

    public static Comparator<CurrencyInfo> sortPriceLowToHigh =  new Comparator<CurrencyInfo>() {
        public int compare(CurrencyInfo c1, CurrencyInfo c2) {
            if(c1.getLast()*c1.getBTC_USD() < c2.getLast()*c2.getBTC_USD()){
                return -1;
            } else if (c1.getLast()*c1.getBTC_USD() > c2.getLast()*c2.getBTC_USD()){
                return 1;
            } else {
                return 0;
            }
        }
    };

}
