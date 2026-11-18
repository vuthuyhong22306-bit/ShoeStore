package com.example.shoestore.repository;

import com.example.shoestore.model.Coupon;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CouponRepository {
    private final DatabaseReference couponsRef;

    public CouponRepository() {
        couponsRef = FirebaseDatabase.getInstance().getReference("coupons");
    }

    public void saveCoupon(Coupon coupon, OnCouponActionCallback callback) {
        if (coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
            if (callback != null) callback.onError("Mã giảm giá không được để trống!");
            return;
        }
        couponsRef.child(coupon.getCode()).setValue(coupon)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError("Lỗi lưu mã giảm giá: " + e.getMessage());
                });
    }

    public void listenCoupons(OnCouponsLoadedListener listener) {
        couponsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Coupon> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Coupon coupon = child.getValue(Coupon.class);
                    if (coupon != null) {
                        list.add(coupon);
                    }
                }
                if (listener != null) listener.onLoaded(list);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (listener != null) listener.onError(error.getMessage());
            }
        });
    }

    public interface OnCouponActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface OnCouponsLoadedListener {
        void onLoaded(List<Coupon> coupons);
        void onError(String message);
    }
}
