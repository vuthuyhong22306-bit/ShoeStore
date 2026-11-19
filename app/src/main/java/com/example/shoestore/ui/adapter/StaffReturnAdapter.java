package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ItemStaffOrderBinding;
import com.example.shoestore.model.Order;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffReturnAdapter extends RecyclerView.Adapter<StaffReturnAdapter.StaffReturnViewHolder> {

    public interface OnReturnActionListener {
        void onViewRelatedOrder(Order order);
        void onAcceptReturn(Order order);
        void onRejectReturn(Order order, String reason);
        void onConfirmReceivedGoods(Order order);
        void onConfirmRefund(Order order);
    }

    private List<Order> orderList = new ArrayList<>();
    private String currentTab = "PENDING";
    private Map<Integer, String> detailsSummaryMap = new HashMap<>();
    private OnReturnActionListener listener;

    public StaffReturnAdapter(OnReturnActionListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> orders, String currentTab, Map<Integer, String> detailsMap) {
        this.orderList = orders != null ? orders : new ArrayList<>();
        this.currentTab = currentTab != null ? currentTab : "PENDING";
        this.detailsSummaryMap = detailsMap != null ? detailsMap : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StaffReturnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStaffOrderBinding binding = ItemStaffOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StaffReturnViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffReturnViewHolder holder, int position) {
        holder.bind(orderList.get(position));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class StaffReturnViewHolder extends RecyclerView.ViewHolder {
        private ItemStaffOrderBinding binding;

        public StaffReturnViewHolder(ItemStaffOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Order order) {
            binding.tvOrderCode.setText("Đơn #DH" + String.format("%04d", order.getId()));
            binding.tvOrderStatus.setText(order.getStatus());

            String summary = detailsSummaryMap.get(order.getId());
            if (summary != null && !summary.isEmpty()) {
                binding.tvCustomerSummary.setText(summary);
            } else {
                String customer = order.getReceiverName() != null ? order.getReceiverName() : "Khách hàng";
                String reason = order.getNote() != null ? order.getNote() : "";
                binding.tvCustomerSummary.setText(customer + " · Lý do: " + reason);
            }

            DecimalFormat fmt = new DecimalFormat("#,###");
            binding.tvOrderTotal.setText(fmt.format(order.getTotalAmount()).replace(",", ".") + "đ");

            if ("PENDING".equals(currentTab)) {
                binding.layoutActionButtons.setVisibility(View.VISIBLE);
                binding.btnConfirm.setText("Duyệt");
                binding.btnReject.setText("Từ chối");
                binding.btnReject.setVisibility(View.VISIBLE);
            } else if ("ACCEPTED".equals(currentTab)) {
                binding.layoutActionButtons.setVisibility(View.VISIBLE);
                binding.btnConfirm.setText("Đã nhận hàng");
                binding.btnReject.setVisibility(View.GONE);
            } else if ("RECEIVED".equals(currentTab)) {
                binding.layoutActionButtons.setVisibility(View.VISIBLE);
                binding.btnConfirm.setText("Xác nhận hoàn tiền");
                binding.btnReject.setVisibility(View.GONE);
            } else {
                binding.layoutActionButtons.setVisibility(View.GONE);
            }

            binding.btnConfirm.setOnClickListener(v -> {
                if (listener != null) {
                    if ("PENDING".equals(currentTab)) {
                        listener.onAcceptReturn(order);
                    } else if ("ACCEPTED".equals(currentTab)) {
                        listener.onConfirmReceivedGoods(order);
                    } else if ("RECEIVED".equals(currentTab)) {
                        listener.onConfirmRefund(order);
                    }
                }
            });

            binding.btnReject.setOnClickListener(v -> {
                if (listener != null && "PENDING".equals(currentTab)) {
                    View dialogView = LayoutInflater.from(binding.getRoot().getContext()).inflate(R.layout.dialog_return, null);
                    EditText etReason = dialogView.findViewById(R.id.etReason);
                    new AlertDialog.Builder(binding.getRoot().getContext())
                            .setTitle("Từ chối trả hàng #DH" + String.format("%04d", order.getId()))
                            .setView(dialogView)
                            .setPositiveButton("Từ chối", (dialog, which) -> {
                                String reason = etReason != null ? etReason.getText().toString().trim() : "";
                                listener.onRejectReturn(order, reason);
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewRelatedOrder(order);
                }
            });
        }
    }
}