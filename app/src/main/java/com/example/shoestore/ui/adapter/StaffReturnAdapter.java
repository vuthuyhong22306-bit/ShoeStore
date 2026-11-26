package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ItemStaffReturnRequestBinding;
import com.example.shoestore.model.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaffReturnAdapter extends RecyclerView.Adapter<StaffReturnAdapter.ReturnViewHolder> {

    public interface OnReturnActionListener {
        void onViewRelatedOrder(Order order);
        void onAcceptReturn(Order order);
        void onRejectReturn(Order order, String reason);
        void onConfirmReceivedGoods(Order order);
        void onConfirmRefund(Order order);
    }

    private List<Order> orders = new ArrayList<>();
    private String currentTab = "PENDING"; // PENDING, ACCEPTED, RECEIVED
    private Map<Integer, String> detailsMap = new HashMap<>();
    private final OnReturnActionListener listener;

    public StaffReturnAdapter(OnReturnActionListener listener) {
        this.listener = listener;
    }

    public void setOrders(List<Order> orders, String currentTab, Map<Integer, String> detailsMap) {
        this.orders = orders != null ? orders : new ArrayList<>();
        this.currentTab = currentTab != null ? currentTab : "PENDING";
        if (detailsMap != null) {
            this.detailsMap = detailsMap;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReturnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStaffReturnRequestBinding binding = ItemStaffReturnRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ReturnViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReturnViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class ReturnViewHolder extends RecyclerView.ViewHolder {
        private final ItemStaffReturnRequestBinding binding;

        public ReturnViewHolder(@NonNull ItemStaffReturnRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Order order) {
            // Format Order ID: e.g. Đơn #DH0112 or Đơn #DH + 4 digits
            String orderIdStr = String.format(Locale.US, "Đơn #DH%04d", order.getId());
            binding.tvOrderId.setText(orderIdStr);

            // Detail line summary
            String detailSummary = detailsMap.get(order.getId());
            if (detailSummary == null || detailSummary.isEmpty()) {
                String customerName = order.getReceiverName() != null ? order.getReceiverName() : "Khách hàng";
                String reason = order.getNote() != null ? order.getNote() : "";
                if (reason.startsWith("Trả hàng: ")) {
                    reason = reason.substring("Trả hàng: ".length());
                }
                detailSummary = customerName + " · Lý do: " + reason;
            }
            binding.tvReturnDetail.setText(detailSummary);

            // Tab-specific UI visibility & styling
            if ("PENDING".equals(currentTab)) {
                binding.tvStatusTag.setText(R.string.tab_pending);
                binding.tvStatusTag.setBackgroundResource(R.drawable.bg_badge_pending_orange);
                binding.tvStatusTag.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), android.R.color.holo_red_dark));

                binding.layoutPendingControls.setVisibility(View.VISIBLE);
                binding.layoutAcceptedControls.setVisibility(View.GONE);
                binding.layoutReceivedControls.setVisibility(View.GONE);

                binding.etRejectReason.setText("");

                binding.tvViewRelatedOrder.setOnClickListener(v -> {
                    if (listener != null) listener.onViewRelatedOrder(order);
                });

                binding.btnAccept.setOnClickListener(v -> {
                    if (listener != null) listener.onAcceptReturn(order);
                });

                binding.btnReject.setOnClickListener(v -> {
                    String reason = binding.etRejectReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(binding.getRoot().getContext(),
                                "Vui lòng nhập lý do từ chối!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (listener != null) listener.onRejectReturn(order, reason);
                });

            } else if ("ACCEPTED".equals(currentTab)) {
                binding.tvStatusTag.setText(R.string.tab_accepted);
                binding.tvStatusTag.setBackgroundResource(R.drawable.bg_badge_accepted_grey);
                binding.tvStatusTag.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), android.R.color.darker_gray));

                binding.layoutPendingControls.setVisibility(View.GONE);
                binding.layoutAcceptedControls.setVisibility(View.VISIBLE);
                binding.layoutReceivedControls.setVisibility(View.GONE);

                binding.btnConfirmReceived.setOnClickListener(v -> {
                    if (listener != null) listener.onConfirmReceivedGoods(order);
                });

            } else if ("RECEIVED".equals(currentTab)) {
                binding.tvStatusTag.setText(R.string.tab_received);
                binding.tvStatusTag.setBackgroundResource(R.drawable.bg_badge_received_green);
                binding.tvStatusTag.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), android.R.color.holo_green_dark));

                binding.layoutPendingControls.setVisibility(View.GONE);
                binding.layoutAcceptedControls.setVisibility(View.GONE);
                binding.layoutReceivedControls.setVisibility(View.VISIBLE);

                binding.btnConfirmRefund.setOnClickListener(v -> {
                    if (listener != null) listener.onConfirmRefund(order);
                });
            }
        }
    }
}
