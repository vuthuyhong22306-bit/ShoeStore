package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.databinding.ItemCouponBinding;
import com.example.shoestore.model.Coupon;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CouponAdapter extends RecyclerView.Adapter<CouponAdapter.CouponViewHolder> {
    private List<Coupon> couponList = new ArrayList<>();
    private final OnCouponClickListener listener;
    private String selectedCode = "";
    private final DecimalFormat df = new DecimalFormat("#,###");

    public interface OnCouponClickListener {
        void onCouponClick(Coupon coupon);
    }

    public CouponAdapter(OnCouponClickListener listener) {
        this.listener = listener;
    }

    public void setCoupons(List<Coupon> coupons, String currentSelectedCode) {
        this.couponList = coupons != null ? coupons : new ArrayList<>();
        this.selectedCode = currentSelectedCode != null ? currentSelectedCode : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CouponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCouponBinding binding = ItemCouponBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CouponViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CouponViewHolder holder, int position) {
        holder.bind(couponList.get(position));
    }

    @Override
    public int getItemCount() {
        return couponList.size();
    }

    public class CouponViewHolder extends RecyclerView.ViewHolder {
        private final ItemCouponBinding binding;

        public CouponViewHolder(ItemCouponBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Coupon coupon) {
            binding.tvCouponCodeBadge.setText(coupon.getCode());

            String title;
            if (coupon.getDiscountPercent() > 0) {
                title = "Giảm " + coupon.getDiscountPercent() + "%";
            } else {
                title = "Mã giảm giá đặc biệt";
            }
            binding.tvCouponTitle.setText(title);

            String minStr = coupon.getMinOrderValue() > 0
                    ? "Đơn tối thiểu " + df.format(coupon.getMinOrderValue()).replace(',', '.') + "đ"
                    : "Áp dụng cho mọi đơn hàng";
            String desc = coupon.getDescription() != null && !coupon.getDescription().isEmpty()
                    ? coupon.getDescription() + " · " + minStr
                    : minStr;
            binding.tvCouponDescription.setText(desc);

            String expiry = coupon.getExpiryDate() != null && !coupon.getExpiryDate().isEmpty()
                    ? "HSD: " + coupon.getExpiryDate()
                    : "HSD: Không giới hạn";
            binding.tvCouponExpiry.setText(expiry);

            boolean isSelected = coupon.getCode().equalsIgnoreCase(selectedCode);
            if (isSelected) {
                binding.btnSelectCoupon.setText("Đã chọn");
                binding.btnSelectCoupon.setAlpha(0.6f);
            } else {
                binding.btnSelectCoupon.setText("Dùng ngay");
                binding.btnSelectCoupon.setAlpha(1.0f);
            }

            binding.btnSelectCoupon.setOnClickListener(v -> {
                if (listener != null) listener.onCouponClick(coupon);
            });

            binding.cardCoupon.setOnClickListener(v -> {
                if (listener != null) listener.onCouponClick(coupon);
            });
        }
    }
}
