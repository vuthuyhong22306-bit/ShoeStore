package com.example.shoestore.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class GuestDataManager {
    private static final String PREF_NAME = "pending_action_pref";
    private static final String KEY_RETURN_DESTINATION = "return_destination";
    private static final String KEY_PENDING_ACTION = "pending_action";
    private static final String KEY_PENDING_PRODUCT_ID = "pending_product_id";
    private static final String KEY_PENDING_SIZE = "pending_size";
    private static final String KEY_PENDING_COLOR = "pending_color";
    private static final String KEY_PENDING_QUANTITY = "pending_quantity";
    private static final String KEY_PENDING_PRICE = "pending_price";

    private static GuestDataManager instance;
    private final SharedPreferences prefs;

    private GuestDataManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized GuestDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new GuestDataManager(context.getApplicationContext());
        }
        return instance;
    }

    public void savePendingAction(String destination, String action, int productId, String size, String color, int quantity, double price) {
        prefs.edit()
                .putString(KEY_RETURN_DESTINATION, destination)
                .putString(KEY_PENDING_ACTION, action)
                .putInt(KEY_PENDING_PRODUCT_ID, productId)
                .putString(KEY_PENDING_SIZE, size)
                .putString(KEY_PENDING_COLOR, color)
                .putInt(KEY_PENDING_QUANTITY, quantity)
                .putFloat(KEY_PENDING_PRICE, (float) price)
                .apply();
    }

    public String getReturnDestination() {
        return prefs.getString(KEY_RETURN_DESTINATION, null);
    }

    public String getPendingAction() {
        return prefs.getString(KEY_PENDING_ACTION, null);
    }

    public int getPendingProductId() {
        return prefs.getInt(KEY_PENDING_PRODUCT_ID, -1);
    }

    public String getPendingSize() {
        return prefs.getString(KEY_PENDING_SIZE, "");
    }

    public String getPendingColor() {
        return prefs.getString(KEY_PENDING_COLOR, "");
    }

    public int getPendingQuantity() {
        return prefs.getInt(KEY_PENDING_QUANTITY, 1);
    }

    public double getPendingPrice() {
        return prefs.getFloat(KEY_PENDING_PRICE, 0f);
    }

    public void clearPendingAction() {
        prefs.edit().clear().apply();
    }

    public void clearAllGuestData() {
        clearPendingAction();
    }
}
