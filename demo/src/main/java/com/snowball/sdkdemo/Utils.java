package com.snowball.sdkdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowball.core.common.SnowBallLog;
import com.snowball.purchase.model.BillingPeriod;
import com.snowball.purchase.model.PriceInfo;
import com.snowball.purchase.model.Sku;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

public class Utils {

    private static final SnowBallLog gDebug = SnowBallLog.createCommonLogger("Utils");

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


    @Nullable
    public static String getDiscountOfferDesc(Context context, Sku sku) {
        BillingPeriod discountOfferBillingPeriod = sku.getDiscountOfferPeriod();
        PriceInfo disCountOfferPriceInfo = sku.getDiscountOfferPrice();
        if (discountOfferBillingPeriod == null || disCountOfferPriceInfo == null) {
            gDebug.e("firstCycleDiscountPeriod or priceInfo is null, return null for getFirstCycleDiscountDesc");
            return null;
        }

        int pluralsResId;
        if (discountOfferBillingPeriod.periodType == BillingPeriod.PeriodType.DAY) {
            pluralsResId = R.plurals.first_day_discount;
        } else if (discountOfferBillingPeriod.periodType == BillingPeriod.PeriodType.WEEK) {
            pluralsResId = R.plurals.first_week_discount;
        } else if (discountOfferBillingPeriod.periodType == BillingPeriod.PeriodType.MONTH) {
            pluralsResId = R.plurals.first_month_discount;
        } else if (discountOfferBillingPeriod.periodType == BillingPeriod.PeriodType.YEAR) {
            pluralsResId = R.plurals.first_year_discount;
        } else {
            gDebug.e("Unknown firstCycleDiscountPeriod: " + discountOfferBillingPeriod.periodType + ", return null for getFirstCycleDiscountDesc");
            return null;
        }

        int totalPeriodCount = discountOfferBillingPeriod.periodValue * sku.getDiscountOfferCycleCount();
        String price = sku.getDiscountOfferCycleCount() > 1 ? convertToPricePerPeriod(context,
                discountOfferBillingPeriod, disCountOfferPriceInfo.getDisplayPrice()) : disCountOfferPriceInfo.getDisplayPrice();

        return context.getResources().getQuantityString(pluralsResId, totalPeriodCount,
                price, totalPeriodCount);
    }

    @Nullable
    public static String getDiscountOfferFullText(@NonNull Context context, @NonNull Sku sku) {
        PriceInfo priceInfo = sku.getPriceInfo();
        Currency currency = Currency.getInstance(priceInfo.getCurrencyCode());
        BillingPeriod billingPeriod = sku.getBillingPeriod();
        DecimalFormat df = new DecimalFormat("0.00");
        String priceWithPeriod = convertToPricePerPeriod(context, billingPeriod,
                currency.getSymbol().toUpperCase() + df.format(priceInfo.getValue()));

        if (sku.hasFreeTrial() || sku.hasDiscountOffer()) {
            if (sku.hasFreeTrial()) {
                if (sku.hasDiscountOffer()) {
                    String firstCycleDiscountDesc = getDiscountOfferDesc(context, sku);
                    return context.getString(R.string.try_for_free_and_first_period_discount_tips, sku.getFreeTrialDays(), firstCycleDiscountDesc, priceWithPeriod);
                } else {
                    return context.getString(R.string.try_for_free_tips, sku.getFreeTrialDays(), priceWithPeriod);
                }

            } else {
                String firstCycleDiscountDesc = getDiscountOfferDesc(context, sku);
                return context.getString(R.string.first_period_discount_tips, firstCycleDiscountDesc, priceWithPeriod);
            }
        }

        return null;
    }

    @NonNull
    public static String getClaimText(@NonNull Context context, @NonNull Sku sku) {
        if (sku.getBillingPeriod().periodType == BillingPeriod.PeriodType.LIFETIME) {
            return context.getString(R.string.purchase_claim_lifetime);

        } else {
            PriceInfo priceInfo = sku.getPriceInfo();
            Currency currency = Currency.getInstance(priceInfo.getCurrencyCode());
            BillingPeriod billingPeriod = sku.getBillingPeriod();
            DecimalFormat df = new DecimalFormat("0.00");
            String priceWithPeriod = convertToPricePerPeriod(context, billingPeriod,
                    currency.getSymbol().toUpperCase() + df.format(priceInfo.getValue()));

            if (sku.hasFreeTrial() || sku.hasDiscountOffer()) {
                if (sku.hasFreeTrial()) {
                    if (sku.hasDiscountOffer()) {
                        String firstCycleDiscountDesc = getDiscountOfferDesc(context, sku);
                        return context.getString(R.string.purchase_claim_subs_with_free_trial, context.getString(R.string.first_period_discount_tips, firstCycleDiscountDesc, priceWithPeriod));
                    } else {
                        return context.getString(R.string.purchase_claim_subs_with_free_trial, priceWithPeriod);
                    }

                } else {
                    String firstCycleDiscountDesc = getDiscountOfferDesc(context, sku);
                    return context.getString(R.string.purchase_claim_subs_without_free_trial, context.getString(R.string.first_period_discount_tips, firstCycleDiscountDesc, priceWithPeriod));
                }
            } else {
                return context.getString(R.string.purchase_claim_subs_without_free_trial, priceWithPeriod);
            }
        }
    }

    public static String getDiscountOfferTextWithoutBasePrice(@NonNull Context context, @NonNull Sku sku) {
        if (sku.hasFreeTrial()) {
            if (sku.hasDiscountOffer()) {
                String firstCycleDiscountDesc = getDiscountOfferDesc(context, sku);
                return context.getString(R.string.try_for_free_and_first_period_discount_tips_without_base, sku.getFreeTrialDays(), firstCycleDiscountDesc);
            } else {
                return context.getString(R.string.try_for_free_tips_without_base, sku.getFreeTrialDays());
            }

        } else if (sku.hasDiscountOffer()) {
            return getDiscountOfferDesc(context, sku);

        } else {
            return null;
        }
    }

    public static int dpToPx(float dpValue) {
        final float density = Resources.getSystem().getDisplayMetrics().density;
        int value = Math.round(dpValue * density);
        if (value == 0 && dpValue != 0) {
            value = 1;
        }

        return value;
    }

    public static void openManageSubscriptionPage(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/account/subscriptions"));
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static String getFormatDate(long timeStamp) {
        Date date = new Date();
        date.setTime(timeStamp);
        return new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).format(date);
    }
    
    /** @noinspection BooleanMethodIsAlwaysInverted*/
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        NetworkInfo networkinfo = manager.getActiveNetworkInfo();

        return !(networkinfo == null || !networkinfo.isAvailable());
    }
}
