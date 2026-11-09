package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shoestore.databinding.FragmentOrdersBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.ui.activity.OrderDetailActivity;
import com.example.shoestore.ui.adapter.OrderAdapter;
import com.example.shoestore.utils.SharedPrefManager;

public class OrdersFragment extends Fragment implements OrderAdapter.OnOrderClickListener {
    private FragmentOrdersBinding binding;
    private OrderRepository orderRepository;
    private OrderAdapter adapter;

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

        int userId = SharedPrefManager.getInstance(getContext()).getUserId();
        orderRepository.getOrdersByUser(userId).observe(getViewLifecycleOwner(), orders -> {
            if (orders != null) {
                adapter.setOrders(orders);
            }
        });
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
