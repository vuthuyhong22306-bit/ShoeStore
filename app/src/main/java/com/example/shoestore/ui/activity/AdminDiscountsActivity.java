package com.example.shoestore.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityAdminDiscountsBinding;
import com.example.shoestore.databinding.ItemAdminCouponBinding;
import com.example.shoestore.model.Coupon;
import com.example.shoestore.repository.CouponRepository;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminDiscountsActivity extends AppCompatActivity {
    private ActivityAdminDiscountsBinding binding;
    private CouponRepository couponRepository;
    private AdminCouponAdapter adapter;
    private List<Coupon> couponList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDiscountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        couponRepository = new CouponRepository();
        adapter = new AdminCouponAdapter();
        binding.rvAdminCoupons.setAdapter(adapter);

        couponRepository.listenCoupons(new CouponRepository.OnCouponsLoadedListener() {
            @Override
            public void onLoaded(List<Coupon> coupons) {
                couponList = coupons != null ? coupons : new ArrayList<>();
                adapter.setCoupons(couponList);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminDiscountsActivity.this, "Lỗi kết nối Firebase: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAddVoucher.setOnClickListener(v -> showAddEditCouponDialog(null));
    }

    private void showAddEditCouponDialog(Coupon existingCoupon) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_add_edit_coupon, null);
        EditText etCode = dialogView.findViewById(R.id.etCouponCode);
        EditText etValue = dialogView.findViewById(R.id.etDiscountValue);
        EditText etMinOrder = dialogView.findViewById(R.id.etMinOrderValue);
        EditText etExpiry = dialogView.findViewById(R.id.etExpiryDate);
        EditText etMaxUsage = dialogView.findViewById(R.id.etMaxUsage);
        RadioGroup rgType = dialogView.findViewById(R.id.rgDiscountType);
        RadioButton rbPercent = dialogView.findViewById(R.id.rbPercent);
        RadioButton rbAmount = dialogView.findViewById(R.id.rbAmount);

        if (etCode != null && etValue != null && etMinOrder != null && etExpiry != null) {
            if (existingCoupon != null) {
                etCode.setText(existingCoupon.getCode());
                etCode.setEnabled(false); // Cannot change code when editing
                if (existingCoupon.getDiscountPercent() > 0) {
                    if (rbPercent != null) rbPercent.setChecked(true);
                    etValue.setText(String.valueOf(existingCoupon.getDiscountPercent()));
                } else if (existingCoupon.getDiscountAmount() > 0) {
                    if (rbAmount != null) rbAmount.setChecked(true);
                    etValue.setText(String.valueOf((long) existingCoupon.getDiscountAmount()));
                }
                etMinOrder.setText(String.valueOf((long) existingCoupon.getMinOrderValue()));
                etExpiry.setText(existingCoupon.getExpiryDate());
                if (etMaxUsage != null) {
                    etMaxUsage.setText(String.valueOf(existingCoupon.getMaxUsage()));
                }
            }

            Calendar calendar = Calendar.getInstance();
            etExpiry.setOnClickListener(v -> {
                DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etExpiry.setText(sdf.format(calendar.getTime()));
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                datePicker.show();
            });
        }

        new AlertDialog.Builder(this)
                .setTitle(existingCoupon == null ? "Tạo mã giảm giá mới" : "Sửa mã giảm giá " + existingCoupon.getCode())
                .setView(dialogView)
                .setPositiveButton("Lưu mã", (dialog, which) -> {
                    if (etCode == null || etValue == null || etMinOrder == null || etExpiry == null) return;

                    String code = etCode.getText().toString().trim().toUpperCase();
                    String valStr = etValue.getText().toString().trim();
                    String minOrderStr = etMinOrder.getText().toString().trim();
                    String expiry = etExpiry.getText().toString().trim();
                    String maxUsageStr = etMaxUsage != null ? etMaxUsage.getText().toString().trim() : "100";

                    if (code.isEmpty() || valStr.isEmpty() || expiry.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (existingCoupon == null) {
                        for (Coupon c : couponList) {
                            if (c.getCode() != null && c.getCode().equalsIgnoreCase(code)) {
                                Toast.makeText(this, "Mã giảm giá đã tồn tại!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }

                    double val = Double.parseDouble(valStr);
                    double minOrder = minOrderStr.isEmpty() ? 0 : Double.parseDouble(minOrderStr);
                    int maxUsage = maxUsageStr.isEmpty() ? 100 : Integer.parseInt(maxUsageStr);

                    int percent = 0;
                    double amount = 0;
                    int selectedTypeId = rgType != null ? rgType.getCheckedRadioButtonId() : -1;

                    if (selectedTypeId == R.id.rbAmount) {
                        amount = val;
                    } else { // Percent
                        if (val < 1 || val > 100) {
                            Toast.makeText(this, "Phần trăm giảm giá phải từ 1% đến 100%!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        percent = (int) val;
                    }

                    Coupon coupon = existingCoupon != null ? existingCoupon : new Coupon();
                    coupon.setCode(code);
                    coupon.setDiscountPercent(percent);
                    coupon.setDiscountAmount(amount);
                    coupon.setMinOrderValue(minOrder);
                    coupon.setExpiryDate(expiry);
                    coupon.setAvailable(true);
                    coupon.setMaxUsage(maxUsage);

                    if (percent > 0) {
                        coupon.setDescription("Giảm " + percent + "% cho đơn hàng từ " + formatCurrency(minOrder));
                    } else {
                        coupon.setDescription("Giảm " + formatCurrency(amount) + " cho đơn hàng từ " + formatCurrency(minOrder));
                    }

                    couponRepository.saveCoupon(coupon, new CouponRepository.OnCouponActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminDiscountsActivity.this, "Đã lưu mã " + code + " lên Firebase!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(AdminDiscountsActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private static String formatCurrency(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount).replace(",", ".") + "đ";
    }

    class AdminCouponAdapter extends RecyclerView.Adapter<AdminCouponAdapter.AdminCouponViewHolder> {
        private List<Coupon> coupons = new ArrayList<>();

        public void setCoupons(List<Coupon> coupons) {
            this.coupons = coupons != null ? coupons : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AdminCouponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAdminCouponBinding binding = ItemAdminCouponBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AdminCouponViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminCouponViewHolder holder, int position) {
            holder.bind(coupons.get(position));
        }

        @Override
        public int getItemCount() {
            return coupons.size();
        }

        class AdminCouponViewHolder extends RecyclerView.ViewHolder {
            private ItemAdminCouponBinding binding;

            public AdminCouponViewHolder(ItemAdminCouponBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Coupon coupon) {
                binding.tvCouponCode.setText(coupon.getCode());

                boolean isExpired = isCouponExpired(coupon.getExpiryDate());
                boolean isLimitReached = coupon.getUsedCount() >= coupon.getMaxUsage() && coupon.getMaxUsage() > 0;
                boolean canBeActive = coupon.isAvailable() && !isExpired && !isLimitReached;

                binding.switchCouponActive.setOnCheckedChangeListener(null);
                binding.switchCouponActive.setChecked(canBeActive);
                binding.switchCouponActive.setEnabled(!isExpired && !isLimitReached);

                binding.switchCouponActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isExpired) {
                        buttonView.setChecked(false);
                        Toast.makeText(AdminDiscountsActivity.this, "Mã này đã hết hạn, không thể bật!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (isLimitReached) {
                        buttonView.setChecked(false);
                        Toast.makeText(AdminDiscountsActivity.this, "Mã này đã hết lượt sử dụng, không thể bật!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    coupon.setAvailable(isChecked);
                    couponRepository.toggleCouponStatus(coupon.getCode(), isChecked, null);
                });

                if (coupon.getDiscountPercent() > 0) {
                    binding.tvDiscountTitle.setText("Giảm " + coupon.getDiscountPercent() + "%");
                } else if (coupon.getDiscountAmount() > 0) {
                    binding.tvDiscountTitle.setText("Giảm " + formatCurrency(coupon.getDiscountAmount()));
                } else {
                    binding.tvDiscountTitle.setText("Ưu đãi giảm giá");
                }

                String minStr = formatCurrency(coupon.getMinOrderValue());
                String expStr = coupon.getExpiryDate() != null ? coupon.getExpiryDate() : "31/12/2026";
                binding.tvCouponCondition.setText("Đơn tối thiểu " + minStr + " · HSD " + expStr);

                int used = coupon.getUsedCount();
                int total = coupon.getMaxUsage();
                binding.tvUsageCount.setText("Đã dùng " + used + "/" + total + " lượt");

                binding.btnEditCoupon.setOnClickListener(v -> showAddEditCouponDialog(coupon));

                binding.btnDeleteCoupon.setOnClickListener(v -> {
                    new AlertDialog.Builder(AdminDiscountsActivity.this)
                            .setTitle("Xóa mã giảm giá")
                            .setMessage("Bạn có chắc chắn muốn xóa mã \"" + coupon.getCode() + "\"?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                couponRepository.deleteCoupon(coupon.getCode(), new CouponRepository.OnCouponActionCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(AdminDiscountsActivity.this, "Đã xóa mã giảm giá", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(String message) {
                                        Toast.makeText(AdminDiscountsActivity.this, message, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            }

            private boolean isCouponExpired(String expiryDate) {
                if (expiryDate == null || expiryDate.trim().isEmpty()) return false;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    Date exp = sdf.parse(expiryDate);
                    Date today = new Date();
                    return exp != null && exp.before(today);
                } catch (Exception e) {
                    return false;
                }
            }
        }
    }
}