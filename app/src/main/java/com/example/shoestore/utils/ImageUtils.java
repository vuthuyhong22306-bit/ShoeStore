package com.example.shoestore.utils;

import com.example.shoestore.R;

public class ImageUtils {
    public static int getFallbackDrawableForProduct(String productName) {
        if (productName == null) return R.drawable.ic_shoe_nike;
        String lower = productName.toLowerCase();
        if (lower.contains("jordan")) {
            return R.drawable.ic_shoe_jordan;
        } else if (lower.contains("nike") || lower.contains("air force") || lower.contains("zoom")) {
            return R.drawable.ic_shoe_nike;
        } else if (lower.contains("adidas") || lower.contains("ultraboost") || lower.contains("stan smith") || lower.contains("nmd")) {
            return R.drawable.ic_shoe_adidas;
        } else if (lower.contains("puma") || lower.contains("suede") || lower.contains("rs-x") || lower.contains("cali")) {
            return R.drawable.ic_shoe_puma;
        } else if (lower.contains("converse") || lower.contains("chuck") || lower.contains("purcell") || lower.contains("star")) {
            return R.drawable.ic_shoe_converse;
        }
        return R.drawable.ic_shoe_default;
    }
}
