package com.snowball.sdkdemo;

import android.content.Context;

import androidx.annotation.NonNull;

import com.snowball.purchase.business.iab.model.BillingPeriod;

import java.text.DecimalFormat;
import java.util.Currency;

public class IabStringUtil {

    public static String convertToPricePerPeriod(Context context, BillingPeriod billingPeriod, String value) {
        if (billingPeriod == null) {
            return value;
        }

        BillingPeriod.PeriodType type = billingPeriod.periodType;
        switch (type) {
            case DAY:
                return billingPeriod.periodValue == 1 ? context.getString(R.string.price_per_day, value) : value + " / " +
                        context.getResources().getQuantityString(R.plurals.day_number, billingPeriod.periodValue, billingPeriod.periodValue);
            case WEEK:
                return billingPeriod.periodValue == 1 ? context.getString(R.string.price_per_week, value) :value + " / " +
                        context.getResources().getQuantityString(R.plurals.week_number, billingPeriod.periodValue, billingPeriod.periodValue);
            case MONTH:
                return billingPeriod.periodValue == 1 ? context.getString(R.string.price_per_month, value) : value + " / " +
                        context.getResources().getQuantityString(R.plurals.month_number, billingPeriod.periodValue, billingPeriod.periodValue);
            case YEAR:
                return billingPeriod.periodValue == 1 ? context.getString(R.string.price_per_year, value) : value + " / " +
                        context.getResources().getQuantityString(R.plurals.year_number, billingPeriod.periodValue, billingPeriod.periodValue);
            case LIFETIME:
                return context.getString(R.string.lifetime);
            default:
                return value;
        }
    }

    public static String convertToPriceEachPeriod(Context context, BillingPeriod billingPeriod, String value) {
        if (billingPeriod == null) {
            return value;
        }
        BillingPeriod.PeriodType type = billingPeriod.periodType;
        switch (type) {
            case DAY:
                return context.getString(R.string.price_each_day, value);
            case WEEK:
                return context.getString(R.string.price_each_week, value);
            case MONTH:
                return context.getString(R.string.price_each_month, value);
            case YEAR:
                return context.getString(R.string.price_each_year, value);
            case LIFETIME:
                return context.getString(R.string.lifetime);
            default:
                return value;
        }
    }

    public static String getDisplayPrice(@NonNull String currencyCode, double value) {
        Currency currency = Currency.getInstance(currencyCode);
        String currencySymbol = currency.getSymbol().toUpperCase();

        DecimalFormat df = new DecimalFormat("0.00");
        return currencySymbol + df.format(value);
    }

    public static String getDecimalFormattedPrice(double value) {
        return new DecimalFormat("0.00").format(value);
    }

    public static String getDisplayOriginalPriceByDiscount(@NonNull String currencyCode, double currentPrice, double discountPercent) {
        return getDisplayPrice(currencyCode, getOriginalPriceValueByDiscount(currentPrice, discountPercent));
    }

    public static double getOriginalPriceValueByDiscount(double currentPrice, double discountPercent) {
        return 1.0f - discountPercent > 0.001 ? currentPrice / (1.0F - discountPercent) : currentPrice;
    }

    public static String getStringByPeriodCycleType(Context context, BillingPeriod period) {
        BillingPeriod.PeriodType type = period.periodType;
        if (period.periodValue == 1) {
            switch (type) {
                case DAY:
                    return context.getResources().getString(R.string.daily);
                case WEEK:
                    return context.getResources().getString(R.string.weekly);
                case MONTH:
                    return context.getResources().getString(R.string.monthly);
                case YEAR:
                    return context.getResources().getString(R.string.yearly);
                case LIFETIME:
                    return context.getString(R.string.lifetime);
                default:
                    return null;
            }
        }

        switch (type) {
            case DAY:
                return context.getResources().getQuantityString(R.plurals.every_day_number, period.periodValue, period.periodValue);
            case WEEK:
                return context.getResources().getQuantityString(R.plurals.every_week_number, period.periodValue, period.periodValue);
            case MONTH:
                return context.getResources().getQuantityString(R.plurals.every_month_number, period.periodValue, period.periodValue);
            case YEAR:
                return context.getResources().getQuantityString(R.plurals.every_year_number, period.periodValue, period.periodValue);
            case LIFETIME:
                return context.getString(R.string.lifetime);
            default:
                return null;
        }
    }
}
