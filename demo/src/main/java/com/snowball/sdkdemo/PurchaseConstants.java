package com.snowball.sdkdemo;

public class PurchaseConstants {

    public static final String BASE64_PLAY_API_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgAaZIsQeiTAahNy4WJvWl3aikZ6t4V+v05695N4LdkqtCuE2gqb76nFvVl0bGFXckH7yXEEE1Kr3oajsKBd/xnJE6B/ZU0lkU549GAkRVGWzR/At24+0yDHA1WbOb5CwFbslXH/fJ6P5XOh08eOewdjb6pCIUQW4h2hueAvDovLMMjob7uUDQkaWFfIJtS+IyzmXkYwASXN0wmkzS8VcJJN6H8TB88I5PeMJz0COn/oUvipSUto7BdKoLHiXd21TU6mNLiqSs3UYV07cuG99Xmb/P+G225IHAZMCbqWYZaZP5J6MgwLKebAFBlbpC+D+8KF8eDAhzs2FmxsOJ6ODlwIDAQAB";

    public static final String DEFAULT_SKU_LIST = "{\n" +
            "  \"iab_product_items\": [\n" +
            "    {\n" +
            "      \"iab_item_type\": \"subs\",\n" +
            "      \"product_item_id\": \"sdownloader.subscription_1m_01\",\n" +
            "      \"subscription_period\": \"1m\",\n" +
            "      \"support_free_trial\": false\n" +
            "    },\n" +
            "    {\n" +
            "      \"iab_item_type\": \"subs\",\n" +
            "      \"product_item_id\": \"sdownloader.subscription_1y_01\",\n" +
            "      \"subscription_period\": \"1y\",\n" +
            "      \"support_free_trial\": false,\n" +
            "      \"free_trial_days\": 3,\n" +
            "      \"discount_percent\": 0.62\n" +
            "    },\n" +
            "    {\n" +
            "      \"iab_item_type\": \"iap\",\n" +
            "      \"product_item_id\": \"sdownloader.inapp_lifetime_02\",\n" +
            "      \"discount_percent\": 0.5\n" +
            "    }\n" +
            "  ],\n" +
            "  \"recommended_iab_item_id\": \"sdownloader.subscription_1y_01\"\n" +
            "}";

    public static final String PURCHASE_HOST_NAME = "store.thinkyeah.com";
}
