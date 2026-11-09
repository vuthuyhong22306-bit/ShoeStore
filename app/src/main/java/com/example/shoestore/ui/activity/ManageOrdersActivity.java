package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.shoestore.databinding.ActivityManageOrdersBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.ui.adapter.OrderAdapter;

public class ManageOrdersActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {
    private ActivityManageOrdersBinding binding;
    private OrderRepository orderRepository;
    private OrderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý đơn hàng");
        }

        orderRepository = new OrderRepository(getApplication());
        adapter = new OrderAdapter(this);
        binding.rvManageOrders.setAdapter(adapter);

        orderRepository.getAllOrders().observe(this, orders -> {
            if (orders != null) {
                adapter.setOrders(orders);
            }
        });
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("ORDER_ID", order.getId());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
