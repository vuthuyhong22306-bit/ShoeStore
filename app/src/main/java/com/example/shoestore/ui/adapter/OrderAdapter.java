package com.example.shoestore.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ItemOrderBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.OrderWithCount;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<OrderWithCount> orderList = new ArrayList<>();
    private OnOrderActionListener actionListener;
    private OnOrderClickListener clickListener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public interface OnOrderActionListener {
        void onOrderDetailClick(Order order);
        void onCancelOrderClick(Order order);
        void onReviewOrderClick(Order order);
        void onReturnOrderClick(Order order);
    }

    public OrderAdapter(OnOrderActionListener listener) {
        this.actionListener = listener;
    }

    public OrderAdapter(OnOrderClickListener listener) {
        this.clickListener = listener;
    }

    public void setOrders(List<OrderWithCount> orders) {
        this.orderList = orders != null ? orders : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setRawOrders(List<Order> orders) {
        List<OrderWithCount> list = new ArrayList<>();
        if (orders != null) {
            for (Order o : orders) {
                list.add(new OrderWithCount(o, 1));
            }
        }
        this.orderList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orderList.get(position));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderBinding binding;

        public OrderViewHolder(ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OrderWithCount orderWithCount) {
            Order order = orderWithCount.getOrder();
            int count = orderWithCount.getItemCount();

            // Order ID format: Đơn #DH0125
            String formattedId = String.format(Locale.getDefault(), "Đơn #DH%04d", order.getId());
            binding.tvOrderId.setText(formattedId);

            // Date format: 10/11/2026 · 2 sản phẩm
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date(order.getOrderDate()));
            String subtitle = dateStr + " · " + count + " sản phẩm";
            binding.tvOrderSubtitle.setText(subtitle);

            // Note (if any, like "Trả hàng: Đã hoàn tiền")
            if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
                binding.tvOrderNote.setVisibility(View.VISIBLE);
                binding.tvOrderNote.setText(order.getNote());
            } else {
                binding.tvOrderNote.setVisibility(View.GONE);
            }

            // Total Amount format: 2.880.000đ
            DecimalFormat df = new DecimalFormat("#,###");
            String formattedPrice = df.format(order.getTotalAmount()).replace(',', '.') + "đ";
            binding.tvTotalAmount.setText(formattedPrice);

            // Status Badge & Stepper & Buttons
            String status = order.getStatus() != null ? order.getStatus() : "Chờ xác nhận";
            binding.tvOrderStatus.setText(status);

            // Reset buttons visibility
            binding.btnOutlineAction.setVisibility(View.GONE);
            binding.btnRedAction.setVisibility(View.VISIBLE);
            binding.layoutStepper.setVisibility(View.GONE);

            switch (status) {
                case "Chờ xác nhận":
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_badge_pending);
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#E5192C"));

                    // Show "Hủy đơn" outline button & "Chi tiết" red button
                    binding.btnOutlineAction.setVisibility(View.VISIBLE);
                    binding.btnOutlineAction.setText("Hủy đơn");
                    binding.btnRedAction.setText("Chi tiết");
                    break;

                case "Đang giao":
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_badge_shipping);
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#495057"));

                    // Show Stepper progress bar
                    binding.layoutStepper.setVisibility(View.VISIBLE);
                    binding.btnRedAction.setText("Chi tiết");
                    break;

                case "Đã hoàn thành":
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_badge_completed);
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#1E8E3E"));

                    if (order.getNote() != null && order.getNote().contains("Trả hàng")) {
                        binding.btnRedAction.setText("Chi tiết");
                    } else {
                        // Show "Đánh giá" outline button & "Trả hàng" red button
                        binding.btnOutlineAction.setVisibility(View.VISIBLE);
                        binding.btnOutlineAction.setText("Đánh giá");
                        binding.btnRedAction.setText("Trả hàng");
                    }
                    break;

                case "Đã hủy":
                default:
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
                    binding.tvOrderStatus.setTextColor(Color.parseColor("#777777"));
                    binding.btnRedAction.setText("Chi tiết");
                    break;
            }

            // Click Listeners
            binding.getRoot().setOnClickListener(v -> {
                if (actionListener != null) actionListener.onOrderDetailClick(order);
                if (clickListener != null) clickListener.onOrderClick(order);
            });

            binding.btnRedAction.setOnClickListener(v -> {
                if (actionListener != null) {
                    String btnText = binding.btnRedAction.getText().toString();
                    if ("Trả hàng".equals(btnText)) {
                        actionListener.onReturnOrderClick(order);
                    } else {
                        actionListener.onOrderDetailClick(order);
                    }
                }
                if (clickListener != null) {
                    clickListener.onOrderClick(order);
                }
            });

            binding.btnOutlineAction.setOnClickListener(v -> {
                if (actionListener != null) {
                    String btnText = binding.btnOutlineAction.getText().toString();
                    if ("Hủy đơn".equals(btnText)) {
                        actionListener.onCancelOrderClick(order);
                    } else if ("Đánh giá".equals(btnText)) {
                        actionListener.onReviewOrderClick(order);
                    }
                }
            });
        }
    }
}
