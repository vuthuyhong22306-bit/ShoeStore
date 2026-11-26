package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityManageOrdersBinding;
import com.example.shoestore.databinding.ItemAdminOrderBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.repository.OrderRepository;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageOrdersActivity extends AppCompatActivity {
    private ActivityManageOrdersBinding binding;
    private OrderRepository orderRepository;
    private AdminOrderAdapter adapter;
    private List<Order> rawOrders = new ArrayList<>();
    private String selectedStatus = "Tất cả";
    private String searchQuery = "";

    private static final String[] STATUS_FILTERS = {
            "Tất cả", "Chờ xác nhận", "Đang giao", "Đã hoàn thành", "Đã hủy"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        orderRepository = new OrderRepository(getApplication());
        adapter = new AdminOrderAdapter();
        binding.rvManageOrders.setAdapter(adapter);

        buildStatusChips();

        orderRepository.getAllOrders().observe(this, orders -> {
            rawOrders = orders != null ? orders : new ArrayList<>();
            filterAndDisplayOrders();
        });

        binding.etSearchOrder.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                filterAndDisplayOrders();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void buildStatusChips() {
        binding.layoutOrderChips.removeAllViews();
        for (String status : STATUS_FILTERS) {
            TextView chip = new TextView(this);
            chip.setText(status);
            chip.setTextSize(14);
            chip.setPadding(36, 18, 36, 18);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            boolean isSelected = selectedStatus.equals(status);
            if (isSelected) {
                chip.setBackgroundResource(R.drawable.bg_admin_chip_selected);
                chip.setTextColor(Color.WHITE);
                chip.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                chip.setBackgroundResource(R.drawable.bg_admin_chip_unselected);
                chip.setTextColor(Color.parseColor("#1C1C1E"));
                chip.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            chip.setOnClickListener(v -> {
                selectedStatus = status;
                buildStatusChips();
                filterAndDisplayOrders();
            });

            binding.layoutOrderChips.addView(chip);
        }
    }

    private void filterAndDisplayOrders() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : rawOrders) {
            boolean matchesStatus = "Tất cả".equals(selectedStatus) || selectedStatus.equalsIgnoreCase(o.getStatus());

            String codeStr = "dh" + String.format(Locale.getDefault(), "%04d", o.getId());
            String nameStr = o.getReceiverName() != null ? o.getReceiverName().toLowerCase() : "";
            boolean matchesSearch = searchQuery.isEmpty() || codeStr.contains(searchQuery) || String.valueOf(o.getId()).contains(searchQuery) || nameStr.contains(searchQuery);

            if (matchesStatus && matchesSearch) {
                filtered.add(o);
            }
        }
        adapter.setOrders(filtered);
    }

    private void showUpdateStatusDialog(Order order) {
        String current = order.getStatus() != null ? order.getStatus() : "Chờ xác nhận";

        if ("Chờ xác nhận".equalsIgnoreCase(current)) {
            String[] options = {"Đang giao", "Hủy đơn"};
            new AlertDialog.Builder(this)
                    .setTitle("Cập nhật đơn hàng #DH" + String.format("%04d", order.getId()))
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            order.setStatus("Đang giao");
                            orderRepository.updateOrderStatus(order);
                            Toast.makeText(this, "Đã cập nhật đơn hàng sang Đang giao", Toast.LENGTH_SHORT).show();
                        } else {
                            showCancelOrderReasonDialog(order);
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else if ("Đang giao".equalsIgnoreCase(current)) {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận hoàn thành đơn hàng")
                    .setMessage("Xác nhận đơn hàng #DH" + String.format("%04d", order.getId()) + " đã giao thành công?")
                    .setPositiveButton("Xác nhận", (dialog, which) -> {
                        order.setStatus("Đã hoàn thành");
                        orderRepository.updateOrderStatus(order);
                        Toast.makeText(this, "Đã cập nhật đơn hàng thành Đã hoàn thành", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Quay lại", null)
                    .show();
        }
    }

    private void showCancelOrderReasonDialog(Order order) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_return, null);
        EditText etReason = dialogView.findViewById(R.id.etReason);
        if (etReason != null) {
            etReason.setHint("Nhập lý do hủy (ví dụ: Hết hàng trong kho)...");
        }

        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng #DH" + String.format("%04d", order.getId()))
                .setView(dialogView)
                .setPositiveButton("Hủy đơn", (dialog, which) -> {
                    String reason = etReason != null ? etReason.getText().toString().trim() : "";
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập lý do hủy đơn!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    order.setNote("Lý do: " + reason);
                    orderRepository.cancelOrder(order, () -> runOnUiThread(() -> {
                        Toast.makeText(this, "Đã hủy đơn hàng và hoàn lại tồn kho!", Toast.LENGTH_SHORT).show();
                        filterAndDisplayOrders();
                    }));
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder> {
        private List<Order> orders = new ArrayList<>();

        public void setOrders(List<Order> orders) {
            this.orders = orders != null ? orders : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AdminOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAdminOrderBinding binding = ItemAdminOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AdminOrderViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminOrderViewHolder holder, int position) {
            holder.bind(orders.get(position));
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class AdminOrderViewHolder extends RecyclerView.ViewHolder {
            private ItemAdminOrderBinding binding;

            public AdminOrderViewHolder(ItemAdminOrderBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Order order) {
                binding.tvOrderCode.setText("Đơn #DH" + String.format("%04d", order.getId()));

                String status = order.getStatus() != null ? order.getStatus() : "Chờ xác nhận";
                binding.tvOrderStatusBadge.setText(status);

                if ("Chờ xác nhận".equalsIgnoreCase(status)) {
                    binding.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_badge_pink);
                    binding.tvOrderStatusBadge.setTextColor(Color.parseColor("#D0343C"));
                    binding.btnOrderUpdate.setVisibility(View.VISIBLE);
                } else if ("Đang giao".equalsIgnoreCase(status)) {
                    binding.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_badge_grey);
                    binding.tvOrderStatusBadge.setTextColor(Color.parseColor("#444444"));
                    binding.btnOrderUpdate.setVisibility(View.VISIBLE);
                } else if ("Đã hoàn thành".equalsIgnoreCase(status)) {
                    binding.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
                    binding.tvOrderStatusBadge.setTextColor(Color.parseColor("#1E7B3C"));
                    binding.btnOrderUpdate.setVisibility(View.GONE);
                } else { // Đã hủy or other
                    binding.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_badge_grey);
                    binding.tvOrderStatusBadge.setTextColor(Color.parseColor("#444444"));
                    binding.btnOrderUpdate.setVisibility(View.GONE);
                }

                String name = order.getReceiverName() != null ? order.getReceiverName() : "Khách hàng";
                String date = "";
                if (order.getOrderDate() > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    date = sdf.format(new java.util.Date(order.getOrderDate()));
                }

                if ("Đã hủy".equalsIgnoreCase(status) && order.getNote() != null && !order.getNote().isEmpty()) {
                    binding.tvOrderSubtitle.setText(name + " · " + date + " · " + order.getNote());
                } else {
                    binding.tvOrderSubtitle.setText(name + " · " + date + " · 1 sản phẩm");
                }

                DecimalFormat formatter = new DecimalFormat("#,###");
                String formattedPrice = formatter.format(order.getTotalAmount()).replace(",", ".") + "đ";
                binding.tvOrderTotalPrice.setText(formattedPrice);

                binding.btnOrderDetail.setOnClickListener(v -> {
                    Intent intent = new Intent(ManageOrdersActivity.this, OrderDetailActivity.class);
                    intent.putExtra("ORDER_ID", order.getId());
                    startActivity(intent);
                });

                binding.btnOrderUpdate.setOnClickListener(v -> showUpdateStatusDialog(order));
            }
        }
    }
}