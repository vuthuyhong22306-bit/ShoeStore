package com.example.shoestore.ui.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivityAdminDiscountsBinding;
import com.example.shoestore.model.Coupon;
import com.example.shoestore.repository.CouponRepository;

import java.util.List;

public class AdminDiscountsActivity extends AppCompatActivity {
    private ActivityAdminDiscountsBinding binding;
    private CouponRepository couponRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDiscountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý mã giảm giá");
        }

        couponRepository = new CouponRepository();

        couponRepository.listenCoupons(new CouponRepository.OnCouponsLoadedListener() {
            @Override
            public void onLoaded(List<Coupon> coupons) {
                // Coupons loaded real-time from Firebase
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminDiscountsActivity.this, "Lỗi đọc dữ liệu: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAddVoucher.setOnClickListener(v -> {
            String code = "SHOE" + System.currentTimeMillis() % 10000;
            Coupon newCoupon = new Coupon(code, 20, 0, "Áp dụng cho tất cả đơn hàng đầu tiên", "31/12/2026", true);
            couponRepository.saveCoupon(newCoupon, new CouponRepository.OnCouponActionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AdminDiscountsActivity.this, "Tạo mã " + code + " thành công trên Firebase!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(AdminDiscountsActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
