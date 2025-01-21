package com.snowball.sdkdemo;

import android.content.Context;

import androidx.annotation.NonNull;

import com.snowball.purchase.business.iab.model.BillingPeriod;

import java.text.DecimalFormat;
import java.util.Currency;

public class IabStringUtil {

    public static String convertToPricePerPeriod(@NonNull Context context, @NonNull BillingPeriod billingPeriod, @NonNull String value) {
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

    public static String getStringByPeriodCycleType(@NonNull Context context, @NonNull BillingPeriod period) {
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
