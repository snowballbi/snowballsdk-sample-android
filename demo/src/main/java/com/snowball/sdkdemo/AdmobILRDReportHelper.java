package com.snowball.sdkdemo;

import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.ResponseInfo;
import com.snowball.common.SnowBallLog;
import com.snowball.tracker.SnowBallTracker;
import com.snowball.tracker.ads.AdConstants;
import com.snowball.tracker.ads.AdType;
import com.snowball.tracker.ads.ILRDInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

class AdmobILRDReportHelper {

    private static final SnowBallLog gDebug = SnowBallLog.createCommonLogger("AdmobILRDReportHelper");

    static void reportAdsEvent(Context context, AdType adType, String adUnitId,
                              ResponseInfo responseInfo, @NonNull AdValue adValue, @NonNull String scene) {
        gDebug.d("==> reportILRD, adType: " + adType + ", adUnitId: " + adUnitId);

        String adapterCredentials = null;
        if (responseInfo != null) {
            adapterCredentials = getAdapterCredentials(responseInfo);
        }

        final String networkName = getNetworkName(responseInfo);

        ILRDInfo ilrdInfo = new ILRDInfo(
                AdConstants.Mediation.Admob,
                AdConstants.RevenueFrom.AdmobPingback,
                responseInfo != null ? responseInfo.getResponseId() : UUID.randomUUID().toString(),
                getRegion(context),
                networkName != null ? networkName : "Unknown",
                adUnitId,
                get3rdPartyPlacementId(networkName, adType, responseInfo),
                adType,
                adValue.getCurrencyCode(),
                adValue.getValueMicros() * 1.0 / 100_0000,
                getRevenuePrecisionName(adValue.getPrecisionType()),
                scene,
                adapterCredentials
        );

        SnowBallTracker.getInstance().trackAdRevenue(context, ilrdInfo);
    }

    private static String getRegion(Context context) {
        String region = getRegionBySim(context);
        if (!TextUtils.isEmpty(region)) {
            return region;

        } else {
            return Locale.getDefault().getCountry().toUpperCase();
        }
    }

    private static String getRegionBySim(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            return null;
        }

        if (TextUtils.isEmpty(tm.getSimCountryIso())) {
            return null;
        }

        String region = tm.getNetworkCountryIso();
        if (!TextUtils.isEmpty(region)) {
            return region.toUpperCase();
        }
        return null;
    }

    private static String getAdapterCredentials(@NonNull ResponseInfo responseInfo) {
        AdapterResponseInfo adapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();
        if (adapterResponseInfo == null) {
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        Bundle bundle = adapterResponseInfo.getCredentials();
        try {
            for (String key : bundle.keySet()) {
                jsonObject.put(key, Objects.requireNonNull(bundle.get(key)).toString());
            }
            return jsonObject.toString();
        } catch (JSONException e) {
            gDebug.e(e);
            return null;
        }
    }

    @Nullable
    private static String getNetworkName(ResponseInfo responseInfo) {
        if (responseInfo == null) {
            return null;
        }

        final String adapterClassName = responseInfo.getMediationAdapterClassName();
        if (adapterClassName == null) {
            return null;
        }

        String networkName = adapterClassName;
        if (AdMobAdapter.class.getName().equals(adapterClassName)) {
            networkName = AdConstants.Network.Admob;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.facebook")) {
            networkName = AdConstants.Network.Facebook;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.applovin")) {
            networkName = AdConstants.Network.Applovin;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.fyber")) {
            networkName = AdConstants.Network.Fyber;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.ironsource")) {
            networkName = AdConstants.Network.IronSource;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.inmobi")) {
            networkName = AdConstants.Network.Inmobi;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.tapjoy")) {
            networkName = AdConstants.Network.Tapjoy;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.unity")) {
            networkName = AdConstants.Network.Unity;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.vungle")) {
            networkName = AdConstants.Network.Vungle;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.pangle")) {
            networkName = AdConstants.Network.Pangle;

        } else if (adapterClassName.startsWith("com.google.ads.mediation.thgoogleadmanager")) {
            networkName = AdConstants.Network.GoogleAdManager;

        } else {
            String prefix = "com.google.ads.mediation.";
            if (adapterClassName.startsWith(prefix) && adapterClassName.length() > prefix.length()) {
                String str = adapterClassName.substring("com.google.ads.mediation.".length());
                String[] parts = str.split("\\.");
                if (parts.length > 0) {
                    networkName = parts[0];
                }
            }
        }
        return networkName;
    }

    @Nullable
    private static String get3rdPartyPlacementId(String networkName, AdType adType, ResponseInfo responseInfo) {
        if (networkName == null || responseInfo == null) {
            return null;
        }

        AdapterResponseInfo adapterResponseInfo = responseInfo.getLoadedAdapterResponseInfo();
        if (adapterResponseInfo == null) {
            return null;
        }

        Bundle bundle = adapterResponseInfo.getCredentials();
        /*
         * Network Name: pangle, Credential: {"class_name":"com.google.ads.mediation.pangle.customevent.PangleCustomEvent","label":"Pangle-N1","parameter":"980273012"}
        */
        String placementId = null;
        if (AdConstants.Network.Admob.equalsIgnoreCase(networkName)) {
            gDebug.d("No need to get 3rd party placement id for Admob");

        } else if (AdConstants.Network.Pangle.equalsIgnoreCase(networkName)) {
            placementId = bundle.getString("placementid");
            if (TextUtils.isEmpty(placementId)) {
                placementId = bundle.getString("parameter");
            }

        } else if (AdConstants.Network.GoogleAdManager.equalsIgnoreCase(networkName)) {
            String parameter = bundle.getString("parameter");
            if (!TextUtils.isEmpty(parameter)) {
                try {
                    JSONObject jsonObject = new JSONObject(parameter);
                    placementId = jsonObject.optString("ad_unit_id");
                } catch (JSONException e) {
                    gDebug.e(e);
                }
            }

        } else if (AdConstants.Network.Facebook.equalsIgnoreCase(networkName)) {
            placementId = bundle.getString("placement_id");

        } else if (AdConstants.Network.Applovin.equalsIgnoreCase(networkName)) {
            if (adType == AdType.Interstitial) {
                placementId = "INTER";
            }

        } else {
            gDebug.i("Unrecognized credentials, network: " + networkName + ", credentials: " + bundle);
        }
        return placementId;
    }

    /**
     * PrecisionType to name of String
     *
     * @param type defined by Admob
     * @return the name of String
     */
    @NonNull
    private static String getRevenuePrecisionName(@AdValue.PrecisionType int type) {
        switch (type) {
            case AdValue.PrecisionType.ESTIMATED:
                return AdConstants.RevenuePrecision.Estimated;

            case AdValue.PrecisionType.PUBLISHER_PROVIDED:
                return AdConstants.RevenuePrecision.PublisherDefined;

            case AdValue.PrecisionType.PRECISE:
                return AdConstants.RevenuePrecision.Exact;

            case AdValue.PrecisionType.UNKNOWN:
            default:
                return AdConstants.RevenuePrecision.Undefined;
        }
    }
}
