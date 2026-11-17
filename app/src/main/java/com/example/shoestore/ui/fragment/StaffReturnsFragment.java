package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shoestore.databinding.FragmentStaffReturnsBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.ui.activity.OrderDetailActivity;
import com.example.shoestore.ui.adapter.StaffOrderAdapter;

import java.util.ArrayList;
import java.util.List;

public class StaffReturnsFragment extends Fragment implements StaffOrderAdapter.OnOrderActionListener {
    private FragmentStaffReturnsBinding binding;
    private OrderRepository orderRepository;
    private StaffOrderAdapter adapter;

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
        adapter = new StaffOrderAdapter(this);
        binding.rvReturnOrders.setAdapter(adapter);

        orderRepository.getAllOrders().observe(getViewLifecycleOwner(), orders -> {
            List<Order> returnsList = new ArrayList<>();
            if (orders != null) {
                for (Order o : orders) {
                    if ("Yêu cầu trả hàng".equalsIgnoreCase(o.getStatus()) || "Đã từ chối".equalsIgnoreCase(o.getStatus())) {
                        returnsList.add(o);
                    }
                }
            }
            adapter.setOrders(returnsList);
        });
    }

    @Override
    public void onConfirmOrder(Order order) {
        order.setStatus("Đã chấp nhận trả");
        orderRepository.updateOrderStatus(order);
    }

    @Override
    public void onRejectOrder(Order order) {
        order.setStatus("Đã từ chối trả");
        orderRepository.updateOrderStatus(order);
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(getContext(), OrderDetailActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
