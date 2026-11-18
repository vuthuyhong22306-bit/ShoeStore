package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.shoestore.R;
import com.example.shoestore.databinding.FragmentOrdersBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.OrderWithCount;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.ui.activity.OrderDetailActivity;
import com.example.shoestore.ui.adapter.OrderAdapter;
import com.example.shoestore.utils.SharedPrefManager;

import java.util.List;

public class OrdersFragment extends Fragment implements OrderAdapter.OnOrderActionListener {
    private FragmentOrdersBinding binding;
    private OrderRepository orderRepository;
    private OrderAdapter adapter;
    private int userId;
    private String currentStatusFilter = "Tất cả";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() == null) return;

        orderRepository = new OrderRepository(getActivity().getApplication());
        adapter = new OrderAdapter(this);
        binding.rvOrders.setAdapter(adapter);

        userId = SharedPrefManager.getInstance(getContext()).getUserId();

        setupFilterChips();
        loadOrders(currentStatusFilter);
    }

    private void setupFilterChips() {
        binding.chipAll.setOnClickListener(v -> selectChip("Tất cả", binding.chipAll));
        binding.chipPending.setOnClickListener(v -> selectChip("Chờ xác nhận", binding.chipPending));
        binding.chipShipping.setOnClickListener(v -> selectChip("Đang giao", binding.chipShipping));
        binding.chipCompleted.setOnClickListener(v -> selectChip("Đã hoàn thành", binding.chipCompleted));
        binding.chipReturned.setOnClickListener(v -> selectChip("Trả hàng", binding.chipReturned));
    }

    private void selectChip(String status, TextView selectedChip) {
        currentStatusFilter = status;

        TextView[] chips = new TextView[]{binding.chipAll, binding.chipPending, binding.chipShipping, binding.chipCompleted, binding.chipReturned};
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(Color.parseColor("#555555"));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        selectedChip.setBackgroundResource(R.drawable.bg_chip_selected);
        selectedChip.setTextColor(Color.parseColor("#FFFFFF"));
        selectedChip.setTypeface(null, android.graphics.Typeface.BOLD);

        loadOrders(status);
    }

    private void loadOrders(String status) {
        if ("Tất cả".equals(status)) {
            orderRepository.getOrdersWithCountByUser(userId).observe(getViewLifecycleOwner(), this::updateOrderList);
        } else {
            orderRepository.getOrdersWithCountByUserAndStatus(userId, status).observe(getViewLifecycleOwner(), this::updateOrderList);
        }
    }

    private void updateOrderList(List<OrderWithCount> orders) {
        if (orders != null && !orders.isEmpty()) {
            adapter.setOrders(orders);
            binding.rvOrders.setVisibility(View.VISIBLE);
            binding.tvEmptyOrders.setVisibility(View.GONE);
        } else {
            adapter.setOrders(null);
            binding.rvOrders.setVisibility(View.GONE);
            binding.tvEmptyOrders.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onOrderDetailClick(Order order) {
        Intent intent = new Intent(getContext(), OrderDetailActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }

    @Override
    public void onCancelOrderClick(Order order) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #DH" + String.format("%04d", order.getId()) + "?")
                .setPositiveButton("Hủy đơn", (dialog, which) ->
                    orderRepository.cancelOrder(order, () -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                                loadOrders(currentStatusFilter);
                            });
                        }
                    })
                )
                .setNegativeButton("Quay lại", null)
                .show();
    }

    @Override
    public void onReviewOrderClick(Order order) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_review, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);

        new AlertDialog.Builder(getContext())
                .setTitle("Đánh giá đơn hàng #DH" + String.format("%04d", order.getId()))
                .setView(dialogView)
                .setPositiveButton("Gửi đánh giá", (dialog, which) -> {
                    float rating = ratingBar != null ? ratingBar.getRating() : 5;
                    Toast.makeText(getContext(), "Cảm ơn bạn đã đánh giá " + (int) rating + " sao!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onReturnOrderClick(Order order) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_return, null);
        EditText etReason = dialogView.findViewById(R.id.etReason);

        new AlertDialog.Builder(getContext())
                .setTitle("Yêu cầu trả hàng / hoàn tiền")
                .setMessage("Vui lòng nhập lý do muốn trả sản phẩm:")
                .setView(dialogView)
                .setPositiveButton("Gửi yêu cầu", (dialog, which) -> {
                    String reason = etReason != null ? etReason.getText().toString().trim() : "";
                    String noteText = "Trả hàng: " + (reason.isEmpty() ? "Đã hoàn tiền" : reason);
                    orderRepository.requestReturnOrder(order, noteText, () -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Đã gửi yêu cầu trả hàng thành công", Toast.LENGTH_SHORT).show();
                                loadOrders(currentStatusFilter);
                            });
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
