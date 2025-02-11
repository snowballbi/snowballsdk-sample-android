package com.snowball.sdkdemo;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowball.core.common.AppContext;
import com.snowball.core.common.FrcHelper;
import com.snowball.core.common.SnowBallLog;
import com.snowball.purchase.SnowBallLicenseController;
import com.snowball.purchase.callback.PurchaseCallback;
import com.snowball.purchase.callback.QuerySkuCallback;
import com.snowball.purchase.callback.RefreshLicenseCallback;
import com.snowball.purchase.model.BillingError;
import com.snowball.purchase.model.DowngradeType;
import com.snowball.purchase.model.LicenseChangeType;
import com.snowball.purchase.model.PurchaseData;
import com.snowball.purchase.model.PurchaseError;
import com.snowball.purchase.model.RefreshLicenseParam;
import com.snowball.purchase.model.Sku;
import com.snowball.purchase.model.SkuListSummary;
import com.snowball.purchase.model.SkuType;

import java.util.List;

public class LicenseUpgradeModel {

    private static final SnowBallLog gDebug = SnowBallLog.createCommonLogger("LicenseUpgradeModel");

    private final Context mContext;
    private final Callback mCallback;

    public interface Callback {

        boolean isViewFinishing();

        void showNoNetworkMessage();

        void showPurchaseFailed(@NonNull PurchaseError purchaseError, @Nullable String data);

        void showProView(@NonNull PurchaseData purchaseData);

        void showLicenseUpgradedPrompt(@NonNull SkuType skuType);

        void showLoadingPriceView();

        void dismissLoadingPriceView();

        void showPriceList(@NonNull List<Sku> skuList, @NonNull SkuListSummary summary);

        void showLoadingPriceFailed(@NonNull BillingError billingError);

        void showRefreshingLicense();

        void showRefreshLicenseFailed(@NonNull String skuGroup, int errorCode, @Nullable String data);

        void dismissRefreshingLicense();

        void showConfirmingPurchase();

        void dismissConfirmingPurchase();

        void showNoProPurchasedMessage();

        void showDowngradePrompt(@NonNull String skuGroup, @NonNull DowngradeType downgradeType, @Nullable String pausedSkuId);

        void showLicensePaused(@NonNull String skuId);

    }

    public LicenseUpgradeModel(@NonNull Context context, @NonNull Callback callback) {
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

        if (!Utils.isNetworkAvailable(mContext)) {
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
        SnowBallLicenseController.getInstance().refresh(refreshLicenseParam, new RefreshLicenseCallback() {
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
        if (licenseChangeType == LicenseChangeType.UPGRADE) {
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

        } else if (licenseChangeType == LicenseChangeType.DATA_CHANGE) {
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
        String skuListConfigJson = FrcHelper.getString("sku_list");
        if (TextUtils.isEmpty(skuListConfigJson)) {
            skuListConfigJson = PurchaseConstants.DEFAULT_SKU_LIST;
        }
        gDebug.d("sku list config:" + skuListConfigJson);
        SnowBallLicenseController.getInstance().queryIabSku(skuListConfigJson, new QuerySkuCallback() {
            @Override
            public void onQuerySkuFinished(@NonNull List<Sku> skuItemList, @NonNull SkuListSummary skuListSummary) {
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
            public void onQueryError(@NonNull BillingError billingError) {
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

        if (!Utils.isNetworkAvailable(mContext)) {
            mCallback.showNoNetworkMessage();
            return;
        }

        PurchaseData purchaseData = SnowBallLicenseController.getInstance().getPurchaseData();

        if (purchaseData != null && purchaseData.isActive()) {
            gDebug.d("License has already been Pro, skip the purchase action and refresh ui");
            mCallback.showLicenseUpgradedPrompt(purchaseData.getSkuType());
            return;
        }

        SnowBallLicenseController.getInstance().purchase((Activity) mContext, sku, purchaseScene, new PurchaseCallback() {
            @Override
            public void purchaseSuccessfully(@NonNull PurchaseData purchaseData) {
                if (mCallback.isViewFinishing()) {
                    return;
                }
                mCallback.showLicenseUpgradedPrompt(purchaseData.getSkuType());
                mCallback.showProView(purchaseData);
            }

            @Override
            public void showPurchaseFailed(@NonNull PurchaseError purchaseError, @Nullable String data) {
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
