package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.shoestore.R;
import com.example.shoestore.databinding.FragmentStaffOrdersBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.ui.activity.OrderDetailActivity;
import com.example.shoestore.ui.activity.CustomerMainActivity;
import com.example.shoestore.ui.adapter.StaffOrderAdapter;
import com.example.shoestore.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

public class StaffOrdersFragment extends Fragment implements StaffOrderAdapter.OnOrderActionListener {
    private FragmentStaffOrdersBinding binding;
    private OrderRepository orderRepository;
    private StaffOrderAdapter adapter;
    private List<Order> allOrdersList = new ArrayList<>();
    private String selectedStatusFilter = "Tất cả";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStaffOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() == null) return;

        orderRepository = new OrderRepository(getActivity().getApplication());
        adapter = new StaffOrderAdapter(this);
        binding.rvStaffOrders.setAdapter(adapter);

        // Display Staff Greeting
        String staffName = SharedPrefManager.getInstance(getContext()).getUserName();
        if (staffName != null && !staffName.isEmpty()) {
            binding.tvStaffGreeting.setText("Xin chào, " + staffName);
        } else {
            binding.tvStaffGreeting.setText("Xin chào, Nguyễn Văn Staff");
        }

        // Header Logout Action
        binding.btnLogoutHeader.setOnClickListener(v -> performLogout());

        // Observe All Orders from Database
        orderRepository.getAllOrders().observe(getViewLifecycleOwner(), orders -> {
            allOrdersList = orders != null ? orders : new ArrayList<>();
            updateStatistics(allOrdersList);
            applyStatusFilter();
        });

        // Filter Chips Listener
        setupFilterChips();

        // Quick Stock Update Action
        binding.btnUpdateStockQuick.setOnClickListener(v -> showQuickStockUpdateDialog());
    }

    private void updateStatistics(List<Order> orders) {
        int pending = 0;
        int shipping = 0;
        int returns = 0;

        for (Order o : orders) {
            String st = o.getStatus();
            if ("Chờ xác nhận".equalsIgnoreCase(st)) pending++;
            else if ("Đang giao".equalsIgnoreCase(st)) shipping++;
            else if ("Yêu cầu trả hàng".equalsIgnoreCase(st) || "Từ chối".equalsIgnoreCase(st)) returns++;
        }

        binding.tvStatPendingCount.setText(String.valueOf(pending));
        binding.tvStatShippingCount.setText(String.valueOf(shipping));
        binding.tvStatReturnsCount.setText(String.valueOf(returns));
    }

    private void setupFilterChips() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipPending)) {
                selectedStatusFilter = "Chờ xác nhận";
            } else if (checkedIds.contains(R.id.chipShipping)) {
                selectedStatusFilter = "Đang giao";
            } else if (checkedIds.contains(R.id.chipCompleted)) {
                selectedStatusFilter = "Đã hoàn thành";
            } else if (checkedIds.contains(R.id.chipReturned)) {
                selectedStatusFilter = "Yêu cầu trả hàng";
            } else {
                selectedStatusFilter = "Tất cả";
            }
            applyStatusFilter();
        });
    }

    private void applyStatusFilter() {
        List<Order> filtered = new ArrayList<>();
        if ("Tất cả".equals(selectedStatusFilter)) {
            filtered.addAll(allOrdersList);
        } else {
            for (Order o : allOrdersList) {
                if (selectedStatusFilter.equalsIgnoreCase(o.getStatus())) {
                    filtered.add(o);
                }
            }
        }

        adapter.setOrders(filtered);

        if (filtered.isEmpty()) {
            binding.rvStaffOrders.setVisibility(View.GONE);
            binding.layoutEmptyOrders.setVisibility(View.VISIBLE);
        } else {
            binding.rvStaffOrders.setVisibility(View.VISIBLE);
            binding.layoutEmptyOrders.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConfirmOrder(Order order) {
        String nextStatus = "Đang giao";
        if ("Đang giao".equals(order.getStatus())) {
            nextStatus = "Đã hoàn thành";
        } else if ("Yêu cầu trả hàng".equals(order.getStatus())) {
            nextStatus = "Đã hoàn trả";
        }

        order.setStatus(nextStatus);
        orderRepository.updateOrderStatus(order);
        Toast.makeText(getContext(), "Đã cập nhật đơn #" + order.getId() + " sang trạng thái: " + nextStatus, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRejectOrder(Order order) {
        order.setStatus("Đã từ chối");
        orderRepository.updateOrderStatus(order);
        Toast.makeText(getContext(), "Đã từ chối đơn hàng #" + order.getId(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(getContext(), OrderDetailActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }

    private void showQuickStockUpdateDialog() {
        if (getContext() == null) return;
        EditText etStock = new EditText(getContext());
        etStock.setHint("Nhập số lượng tồn kho mới");
        etStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(getContext())
                .setTitle("Cập nhật tồn kho")
                .setMessage("Nhập số lượng sản phẩm tồn kho mới cho cửa hàng:")
                .setView(etStock)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String val = etStock.getText().toString().trim();
                    if (!val.isEmpty()) {
                        binding.tvStockItem1.setText(val + " đôi");
                        Toast.makeText(getContext(), "Cập nhật tồn kho thành công!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLogout() {
        if (getContext() == null) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.msg_confirm_logout_title)
                .setMessage(R.string.msg_confirm_logout_staff)
                .setPositiveButton(R.string.btn_logout, (dialog, which) -> {
                    SharedPrefManager.getInstance(getContext()).logout();
                    Intent intent = new Intent(getContext(), CustomerMainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
