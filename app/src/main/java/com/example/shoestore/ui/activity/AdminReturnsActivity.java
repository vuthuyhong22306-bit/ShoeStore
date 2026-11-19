package com.example.shoestore.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
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
import com.example.shoestore.databinding.ActivityAdminReturnsBinding;
import com.example.shoestore.databinding.ItemAdminReturnBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;

public class AdminReturnsActivity extends AppCompatActivity {
    private ActivityAdminReturnsBinding binding;
    private OrderRepository orderRepository;
    private AdminReturnAdapter adapter;
    private List<Order> allReturnOrders = new ArrayList<>();
    private String selectedStatusFilter = "Tất cả";

    private static final String[] CHIP_FILTERS = {
            "Tất cả", "Chờ duyệt", "Đã từ chối", "Đang hoàn"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReturnsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        orderRepository = new OrderRepository(getApplication());
        adapter = new AdminReturnAdapter();
        binding.rvAdminReturnOrders.setAdapter(adapter);

        buildFilterChips();

        orderRepository.getAllOrders().observe(this, orders -> {
            allReturnOrders.clear();
            if (orders != null) {
                for (Order o : orders) {
                    String st = o.getStatus();
                    if (st != null && (st.contains("trả hàng") || st.contains("hoàn") || st.contains("từ chối") || st.equalsIgnoreCase("Chờ duyệt"))) {
                        allReturnOrders.add(o);
                    }
                }
            }
            filterAndDisplayReturns();
        });
    }

    private void buildFilterChips() {
        binding.layoutReturnChips.removeAllViews();
        for (String filter : CHIP_FILTERS) {
            TextView chip = new TextView(this);
            chip.setText(filter);
            chip.setTextSize(14);
            chip.setPadding(36, 18, 36, 18);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            boolean isSelected = selectedStatusFilter.equals(filter);
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
                selectedStatusFilter = filter;
                buildFilterChips();
                filterAndDisplayReturns();
            });

            binding.layoutReturnChips.addView(chip);
        }
    }

    private void filterAndDisplayReturns() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : allReturnOrders) {
            String st = o.getStatus() != null ? o.getStatus() : "";
            if ("Tất cả".equals(selectedStatusFilter)) {
                filtered.add(o);
            } else if ("Chờ duyệt".equals(selectedStatusFilter) && (st.equalsIgnoreCase("Yêu cầu trả hàng") || st.equalsIgnoreCase("Chờ duyệt"))) {
                filtered.add(o);
            } else if ("Đã từ chối".equals(selectedStatusFilter) && (st.contains("từ chối") || st.contains("Từ chối"))) {
                filtered.add(o);
            } else if ("Đang hoàn".equals(selectedStatusFilter) && (st.contains("Chấp nhận") || st.contains("Đã nhận hàng") || st.contains("Đang hoàn"))) {
                filtered.add(o);
            }
        }
        adapter.setOrders(filtered);
    }

    private void showRejectReasonDialog(Order order) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_return, null);
        EditText etReason = dialogView.findViewById(R.id.etReason);
        if (etReason != null) {
            etReason.setHint("Nhập lý do từ chối yêu cầu trả hàng...");
        }

        new AlertDialog.Builder(this)
                .setTitle("Từ chối trả hàng #DH" + String.format("%04d", order.getId()))
                .setView(dialogView)
                .setPositiveButton("Từ chối", (dialog, which) -> {
                    String reason = etReason != null ? etReason.getText().toString().trim() : "";
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập lý do từ chối!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    order.setStatus("Đã từ chối");
                    order.setNote((order.getNote() != null ? order.getNote() + "\n" : "") + "Nhân viên từ chối: " + reason);
                    orderRepository.updateOrderStatus(order);
                    Toast.makeText(this, "Đã từ chối yêu cầu trả hàng!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    class AdminReturnAdapter extends RecyclerView.Adapter<AdminReturnAdapter.AdminReturnViewHolder> {
        private List<Order> orders = new ArrayList<>();

        public void setOrders(List<Order> orders) {
            this.orders = orders != null ? orders : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AdminReturnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAdminReturnBinding binding = ItemAdminReturnBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AdminReturnViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminReturnViewHolder holder, int position) {
            holder.bind(orders.get(position));
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class AdminReturnViewHolder extends RecyclerView.ViewHolder {
            private ItemAdminReturnBinding binding;

            public AdminReturnViewHolder(ItemAdminReturnBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Order order) {
                binding.tvReturnOrderCode.setText("Đơn #DH" + String.format("%04d", order.getId()));

                String status = order.getStatus() != null ? order.getStatus() : "Yêu cầu trả hàng";
                String name = order.getReceiverName() != null ? order.getReceiverName() : "Khách hàng";
                String note = order.getNote() != null ? order.getNote() : "Lý do không xác định";

                binding.tvReturnSubtitle.setText(name + " · " + note);

                // Default reset stepper buttons visibility
                binding.btnReject.setVisibility(View.GONE);
                binding.btnApprove.setVisibility(View.GONE);
                binding.btnReprocess.setVisibility(View.GONE);
                binding.btnConfirmRefund.setVisibility(View.GONE);

                // Configure stepper dots and labels according to status
                if (status.equalsIgnoreCase("Yêu cầu trả hàng") || status.equalsIgnoreCase("Chờ duyệt")) {
                    binding.tvReturnStatusBadge.setText("Chờ duyệt");
                    binding.tvReturnStatusBadge.setBackgroundResource(R.drawable.bg_badge_pink);
                    binding.tvReturnStatusBadge.setTextColor(Color.parseColor("#D0343C"));

                    setStepState(1);

                    binding.btnReject.setVisibility(View.VISIBLE);
                    binding.btnApprove.setVisibility(View.VISIBLE);
                    binding.btnApprove.setText("Duyệt");
                } else if (status.contains("từ chối") || status.contains("Từ chối")) {
                    binding.tvReturnStatusBadge.setText("Đã từ chối");
                    binding.tvReturnStatusBadge.setBackgroundResource(R.drawable.bg_badge_grey);
                    binding.tvReturnStatusBadge.setTextColor(Color.parseColor("#444444"));

                    setStepState(1);

                    binding.btnReprocess.setVisibility(View.VISIBLE);
                } else if (status.equalsIgnoreCase("Chấp nhận trả hàng") || status.equalsIgnoreCase("Chấp nhận")) {
                    binding.tvReturnStatusBadge.setText("Đã chấp nhận");
                    binding.tvReturnStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
                    binding.tvReturnStatusBadge.setTextColor(Color.parseColor("#1E7B3C"));

                    setStepState(2);

                    binding.btnConfirmRefund.setVisibility(View.VISIBLE);
                    binding.btnConfirmRefund.setText("Đã nhận hàng");
                } else if (status.equalsIgnoreCase("Đã nhận hàng trả") || status.equalsIgnoreCase("Đã nhận hàng")) {
                    binding.tvReturnStatusBadge.setText("Đã nhận hàng trả");
                    binding.tvReturnStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
                    binding.tvReturnStatusBadge.setTextColor(Color.parseColor("#1E7B3C"));

                    setStepState(3);

                    binding.btnConfirmRefund.setVisibility(View.VISIBLE);
                    binding.btnConfirmRefund.setText("Xác nhận hoàn tiền");
                } else if (status.equalsIgnoreCase("Đã hoàn tiền")) {
                    binding.tvReturnStatusBadge.setText("Đã hoàn tiền");
                    binding.tvReturnStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
                    binding.tvReturnStatusBadge.setTextColor(Color.parseColor("#1E7B3C"));

                    setStepState(4);
                }

                binding.btnApprove.setOnClickListener(v -> {
                    order.setStatus("Chấp nhận trả hàng");
                    orderRepository.updateOrderStatus(order);
                    Toast.makeText(AdminReturnsActivity.this, "Đã duyệt yêu cầu trả hàng!", Toast.LENGTH_SHORT).show();
                });

                binding.btnReject.setOnClickListener(v -> showRejectReasonDialog(order));

                binding.btnReprocess.setOnClickListener(v -> {
                    order.setStatus("Yêu cầu trả hàng");
                    orderRepository.updateOrderStatus(order);
                    Toast.makeText(AdminReturnsActivity.this, "Đã đưa đơn về trạng thái Chờ duyệt!", Toast.LENGTH_SHORT).show();
                });

                binding.btnConfirmRefund.setOnClickListener(v -> {
                    String currentSt = order.getStatus() != null ? order.getStatus() : "";
                    if (currentSt.equalsIgnoreCase("Chấp nhận trả hàng") || currentSt.equalsIgnoreCase("Chấp nhận")) {
                        order.setStatus("Đã nhận hàng trả");
                        orderRepository.updateOrderStatus(order);
                        Toast.makeText(AdminReturnsActivity.this, "Đã xác nhận nhận hàng trả thành công!", Toast.LENGTH_SHORT).show();
                    } else if (currentSt.equalsIgnoreCase("Đã nhận hàng trả") || currentSt.equalsIgnoreCase("Đã nhận hàng")) {
                        order.setStatus("Đã hoàn tiền");
                        orderRepository.updateOrderStatus(order);
                        Toast.makeText(AdminReturnsActivity.this, "Đã hoàn tất thủ tục hoàn tiền!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            private void setStepState(int activeStep) {
                binding.dotStep1.setBackgroundResource(activeStep >= 1 ? R.drawable.bg_stepper_dot_active : R.drawable.bg_stepper_dot_inactive);
                binding.tvLabelStep1.setTextColor(activeStep >= 1 ? Color.parseColor("#1C1C1E") : Color.parseColor("#8E8E93"));

                binding.dotStep2.setBackgroundResource(activeStep >= 2 ? R.drawable.bg_stepper_dot_active : R.drawable.bg_stepper_dot_inactive);
                binding.tvLabelStep2.setTextColor(activeStep >= 2 ? Color.parseColor("#1C1C1E") : Color.parseColor("#8E8E93"));

                binding.dotStep3.setBackgroundResource(activeStep >= 3 ? R.drawable.bg_stepper_dot_active : R.drawable.bg_stepper_dot_inactive);
                binding.tvLabelStep3.setTextColor(activeStep >= 3 ? Color.parseColor("#1C1C1E") : Color.parseColor("#8E8E93"));

                binding.dotStep4.setBackgroundResource(activeStep >= 4 ? R.drawable.bg_stepper_dot_active : R.drawable.bg_stepper_dot_inactive);
                binding.tvLabelStep4.setTextColor(activeStep >= 4 ? Color.parseColor("#1C1C1E") : Color.parseColor("#8E8E93"));
            }
        }
    }
}