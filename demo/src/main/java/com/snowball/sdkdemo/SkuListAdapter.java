package com.snowball.sdkdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.snowball.purchase.model.BillingPeriod;
import com.snowball.purchase.model.PriceInfo;
import com.snowball.purchase.model.Sku;
import com.snowball.purchase.model.SkuListSummary;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class SkuListAdapter extends RecyclerView.Adapter<SkuListAdapter.SkuItemViewHolder> {

    public interface SkuListAdapterListener {
        void onIabItemClicked(int position, @NonNull Sku sku);
    }

    protected Activity mHostActivity;
    private final SkuListAdapter.SkuListAdapterListener mListener;
    protected SkuListSummary mItemInfoListSummary = null;
    protected List<Sku> mSkuList;
    private int mSelectedIndex = -1;

    public SkuListAdapter(Activity activity, SkuListAdapterListener listAdapterListener) {
        mHostActivity = activity;
        mListener = listAdapterListener;
    }

    public void setData(List<Sku> skuList, SkuListSummary summary) {
        mSkuList = skuList;
        mItemInfoListSummary = summary;
    }

    @NonNull
    @Override
    public SkuListAdapter.SkuItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(mHostActivity).inflate(R.layout.item_sku, viewGroup, false);
        return new SkuItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SkuItemViewHolder viewHolder, int position) {
        Context context = viewHolder.itemView.getContext();
        Sku sku = mSkuList.get(position);
        setData(sku, viewHolder);
        int currentItemIndex= getCurrentItemIndex();
        if (currentItemIndex >= 0 && currentItemIndex == position) {
            viewHolder.divideTextView.setTextColor(ContextCompat.getColor(context, R.color.pro_gradient_center));
            setPeriodGradient(viewHolder.periodTextView, viewHolder.itemView.getContext());
            setPriceGradient(viewHolder.priceTextView, viewHolder.itemView.getContext());
            setPriceGradient(viewHolder.offerDescView, viewHolder.itemView.getContext());
            viewHolder.mContainerLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_bg_license_status_selected));

        } else {
            viewHolder.divideTextView.setTextColor(ContextCompat.getColor(context, R.color.text_common_color_first));
            viewHolder.periodTextView.getPaint().setShader(null);
            viewHolder.periodTextView.setTextColor(ContextCompat.getColor(context, R.color.text_common_color_first));
            viewHolder.priceTextView.getPaint().setShader(null);
            viewHolder.priceTextView.setTextColor(ContextCompat.getColor(context, R.color.text_common_color_first));
            viewHolder.offerDescView.getPaint().setShader(null);
            viewHolder.offerDescView.setTextColor(ContextCompat.getColor(context, R.color.text_common_color_first));
            viewHolder.mContainerLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_bg_license_status));
        }
    }

    public Sku getRecommendedIabSku() {
        int recommendItemIndex = mItemInfoListSummary != null ?
                mItemInfoListSummary.getRecommendSkuIndex() : -1;
        if (recommendItemIndex >= 0) {
            int recommendIndex = mItemInfoListSummary.getRecommendSkuIndex();
            if (recommendIndex>= 0 && mSkuList != null && mSkuList.size() > recommendIndex) {
                return mSkuList.get(mItemInfoListSummary.getRecommendSkuIndex());
            } else {
                return null;
            }
        }
        return null;
    }

    private void setPeriodGradient(TextView textView, Context context) {
        int[] colors = {ContextCompat.getColor(context, R.color.pro_gradient_start),
                ContextCompat.getColor(context, R.color.pro_gradient_center)};
        float[] position = {0f, 1.0f};
        LinearGradient linearGradient = new LinearGradient(0, 0,
                textView.getPaint().measureText(textView.getText().toString()), 0, colors, position, Shader.TileMode.CLAMP);
        textView.getPaint().setShader(linearGradient);
    }

    private void setPriceGradient(TextView textView, Context context) {
        LinearGradient linearGradient = new LinearGradient(0, 0,
                textView.getPaint().measureText(textView.getText().toString()), 0,
                ContextCompat.getColor(context, R.color.pro_gradient_center),
                ContextCompat.getColor(context, R.color.pro_gradient_end), Shader.TileMode.CLAMP);
        textView.getPaint().setShader(linearGradient);
    }

    @SuppressLint("SetTextI18n")
    private void setData(Sku sku, @NonNull SkuListAdapter.SkuItemViewHolder viewHolder) {
        PriceInfo priceInfo = sku.getPriceInfo();
        Currency currency = Currency.getInstance(priceInfo.getCurrencyCode());
        BillingPeriod billingPeriod = sku.getBillingPeriod();
        DecimalFormat df = new DecimalFormat("0.00");
        String priceText;

        priceText = currency.getSymbol() + df.format(priceInfo.getValue());
        viewHolder.priceTextView.setText(priceText);

        boolean setUnitPrice = false;
        if (mItemInfoListSummary != null && mItemInfoListSummary.shouldShowUnitPrice()) {
            BillingPeriod.PeriodType unitPricePeriodType = mItemInfoListSummary != null ? mItemInfoListSummary.getUnitPricePeriodType() : BillingPeriod.PeriodType.WEEK;
            if (billingPeriod != null
                    && billingPeriod.periodType != BillingPeriod.PeriodType.LIFETIME
                    && (billingPeriod.periodType != unitPricePeriodType || billingPeriod.periodValue != 1)) {

                BillingPeriod unitPeriod = new BillingPeriod(unitPricePeriodType, 1);
                double unitPrice = priceInfo.getValue() / billingPeriod.getPeriodInDays() * unitPeriod.getPeriodInDays();
                String unitPriceStr = Utils.convertToPricePerPeriod(mHostActivity, unitPeriod, currency.getSymbol() + df.format(unitPrice));
                viewHolder.originPriceTextView.setText("= " + unitPriceStr);
                setUnitPrice = true;

                viewHolder.originPriceTextView.setVisibility(View.VISIBLE);
            }
        }

        if (sku.shouldShowDiscountPercent()) {
            double priceDiscountPercent = sku.getDiscountPercent();
            if (!setUnitPrice && (1.0f - priceDiscountPercent) > 0.001) {
                viewHolder.originPriceTextView.setText(currency.getSymbol() + df.format(priceInfo.getValue() / (1.0f - priceDiscountPercent)));

                viewHolder.originPriceTextView.getPaint().setFlags(viewHolder.originPriceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                viewHolder.originPriceTextView.setVisibility(View.VISIBLE);
            }

            if (priceDiscountPercent > 0 && priceDiscountPercent < 1) {
                viewHolder.discountView.setText(String.format(Locale.US, "%.0f%% OFF", priceDiscountPercent * 100));
                viewHolder.discountView.setVisibility(View.VISIBLE);
            }else{
                viewHolder.discountView.setVisibility(View.GONE);
            }
        }else {
            viewHolder.discountView.setVisibility(View.GONE);
        }

        if (billingPeriod != null) {
            String periodText = Utils.getStringByPeriodCycleType(mHostActivity, billingPeriod);
            viewHolder.periodTextView.setText(periodText);

        } else {
            viewHolder.periodTextView.setVisibility(View.GONE);
        }

        String offerTextWithBasePrice = Utils.getDiscountOfferTextWithoutBasePrice(viewHolder.offerDescView.getContext(), sku);
        if (!TextUtils.isEmpty(offerTextWithBasePrice)) {
            viewHolder.offerDescView.setText(offerTextWithBasePrice);
            viewHolder.offerDescView.setVisibility(View.VISIBLE);

        } else {
            viewHolder.offerDescView.setVisibility(View.GONE);
        }
    }

    private int getCurrentItemIndex() {
        if (mSelectedIndex >= 0) {
            return mSelectedIndex;

        } else {
            return mItemInfoListSummary != null ?
                    mItemInfoListSummary.getRecommendSkuIndex() : -1;
        }
    }

    @Override
    public int getItemCount() {
        return mSkuList == null ? 0 : mSkuList.size();
    }

    @Override
    public long getItemId(int position) {
        return (mSkuList.get(position).getSkuId()).hashCode();
    }

    public class SkuItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public RelativeLayout mContainerLayout;
        public TextView periodTextView;
        public TextView divideTextView;
        public TextView priceTextView;
        public TextView originPriceTextView;
        public TextView discountView;
        public TextView offerDescView;

        public SkuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            periodTextView = itemView.findViewById(R.id.tv_period);
            priceTextView = itemView.findViewById(R.id.tv_price);
            divideTextView = itemView.findViewById(R.id.tv_divide);
            originPriceTextView = itemView.findViewById(R.id.tv_original_price);
            discountView = itemView.findViewById(R.id.tv_discount);
            offerDescView = itemView.findViewById(R.id.tv_first_cycle_discount_desc);
            mContainerLayout = itemView.findViewById(R.id.rl_container);
            itemView.setOnClickListener(this);
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onClick(View v) {
            if (mListener != null && mSkuList != null) {
                int position = getBindingAdapterPosition();
                if (position >= 0 && position < getItemCount()) {
                    mListener.onIabItemClicked(position, mSkuList.get(getBindingAdapterPosition()));
                    mSelectedIndex = position;
                    notifyDataSetChanged();
                }
            }
        }
    }
}
