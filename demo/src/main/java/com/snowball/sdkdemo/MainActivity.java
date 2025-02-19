package com.snowball.sdkdemo;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.messaging.FirebaseMessaging;
import com.snowball.core.common.FrcHelper;
import com.snowball.core.common.SnowBallLog;
import com.snowball.core.tracker.SnowBallTracker;
import com.snowball.core.tracker.ads.AdType;
import com.snowball.purchase.SnowBallLicenseController;
import com.snowball.purchase.callback.ConsumePurchaseCallback;
import com.snowball.purchase.callback.RefreshLicenseCallback;
import com.snowball.purchase.model.BillingError;
import com.snowball.purchase.model.LicenseChangeType;
import com.snowball.purchase.model.LicenseChangedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends ComponentActivity {
    private static final SnowBallLog gDebug = SnowBallLog.createCommonLogger("MainActivity");
    private TextView mTextViewPushToken;
    private TextView mTextViewLicenseStatus;
    private Button mEnableFrcTestModeButton;
    private Button mDisbaleFrcTestModeButton;

    private String mPushInstanceToken;

    private final FrcHelper.FrcCallback mFrcCallback = new FrcHelper.FrcCallback() {
        @Override
        public void onReady() {
            gDebug.d("onReady called");
            queryFirebaseRemoteConfigId();
        }

        @Override
        public void onRefreshed() {
            gDebug.d("onRefreshed called");
            queryFirebaseRemoteConfigId();
        }

        @Override
        public void onRefreshedButVersionIdNotChanged() {
            gDebug.d("onRefreshedButVersionIdNotChanged called");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FrcHelper.refresh(); // Suggest calling this on your first open activity

        FrcHelper.addCallback(mFrcCallback);

        initViews();

        initAdmob();

        checkLicense();
    }

    @Override
    protected void onDestroy() {
        FrcHelper.removeCallback(mFrcCallback);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void checkLicense() {
        // This will only check once one day.
        SnowBallLicenseController.getInstance().refreshWhenOpenMainUI(new RefreshLicenseCallback() {
            @Override
            public void onRefreshLicenseSuccess(@NonNull String skuGroup, @NonNull LicenseChangeType licenseChangeType, @Nullable String pausedSkuId) {
                if (licenseChangeType.isDowngrade()) {
                    Toast.makeText(MainActivity.this, getString(R.string.license_downgraded), Toast.LENGTH_LONG).show();
                } else if (licenseChangeType.isUpgrade()) {
                    Toast.makeText(MainActivity.this, getString(R.string.license_upgraded), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onRefreshLicenseFailed(@NonNull String skuGroup, int errorCode, @Nullable String data) {
                gDebug.e("onRefreshLicenseFailed, errorCode:" + errorCode + ", data: " + data);
            }
        });
    }

    private void initViews() {
        Button btnShowAds = findViewById(R.id.btn_show_ads);
        btnShowAds.setOnClickListener((v) -> {
            //Send a test event
            SnowBallTracker.getInstance().sendEvent("TestShowAds", null);

            loadAndShowDemoAds();
        });

        mTextViewPushToken = findViewById(R.id.user_push_token_text_view);
        mTextViewLicenseStatus = findViewById(R.id.license_status_textview);

        Button btnCopyPushToken = findViewById(R.id.btn_copy_push_token);
        btnCopyPushToken.setOnClickListener((v) -> {
            if (mPushInstanceToken == null) {
                queryFirebasePushToken(true);
            } else {
                copyPushToken();
            }
        });

        Button btnOpenUpgradePage = findViewById(R.id.btn_open_purchase_page);
        btnOpenUpgradePage.setOnClickListener((v) -> startActivity(new Intent(MainActivity.this, LicenseUpgradeActivity.class)));

        Button btnConsumeLifetimePurchase = findViewById(R.id.btn_consume_lifetime_purchase);
        // Only for test to cancel lifetime purchase
        btnConsumeLifetimePurchase.setOnClickListener((v) -> SnowBallLicenseController.getInstance().consumeAllInappPurchases(new ConsumePurchaseCallback() {
            @Override
            public void onConsumed(@NonNull String purchaseToken, @NonNull String skuId, @Nullable String orderId) {
                Toast.makeText(MainActivity.this, "Consumed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull BillingError error) {
                Toast.makeText(MainActivity.this, "Error: " + error.name(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoInappPurchase() {
                Toast.makeText(MainActivity.this, "onNoInappPurchase", Toast.LENGTH_SHORT).show();
            }
        }));

        findViewById(R.id.btn_refresh_frc).setOnClickListener(v -> FrcHelper.refresh());

        View.OnClickListener onClickListener = v -> {
            boolean enabled = !FrcHelper.isTestModeEnabled();
            FrcHelper.setTestModeEnabled(enabled);
            Toast.makeText(this, enabled ? "Frc Test Mode Enabled" : "Frc Test Mode Disabled", Toast.LENGTH_SHORT).show();
            refreshTestModeButtonStatus();
        };
        mEnableFrcTestModeButton = findViewById(R.id.btn_enable_frc_test_mode);
        mEnableFrcTestModeButton.setOnClickListener(onClickListener);
        mDisbaleFrcTestModeButton = findViewById(R.id.btn_disable_frc_test_mode);
        mDisbaleFrcTestModeButton.setOnClickListener(onClickListener);

        refreshTestModeButtonStatus();

        queryFirebasePushToken(false);

        queryFirebaseRemoteConfigId();

        refreshLicenseTextView();
    }

    private void refreshLicenseTextView() {
        mTextViewLicenseStatus.setText("License Status: " + (SnowBallLicenseController.getInstance().isProLicense() ? "Pro" : "Free"));
    }

    private void refreshTestModeButtonStatus() {
        boolean isTestModeEnabled = FrcHelper.isTestModeEnabled();
        mEnableFrcTestModeButton.setVisibility(isTestModeEnabled ? View.GONE : View.VISIBLE);
        mDisbaleFrcTestModeButton.setVisibility(isTestModeEnabled ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("SetTextI18n")
    private void queryFirebaseRemoteConfigId() {
        TextView frcVersionIdTextView = findViewById(R.id.frc_version_id_textView);
        frcVersionIdTextView.setText("Firebase Remote Config Version Id: " + FrcHelper.getVersionId());
    }

    private void copyPushToken() {
        String pushInstanceToken = mPushInstanceToken;
        if (!TextUtils.isEmpty(pushInstanceToken)) {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, pushInstanceToken));
            Toast.makeText(MainActivity.this, "Already copied to ClipBoard.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "PushInstanceToken is not found", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void queryFirebasePushToken(boolean autoCopy) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        gDebug.e("Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    gDebug.d("Refreshed token: " + token);
                    mPushInstanceToken = token;
                    if (mTextViewPushToken != null) {
                        mTextViewPushToken.setText("Push Token: " + mPushInstanceToken);

                        if (autoCopy) {
                            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, mPushInstanceToken));
                            Toast.makeText(MainActivity.this, "Already copied to ClipBoard.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        gDebug.e("mPushInstanceTokenOperationItem == null");
                        if (autoCopy) {
                            Toast.makeText(MainActivity.this, "PushInstanceToken is not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadAndShowDemoAds() {
        loadAdmobIntersAds();
    }


    private void initAdmob() {
        MobileAds.initialize(this, initializationStatus -> gDebug.d("Admob initialized: " + initializationStatus));
    }

    private InterstitialAd mInterstitialAd;

    private void loadAdmobIntersAds() {
        AdRequest adRequest = new AdRequest.Builder().build();

        Toast.makeText(this, "Loading Ads ...", Toast.LENGTH_SHORT).show();
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        gDebug.i("onAdLoaded");

                        showAdmobIntersAds();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        //Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                        Toast.makeText(MainActivity.this, "Loading Ads Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAdmobIntersAds() {
        String adsScene = "MainPage";
        if (mInterstitialAd != null) {
            InterstitialAd interstitialAd = mInterstitialAd;

            interstitialAd.setOnPaidEventListener(adValue ->
                    AdmobILRDReportHelper.reportAdsEvent(this,
                            AdType.INTERSTITIAL,
                            interstitialAd.getAdUnitId(),
                            interstitialAd.getResponseInfo(),
                            adValue,
                            adsScene
                    ));

            mInterstitialAd.show(MainActivity.this);
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLicenseChange(LicenseChangedEvent licenseChangedEvent) {
        refreshLicenseTextView();
        gDebug.d("onLicenseChange, isPro:" + SnowBallLicenseController.getInstance().isProLicense());
    }
}