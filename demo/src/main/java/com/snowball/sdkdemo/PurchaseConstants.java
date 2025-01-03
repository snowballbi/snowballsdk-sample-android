package com.snowball.sdkdemo;

public class PurchaseConstants {

    public static final String BASE64_PLAY_API_PUBLIC_KEY = "YOUR_PLAY_API_KEY"; // 借助 Play 变现 -> 创收设置 -> 许可

    public static final String DEFAULT_SKU_LIST = "{\n" +
            "\t\"iab_product_items\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"iab_item_type\": \"subs\",\n" +
            "\t\t\t\"product_item_id\": \"snowball_sdk_demo.subscription_1m_01\",\n" +
            "\t\t\t\"subscription_period\": \"1m\",\n" +
            "\t\t\t\"support_free_trial\": true,\n" +
            "\t\t\t\"free_trial_days\": 3\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"iab_item_type\": \"subs\",\n" +
            "\t\t\t\"product_item_id\": \"snowball_sdk_demo.subscription_1y_01\",\n" +
            "\t\t\t\"subscription_period\": \"1Y\",\n" +
            "\t\t\t\"support_free_trial\": false,\n" +
            "\t\t\t\"discount_percent\": 0.45\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"iab_item_type\": \"iap\",\n" +
            "\t\t\t\"product_item_id\": \"snowball_sdk_demo.inapp_lifetime_01\",\n" +
            "\t\t\t\"discount_percent\": 0.7\n" +
            "\t\t}\n" +
            "\t],\n" +
            "\t\"recommended_iab_item_id\": \"snowball_sdk_demo.subscription_1m_01\"\n" +
            "}";

    public static final String PURCHASE_HOST_NAME = "your_purchase_server.com";
}
