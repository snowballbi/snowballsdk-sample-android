package com.snowball.sdkdemo;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.snowball.common.SnowBallLog;
import com.snowball.common.SnowBallUtils;
import com.snowball.purchase.business.SnowBallLicenseController;
import com.snowball.purchase.business.iab.IabController;
import com.snowball.purchase.business.iab.IabPurchaseUtil;
import com.snowball.purchase.business.iab.model.BillingPeriod;
import com.snowball.purchase.business.iab.model.SkuListSummary;
import com.snowball.purchase.business.iab.model.Sku;
import com.snowball.purchase.business.license.model.DowngradeType;
import com.snowball.purchase.business.license.model.PurchaseData;
import com.snowball.purchase.business.license.model.PurchaseError;
import com.snowball.purchase.business.license.model.SkuType;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.List;

public class LicenseUpgradeActivity extends FragmentActivity implements LicenseUpgradeModel.Callback {
    private static final SnowBallLog gDebug = SnowBallLog.createCommonLogger("LicenseUpgradeActivity");

    private static final String DIALOG_TAG_REFRESHING_LICENSE = "refreshing_license";
    private static final String DIALOG_TAG_CONFIRMING_LICENSE = "confirming_license";

    private final LicenseUpgradeModel mLicenseUpgradeModel = new LicenseUpgradeModel(this, this);
    private ActivityResultLauncher<Intent> mResumeLicenseActivityResultLauncher;


    private TextView mTryForFreeTv;
    private TextView mManageSubscriptionTv;
    private SkuListAdapter mSkuListAdapter;
    private View mToPurchaseLayout;
    private View mPurchasedLayout;
    private TextView mClaimTextView;
    private Button mPurchaseBtn;
    private Sku mSelectedSku;
    private TextView mPurchasedLicenseTypeTv;
    private TextView mPurchasedExpireDateTv;
    private View mLoadingPriceView;
    private View mButtonContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityResultLauncher();

        setContentView(R.layout.activity_license_upgrade);
        initView();
        mLicenseUpgradeModel.loadData(true);
    }

    private void initActivityResultLauncher() {
        mResumeLicenseActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> mLicenseUpgradeModel.refreshLicenseData(true));
    }

    private void initView() {
        mLoadingPriceView = findViewById(R.id.v_loading_price);
        mPurchasedLayout = findViewById(R.id.v_upgraded);
        mToPurchaseLayout = findViewById(R.id.rl_to_purchase);
        mClaimTextView = findViewById(R.id.tv_claim);
        mManageSubscriptionTv = findViewById(R.id.tv_manage_subscription);
        mPurchaseBtn = findViewById(R.id.btn_purchase);
        mPurchaseBtn.setOnClickListener(v -> {
            if (mSelectedSku != null) {
                mLicenseUpgradeModel.startPurchasing(mSelectedSku, "default_scene");
            }
        });
        mTryForFreeTv = findViewById(R.id.tv_free_trail_tips);

        mPurchasedLicenseTypeTv = findViewById(R.id.tv_license_type);
        mPurchasedExpireDateTv = findViewById(R.id.tv_expire_date);
        mButtonContainer = findViewById(R.id.ll_bottom_buttons);

        mSkuListAdapter = new SkuListAdapter(this, (position, sku) -> {
            mSelectedSku = sku;
            updatePurchaseClaimText(sku);
        });

        mSkuListAdapter.setHasStableIds(true);

        RecyclerView upgradeOptions = findViewById(R.id.rv_upgrade_options);
        upgradeOptions.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        upgradeOptions.setLayoutManager(layoutManager);
        int recyclerViewItemMargin = SnowBallUtils.dpToPx(10);
        upgradeOptions.addItemDecoration(new SkuSpaceItemDecoration(recyclerViewItemMargin));
        upgradeOptions.setAdapter(mSkuListAdapter);
        mManageSubscriptionTv.setOnClickListener(v -> SnowBallUtils.openManageSubscriptionPage(this));
        findViewById(R.id.iv_close).setOnClickListener(v -> LicenseUpgradeActivity.this.finish());
        findViewById(R.id.tv_restore_purchase).setOnClickListener(v -> mLicenseUpgradeModel.refreshLicenseData(true));

    }

    @Override
    public boolean isViewFinishing() {
        return LicenseUpgradeActivity.this.isFinishing();
    }

    @Override
    public void showNoNetworkMessage() {
        Toast.makeText(LicenseUpgradeActivity.this, getString(R.string.msg_network_error), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showPurchaseFailed(@NonNull PurchaseError purchaseError, @Nullable String data) {
        Toast.makeText(getApplicationContext(), getString(R.string.pay_failed), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showProView(PurchaseData purchaseData) {
        mLoadingPriceView.setVisibility(View.GONE);
        mPurchasedLayout.setVisibility(View.VISIBLE);
        mToPurchaseLayout.setVisibility(View.GONE);
        if (purchaseData.getSkuType() == SkuType.Subs) {
            long endDate = purchaseData.getExpireTime();
            String date = SnowBallUtils.getFormatDate(endDate);
            mPurchasedExpireDateTv.setText(getString(R.string.expire_time, date));
            mPurchasedLicenseTypeTv.setText(getString(R.string.subscription));
            mManageSubscriptionTv.setVisibility(View.VISIBLE);

        } else {
            mPurchasedExpireDateTv.setVisibility(View.GONE);
            mPurchasedLicenseTypeTv.setText(getString(R.string.lifetime));
            mManageSubscriptionTv.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void showLicenseUpgradedPrompt(SkuType skuType) {
        Toast.makeText(this, getString(R.string.message_license_upgraded), Toast.LENGTH_LONG).show();
    }

    @Override
    public void showLoadingPriceView() {
        mLoadingPriceView.setVisibility(View.VISIBLE);
        mToPurchaseLayout.setVisibility(View.VISIBLE);
        mPurchasedLayout.setVisibility(View.GONE);
        mButtonContainer.setVisibility(View.GONE);
    }

    @Override
    public void dismissLoadingPriceView() {
        mLoadingPriceView.setVisibility(View.GONE);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void showPriceList(List<Sku> skuList, SkuListSummary summary) {
        mLoadingPriceView.setVisibility(View.GONE);
        mButtonContainer.setVisibility(View.VISIBLE);
        mSkuListAdapter.setData(skuList, summary);
        mSkuListAdapter.notifyDataSetChanged();
        Sku recommendSku = mSkuListAdapter.getRecommendedIabSku();
        mSelectedSku = recommendSku;

        if (!SnowBallLicenseController.getInstance().isProLicense()) {
            updatePurchaseClaimText(recommendSku);
        }
    }

    private void updatePurchaseClaimText(Sku sku) {
        if (sku == null) {
            gDebug.e("updateClaimTv sku == null");
            return;
        }

        if (sku.getBillingPeriod().periodType == BillingPeriod.PeriodType.LIFETIME) {
            mPurchaseBtn.setText(getString(R.string.upgrade_to_pro));
            mTryForFreeTv.setVisibility(View.GONE);
            mClaimTextView.setVisibility(View.VISIBLE);
            mClaimTextView.setText(getString(R.string.purchase_claim_lifetime));

        } else {
            Sku.PriceInfo priceInfo = sku.getPriceInfo();
            Currency currency = Currency.getInstance(priceInfo.currencyCode);
            BillingPeriod billingPeriod = sku.getBillingPeriod();
            DecimalFormat df = new DecimalFormat("0.00");
            String priceWithPeriod = IabStringUtil.convertToPriceEachPeriod(this, billingPeriod,
                    currency.getSymbol().toUpperCase() + df.format(priceInfo.value));
            mClaimTextView.setVisibility(View.VISIBLE);
            if (sku.isSupportFreeTrial()) {
                mPurchaseBtn.setText(getString(R.string.days_trial, sku.getFreeTrialDays()));
                mTryForFreeTv.setVisibility(View.VISIBLE);
                mTryForFreeTv.setText(getString(R.string.try_for_free_tips,
                        String.valueOf(sku.getFreeTrialDays()), priceWithPeriod));
                mClaimTextView.setText(getString(R.string.purchase_claim_subs_with_free_trial, priceWithPeriod));

            } else {
                mPurchaseBtn.setText(getString(R.string.upgrade_to_pro));
                mTryForFreeTv.setVisibility(View.GONE);
                mClaimTextView.setText(getString(R.string.purchase_claim_subs_without_free_trial, priceWithPeriod));
            }
        }
    }

    @Override
    public void showLoadingPriceFailed(IabController.BillingError billingError) {
        Toast.makeText(this, getString(R.string.load_price_error), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void showRefreshingLicense() {
        showProgressDialog(getString(R.string.loading), DIALOG_TAG_REFRESHING_LICENSE);
    }

    @Override
    public void showRefreshLicenseFailed(@NonNull String skuGroup, int errorCode, String data) {
        Toast.makeText(this, getString(R.string.restore_failed), Toast.LENGTH_LONG).show();
    }

    @Override
    public void dismissRefreshingLicense() {
        dismissProgressDialog(DIALOG_TAG_REFRESHING_LICENSE);
    }

    @Override
    public void showConfirmingPurchase() {
        showProgressDialog(getString(R.string.loading), DIALOG_TAG_CONFIRMING_LICENSE);
    }

    @Override
    public void dismissConfirmingPurchase() {
        dismissProgressDialog(DIALOG_TAG_CONFIRMING_LICENSE);
    }

    @Override
    public void showNoProPurchasedMessage() {
        Toast.makeText(getApplicationContext(), R.string.toast_no_pro_purchased, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showDowngradePrompt(String skuGroup, DowngradeType downgradeType, String pausedSkuId) {
        Toast.makeText(this, R.string.license_downgraded, Toast.LENGTH_LONG).show();

        if (downgradeType == DowngradeType.SubsToFreePaused) {
            showLicensePaused(skuGroup);
        }
    }

    @Override
    public void showLicensePaused(String skuId) {
        android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(R.string.dialog_message_license_paused_to_resume)
                .setPositiveButton(R.string.dialog_button_resume, (dialog, which) -> {
                    goToGooglePlayToSeePausedSubsProduct(skuId);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        alertDialog.setOwnerActivity(this);
        alertDialog.show();
    }

    public void goToGooglePlayToSeePausedSubsProduct(String pausedSkdId) {
        Intent intent = IabPurchaseUtil.getGoToGooglePlayToViewPausedSubsProductIntent(pausedSkdId, getPackageName());
        mResumeLicenseActivityResultLauncher.launch(intent);
    }

    private void showProgressDialog(String message, String tag) {
        ProgressDialogFragment.newInstance(message)
                .show(getSupportFragmentManager(), tag);
    }

    private void dismissProgressDialog(String tag) {
        ProgressDialogFragment dialogFragment = (ProgressDialogFragment) getSupportFragmentManager().findFragmentByTag(tag);
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss();
        }
    }

    public static class ProgressDialogFragment extends DialogFragment {

        private static final String KEY_MESSAGE = "message";

        public static ProgressDialogFragment newInstance(String message) {
            Bundle args = new Bundle();
            args.putString(KEY_MESSAGE, message);
            ProgressDialogFragment progressDialogFragment = new ProgressDialogFragment();
            progressDialogFragment.setArguments(args);
            return progressDialogFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            @SuppressLint("InflateParams") View view = LayoutInflater.from(requireContext()).inflate(R.layout.view_processing, null);
            TextView messageTextView = view.findViewById(R.id.tv_message);
            messageTextView.setText(requireArguments().getString(KEY_MESSAGE));
            return new AlertDialog.Builder(requireContext()).setView(view).create();
        }
    }
}
