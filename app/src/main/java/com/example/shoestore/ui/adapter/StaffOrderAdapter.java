package com.example.shoestore.ui.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.databinding.ItemStaffOrderBinding;
import com.example.shoestore.model.Order;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StaffOrderAdapter extends RecyclerView.Adapter<StaffOrderAdapter.StaffOrderViewHolder> {

    public interface OnOrderActionListener {
        void onConfirmOrder(Order order);
        void onRejectOrder(Order order);
        void onOrderClick(Order order);
    }

    private List<Order> orderList = new ArrayList<>();
    private OnOrderActionListener listener;

    public StaffOrderAdapter(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> orders) {
        this.orderList = orders != null ? orders : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StaffOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStaffOrderBinding binding = ItemStaffOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StaffOrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffOrderViewHolder holder, int position) {
        holder.bind(orderList.get(position));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class StaffOrderViewHolder extends RecyclerView.ViewHolder {
        private ItemStaffOrderBinding binding;

        public StaffOrderViewHolder(ItemStaffOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Order order) {
            binding.tvOrderCode.setText("Đơn #DH" + String.format(Locale.getDefault(), "%04d", order.getId()));
            binding.tvOrderStatus.setText(order.getStatus());

            // Status Badge styling
            if ("Chờ xác nhận".equals(order.getStatus())) {
                binding.tvOrderStatus.setTextColor(Color.parseColor("#E5192C"));
                binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#1AE5192C"));
                binding.layoutActionButtons.setVisibility(View.VISIBLE);
                binding.btnConfirm.setText("Xác nhận");
                binding.btnReject.setVisibility(View.VISIBLE);
            } else if ("Đang giao".equals(order.getStatus())) {
                binding.tvOrderStatus.setTextColor(Color.parseColor("#1565C0"));
                binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#1A1565C0"));
                binding.layoutActionButtons.setVisibility(View.VISIBLE);
                binding.btnConfirm.setText("Hoàn thành");
                binding.btnReject.setVisibility(View.GONE);
            } else if ("Đã hoàn thành".equals(order.getStatus())) {
                binding.tvOrderStatus.setTextColor(Color.parseColor("#2E7D32"));
                binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#1A2E7D32"));
                binding.layoutActionButtons.setVisibility(View.GONE);
            } else if ("Yêu cầu trả hàng".equals(order.getStatus())) {
                binding.tvOrderStatus.setTextColor(Color.parseColor("#D32F2F"));
                binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#1AD32F2F"));
                binding.layoutActionButtons.setVisibility(View.VISIBLE);
                binding.btnConfirm.setText("Đồng ý trả");
                binding.btnReject.setText("Từ chối trả");
                binding.btnReject.setVisibility(View.VISIBLE);
            } else {
                binding.tvOrderStatus.setTextColor(Color.parseColor("#757575"));
                binding.tvOrderStatus.setBackgroundColor(Color.parseColor("#1A757575"));
                binding.layoutActionButtons.setVisibility(View.GONE);
            }

            // Customer Summary
            String customerName = order.getReceiverName() != null ? order.getReceiverName() : "Khách hàng";
            String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod() : "COD";
            binding.tvCustomerSummary.setText(customerName + " - " + paymentMethod);

            // Total Price
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvOrderTotal.setText(formatter.format(order.getTotalAmount()));

            // Confirm Button Action
            binding.btnConfirm.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConfirmOrder(order);
                }
            });

            // Reject Button Action with Confirmation Dialog
            binding.btnReject.setOnClickListener(v -> {
                Context context = binding.getRoot().getContext();
                new AlertDialog.Builder(context)
                        .setTitle("Xác nhận từ chối")
                        .setMessage("Bạn có chắc chắn muốn từ chối đơn hàng #" + order.getId() + " này không?")
                        .setPositiveButton("Từ chối đơn", (dialog, which) -> {
                            if (listener != null) {
                                listener.onRejectOrder(order);
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });

            // Card Click Event
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(order);
                }
            });
        }
    }
}
