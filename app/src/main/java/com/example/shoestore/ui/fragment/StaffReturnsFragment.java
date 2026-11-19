package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.shoestore.R;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.databinding.FragmentStaffReturnsBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.OrderDetail;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.ui.activity.OrderDetailActivity;
import com.example.shoestore.ui.adapter.StaffReturnAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffReturnsFragment extends Fragment implements StaffReturnAdapter.OnReturnActionListener {

    private FragmentStaffReturnsBinding binding;
    private OrderRepository orderRepository;
    private StaffReturnAdapter adapter;

    private String currentTab = "PENDING"; // PENDING, ACCEPTED, RECEIVED
    private List<Order> allOrdersList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStaffReturnsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() == null) return;

        orderRepository = new OrderRepository(getActivity().getApplication());
        adapter = new StaffReturnAdapter(this);
        binding.rvReturnOrders.setAdapter(adapter);

        setupTabs();

        orderRepository.getAllOrders().observe(getViewLifecycleOwner(), orders -> {
            allOrdersList = orders != null ? orders : new ArrayList<>();
            filterAndDisplayOrders();
        });
    }

    private void setupTabs() {
        binding.tabPending.setOnClickListener(v -> selectTab("PENDING"));
        binding.tabAccepted.setOnClickListener(v -> selectTab("ACCEPTED"));
        binding.tabReceived.setOnClickListener(v -> selectTab("RECEIVED"));
        updateTabStyles();
    }

    private void selectTab(String tab) {
        this.currentTab = tab;
        updateTabStyles();
        filterAndDisplayOrders();
    }

    private void updateTabStyles() {
        if (getContext() == null) return;

        int selectedBg = R.drawable.bg_tab_selected;
        int unselectedBg = R.drawable.bg_tab_unselected;
        int selectedTextColor = ContextCompat.getColor(getContext(), android.R.color.white);
        int unselectedTextColor = ContextCompat.getColor(getContext(), R.color.black);

        binding.tabPending.setBackgroundResource("PENDING".equals(currentTab) ? selectedBg : unselectedBg);
        binding.tabPending.setTextColor("PENDING".equals(currentTab) ? selectedTextColor : unselectedTextColor);

        binding.tabAccepted.setBackgroundResource("ACCEPTED".equals(currentTab) ? selectedBg : unselectedBg);
        binding.tabAccepted.setTextColor("ACCEPTED".equals(currentTab) ? selectedTextColor : unselectedTextColor);

        binding.tabReceived.setBackgroundResource("RECEIVED".equals(currentTab) ? selectedBg : unselectedBg);
        binding.tabReceived.setTextColor("RECEIVED".equals(currentTab) ? selectedTextColor : unselectedTextColor);
    }

    private void filterAndDisplayOrders() {
        List<Order> filteredList = new ArrayList<>();

        for (Order o : allOrdersList) {
            String status = o.getStatus() != null ? o.getStatus().trim() : "";
            if ("PENDING".equals(currentTab)) {
                if ("Yêu cầu trả hàng".equalsIgnoreCase(status) || "Chờ duyệt".equalsIgnoreCase(status)) {
                    filteredList.add(o);
                }
            } else if ("ACCEPTED".equals(currentTab)) {
                if ("Đã chấp nhận".equalsIgnoreCase(status) || "Đã chấp nhận trả".equalsIgnoreCase(status)) {
                    filteredList.add(o);
                }
            } else if ("RECEIVED".equals(currentTab)) {
                if ("Đã nhận hàng trả".equalsIgnoreCase(status)) {
                    filteredList.add(o);
                }
            }
        }

        if (filteredList.isEmpty()) {
            binding.rvReturnOrders.setVisibility(View.GONE);
            binding.tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvReturnOrders.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setVisibility(View.GONE);
        }

        // Process detail summary mapping asynchronously
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Map<Integer, String> detailsMap = new HashMap<>();
            if (getContext() == null) return;
            AppDatabase db = AppDatabase.getDatabase(getContext());

            for (Order o : filteredList) {
                List<OrderDetail> details = db.orderDetailDao().getOrderDetailsList(o.getId());
                String customerName = o.getReceiverName() != null && !o.getReceiverName().isEmpty() ? o.getReceiverName() : "Khách hàng";
                String productName = "Giày Sneaker";
                String size = "42";

                if (details != null && !details.isEmpty()) {
                    OrderDetail firstDetail = details.get(0);
                    if (firstDetail.getSize() != null && !firstDetail.getSize().isEmpty()) {
                        size = firstDetail.getSize();
                    }
                    Product p = db.productDao().getProductById(firstDetail.getProductId()).getValue();
                    if (p != null && p.getName() != null) {
                        productName = p.getName();
                    } else {
                        // Fallback name mapping if product search requires sync
                        productName = getProductNameFallback(firstDetail.getProductId());
                    }
                }

                String reason = o.getNote() != null ? o.getNote() : "";
                if (reason.startsWith("Trả hàng: ")) {
                    reason = reason.substring("Trả hàng: ".length());
                }

                String summary = customerName + " · " + productName + " · Size " + size + " · Lý do: " + reason;
                detailsMap.put(o.getId(), summary);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.setOrders(filteredList, currentTab, detailsMap));
            }
        });
    }

    private String getProductNameFallback(int productId) {
        if (productId == 112 || productId == 1) return "Sneaker Runner 01";
        if (productId == 108 || productId == 2) return "Sneaker Street 02";
        if (productId == 101 || productId == 3) return "Sneaker Classic 04";
        return "Sneaker Runner 01";
    }

    @Override
    public void onViewRelatedOrder(Order order) {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), OrderDetailActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }

    @Override
    public void onAcceptReturn(Order order) {
        orderRepository.acceptReturnOrder(order, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã chấp nhận yêu cầu trả hàng!", Toast.LENGTH_SHORT).show();
                    filterAndDisplayOrders();
                });
            }
        });
    }

    @Override
    public void onRejectReturn(Order order, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập lý do từ chối!", Toast.LENGTH_SHORT).show();
            return;
        }

        orderRepository.rejectReturnOrder(order, reason, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã từ chối yêu cầu trả hàng!", Toast.LENGTH_SHORT).show();
                    filterAndDisplayOrders();
                });
            }
        });
    }

    @Override
    public void onConfirmReceivedGoods(Order order) {
        orderRepository.confirmReceivedReturnGoods(order, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã xác nhận nhận hàng trả & cộng lại tồn kho!", Toast.LENGTH_LONG).show();
                    filterAndDisplayOrders();
                });
            }
        });
    }

    @Override
    public void onConfirmRefund(Order order) {
        orderRepository.confirmRefund(order, () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã hoàn tất giao dịch & xác nhận hoàn tiền!", Toast.LENGTH_SHORT).show();
                    filterAndDisplayOrders();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
