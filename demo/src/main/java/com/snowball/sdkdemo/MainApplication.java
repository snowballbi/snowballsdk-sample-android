package com.snowball.sdkdemo;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.snowball.common.AppContext;
import com.snowball.common.SnowBallLog;
import com.snowball.purchase.business.SnowBallLicenseController;
import com.snowball.purchase.business.license.SnowBallLicenseConfigure;
import com.snowball.push.IPushReceiverHandler;
import com.snowball.push.SnowBallPush;
import com.snowball.push.constants.PushConstants;
import com.snowball.tracker.SnowBallTracker;
import com.snowball.tracker.attribution.AttributionInfo;
import com.snowball.tracker.attribution.AttributionSource;

import org.json.JSONObject;

import java.util.Map;

public class MainApplication extends Application {

    private final static SnowBallLog gDebug = SnowBallLog.createCommonLogger("MainApplication");

    @Override
    public void onCreate() {
        super.onCreate();
        AppContext.set(this); // Used in snowball sdk. Must be called.

        initSnowBallTracker();
        initSnowBallLicense(); // optional
        initSnowBallPush();
        reportAttributeFromAdjust(); // optional
    }

    private void initSnowBallTracker() {
        SnowBallTracker.getInstance().init(this);
    }

    private void initSnowBallPush() {
        SnowBallPush.getInstance(this).init(() -> SnowBallLicenseController.getInstance().isProLicense(), new IPushReceiverHandler() {
            @Override
            public boolean handlePushData(@NonNull Context context,
                                          @NonNull String pushMessageId,
                                          @NonNull JSONObject pushData,
                                          boolean highPriority) {

                String actionType = pushData.optString(PushConstants.PUSH_DATA_KEY_ACTION);
                gDebug.d("handlePushData, action type:" + actionType + ", data:" + pushData);
                // handle your custom action operations here
                return true;
            }

            @Override
            public boolean handlePushNotificationOnForeground(@NonNull Context context, String title, String body, @NonNull Map<String, String> pushData) {
                String actionType = pushData.get(PushConstants.PUSH_DATA_KEY_ACTION);
                gDebug.d("handlePushData, action type: " + actionType + ", title: " + title + ", boday: " + body);
                // handle your custom action operations here
                return true;
            }
        });

        //subscribe any app custom topic if needed
//        SnowBallPush.getInstance(this).subscribeToTopic("PUSH_TOPIC_SAMPLE");
    }

    private void initSnowBallLicense() {
        SnowBallLicenseController.getInstance().init(new SnowBallLicenseConfigure.LicenseInitCallback() {

            @Override
            public String getPlayBillingBase64ApiPublicKey() {
                return PurchaseConstants.BASE64_PLAY_API_PUBLIC_KEY;
            }

            @Override
            public String getStoreApiServerHostName() {
                return PurchaseConstants.PURCHASE_HOST_NAME;
            }
        });
    }

    private void reportAttributeFromAdjust() {
        String appToken = "your_app_token_for_adjust";
        AdjustConfig config = new AdjustConfig(this, appToken, AdjustConfig.ENVIRONMENT_PRODUCTION);
        config.setOnAttributionChangedListener(attribution -> {
            AttributionInfo attributionInfo = new AttributionInfo(AttributionSource.Adjust,
                    attribution.trackerToken,
                    attribution.trackerName,
                    attribution.network,
                    attribution.campaign,
                    attribution.adgroup,
                    attribution.creative,
                    attribution.clickLabel,
                    attribution.costType,
                    attribution.costAmount,
                    attribution.costCurrency
            );

            SnowBallTracker.getInstance().reportAttributionInfo(MainApplication.this, attributionInfo);
        });
        Adjust.initSdk(config);
    }
}
