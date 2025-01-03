package com.snowball.sdkdemo;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowball.common.AppContext;
import com.snowball.common.SnowBallLog;
import com.snowball.common.SnowBallUtils;
import com.snowball.purchase.business.LicenseRefresher;
import com.snowball.purchase.business.SnowBallLicenseController;
import com.snowball.purchase.business.iab.IabController;
import com.snowball.purchase.business.iab.model.SkuListSummary;
import com.snowball.purchase.business.iab.model.Sku;
import com.snowball.purchase.business.license.model.DowngradeType;
import com.snowball.purchase.business.license.model.LicenseChangeType;
import com.snowball.purchase.business.license.model.PurchaseData;
import com.snowball.purchase.business.license.model.PurchaseError;
import com.snowball.purchase.business.license.model.RefreshLicenseParam;
import com.snowball.purchase.business.license.model.SkuType;

import java.util.List;

public class LicenseUpgradeModel {

    private static final SnowBallLog gDebug = SnowBallLog.createCommonLogger("LicenseUpgradeModel");

    private final Context mContext;
    private final Callback mCallback;

    public interface Callback {

        boolean isViewFinishing();

        void showNoNetworkMessage();

        void showPurchaseFailed(@NonNull PurchaseError purchaseError, @Nullable String data);

        void showProView(PurchaseData purchaseData);

        void showLicenseUpgradedPrompt(SkuType skuType);

        void showLoadingPriceView();

        void dismissLoadingPriceView();

        void showPriceList(List<Sku> skuList, SkuListSummary summary);

        void showLoadingPriceFailed(IabController.BillingError billingError);

        void showRefreshingLicense();

        void showRefreshLicenseFailed(@NonNull String skuGroup, int errorCode, String data);

        void dismissRefreshingLicense();

        void showConfirmingPurchase();

        void dismissConfirmingPurchase();

        void showNoProPurchasedMessage();

        void showDowngradePrompt(String skuGroup, DowngradeType downgradeType, String pausedSkuId);

        void showLicensePaused(String skuId);

    }

    public LicenseUpgradeModel(Context context, Callback callback) {
        mContext = context;
        mCallback = callback;
    }

    public void loadData(boolean shouldAutoRefreshLicenseInBackground) {
        if (mCallback.isViewFinishing()) {
            return;
        }
        if (SnowBallLicenseController.getInstance().isProLicense()) {
            mCallback.showProView(SnowBallLicenseController.getInstance().getPurchaseData());

        } else {
            loadPrice();
        }

        if (shouldAutoRefreshLicenseInBackground) {
            refreshLicenseData(false);
        }
    }

    public void refreshLicenseData(boolean isRestoreLicenseManually) {
        if (mCallback.isViewFinishing()) {
            return;
        }

        if (!SnowBallUtils.isNetworkAvailable(mContext)) {
            mCallback.showNoNetworkMessage();
            return;
        }

        if (isRestoreLicenseManually) {
            mCallback.showRefreshingLicense();
        }

        RefreshLicenseParam refreshLicenseParam = RefreshLicenseParam.newBuilder()
                .forceRefresh(true)
                .checkUpgrade(true)
                .packageName(mContext.getPackageName())
                .shouldCheckLicenseByAdidAndFirebaseId(isRestoreLicenseManually)
                .build();
        SnowBallLicenseController.getInstance().refresh(refreshLicenseParam, new LicenseRefresher.Callback() {
            @Override
            public void onRefreshLicenseSuccess(@NonNull String skuGroup, @NonNull LicenseChangeType licenseChangeType, @Nullable String pausedSkuId) {
                if (mCallback.isViewFinishing()) {
                    return;
                }
                if (isRestoreLicenseManually) {
                    mCallback.dismissRefreshingLicense();
                }
                covertToCallbackAfterRefreshLicenseSuccessfully(skuGroup, licenseChangeType, pausedSkuId, isRestoreLicenseManually);
            }

            @Override
            public void onRefreshLicenseFailed(@NonNull String skuGroup, int errorCode, String data) {
                if (mCallback.isViewFinishing()) {
                    return;
                }
                if (isRestoreLicenseManually) {
                    mCallback.dismissRefreshingLicense();
                }
                mCallback.showRefreshLicenseFailed(skuGroup, errorCode, data);
            }
        });
    }

    private void covertToCallbackAfterRefreshLicenseSuccessfully(@NonNull String skuGroup, @NonNull LicenseChangeType licenseChangeType, @Nullable String pausedSkuId, boolean isRestoreLicenseManually) {
        if (mCallback.isViewFinishing()) {
            return;
        }
        PurchaseData purchaseData = SnowBallLicenseController.getInstance().getPurchaseData();
        if (licenseChangeType == LicenseChangeType.Upgrade) {
            if (purchaseData == null) {
                gDebug.e("license info not be null");
                if (isRestoreLicenseManually) {
                    mCallback.showNoProPurchasedMessage();
                }
                return;
            }
            mCallback.showLicenseUpgradedPrompt(purchaseData.getSkuType());
            mCallback.showProView(purchaseData);

        } else if (licenseChangeType.isDowngrade()) {
            loadData(false);
            mCallback.showDowngradePrompt(skuGroup, licenseChangeType.getDowngradeType(), pausedSkuId);

        } else if (licenseChangeType == LicenseChangeType.DataChange) {
            if (purchaseData == null) {
                gDebug.e("license info not be null");
                if (isRestoreLicenseManually) {
                    mCallback.showNoProPurchasedMessage();
                }
                return;
            }

            if (purchaseData.isActive()) {
                mCallback.showProView(purchaseData);
            }

        } else {
            if (purchaseData != null && purchaseData.isPaused()) {
                mCallback.showLicensePaused(purchaseData.getSkuId());
            } else {
                if (isRestoreLicenseManually) {
                    mCallback.showNoProPurchasedMessage();
                }
            }
        }
    }

    private void loadPrice() {
        mCallback.showLoadingPriceView();
        SkuListSummary listSummary;
        String skuListConfigJson = PurchaseConstants.DEFAULT_SKU_LIST; // You can fetch it from remote config instead
        gDebug.d("sku list config:" + skuListConfigJson);
        listSummary = IabController.parseIabSubProductItemsFromJson(skuListConfigJson);
        IabController.getInstance().queryIabSku(listSummary, new IabController.QuerySkuCallback() {
            @Override
            public void onQuerySkuFinished(List<Sku> skuItemList, SkuListSummary skuListSummary) {
                gDebug.d("==> loadPlayIabProductSku, onQueryIabProductSkuFinished");
                if (SnowBallLicenseController.getInstance().isProLicense()) {
                    return;
                }

                AppContext.runOnMainUiThread(() -> {
                    if (mCallback.isViewFinishing()) {
                        return;
                    }

                    if (SnowBallLicenseController.getInstance().isProLicense()) {
                        return;
                    }

                    mCallback.dismissLoadingPriceView();
                    mCallback.showPriceList(skuItemList, skuListSummary);
                });
            }

            @Override
            public void onQueryError(IabController.BillingError billingError) {
                gDebug.e("load pab iab items sku failed, error " + billingError);
                if (SnowBallLicenseController.getInstance().isProLicense()) {
                    return;
                }

                AppContext.runOnMainUiThread(() -> {
                    if (mCallback.isViewFinishing()) {
                        return;
                    }

                    mCallback.dismissLoadingPriceView();
                    mCallback.showLoadingPriceFailed(billingError);
                });
            }
        });
    }

    public void startPurchasing(@NonNull Sku sku, String purchaseScene) {
        gDebug.d("purchase, sku: " + sku);
        if (mCallback.isViewFinishing()) {
            return;
        }

        if (!SnowBallUtils.isNetworkAvailable(mContext)) {
            mCallback.showNoNetworkMessage();
            return;
        }

        PurchaseData purchaseData = SnowBallLicenseController.getInstance().getPurchaseData();

        if (purchaseData != null && purchaseData.isActive()) {
            gDebug.d("License has already been Pro, skip the purchase action and refresh ui");
            mCallback.showLicenseUpgradedPrompt(purchaseData.getSkuType());
            return;
        }

        IabController.getInstance().purchase((Activity) mContext, sku, purchaseScene, new IabController.PurchaseCallback() {
            @Override
            public void purchaseSuccessfully(PurchaseData purchaseData) {
                if (mCallback.isViewFinishing()) {
                    return;
                }
                mCallback.showLicenseUpgradedPrompt(purchaseData.getSkuType());
                mCallback.showProView(purchaseData);
            }

            @Override
            public void showPurchaseFailed(PurchaseError purchaseError, @Nullable String data) {
                if (mCallback.isViewFinishing()) {
                    return;
                }
                mCallback.showPurchaseFailed(purchaseError, data);
            }

            @Override
            public void beginConfirmingPurchase() {
                if (mCallback.isViewFinishing()) {
                    return;
                }
                mCallback.showConfirmingPurchase();
            }

            @Override
            public void endConfirmingPurchase() {
                if (mCallback.isViewFinishing()) {
                    return;
                }
                mCallback.dismissConfirmingPurchase();
            }
        });
    }

}
