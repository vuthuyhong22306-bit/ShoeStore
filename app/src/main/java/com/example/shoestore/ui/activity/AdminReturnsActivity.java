package com.example.shoestore.ui.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivityAdminReturnsBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.ui.adapter.StaffOrderAdapter;

import java.util.ArrayList;
import java.util.List;

public class AdminReturnsActivity extends AppCompatActivity implements StaffOrderAdapter.OnOrderActionListener {
    private ActivityAdminReturnsBinding binding;
    private OrderRepository orderRepository;
    private StaffOrderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReturnsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý trả hàng");
        }

        orderRepository = new OrderRepository(getApplication());
        adapter = new StaffOrderAdapter(this);
        binding.rvAdminReturnOrders.setAdapter(adapter);

        orderRepository.getAllOrders().observe(this, orders -> {
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
        order.setStatus("Đã hoàn tiền");
        orderRepository.updateOrderStatus(order);
    }

    @Override
    public void onRejectOrder(Order order) {
        order.setStatus("Từ chối hoàn tiền");
        orderRepository.updateOrderStatus(order);
    }

    @Override
    public void onOrderClick(Order order) {
        // View detail
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
