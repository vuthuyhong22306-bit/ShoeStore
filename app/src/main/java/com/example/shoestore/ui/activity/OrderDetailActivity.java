package com.example.shoestore.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.shoestore.databinding.ActivityOrderDetailBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.ui.adapter.OrderDetailAdapter;
import com.example.shoestore.utils.SharedPrefManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {
    private ActivityOrderDetailBinding binding;
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private OrderDetailAdapter adapter;
    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int orderId = getIntent().getIntExtra("ORDER_ID", -1);
        orderRepository = new OrderRepository(getApplication());
        productRepository = new ProductRepository(getApplication());
        adapter = new OrderDetailAdapter(productRepository, this);
        binding.rvOrderDetails.setAdapter(adapter);

        orderRepository.getOrderById(orderId).observe(this, order -> {
            if (order != null) {
                currentOrder = order;
                displayOrderInfo(order);
                checkPermissions();
            }
        });

        orderRepository.getOrderDetails(orderId).observe(this, details -> {
            if (details != null) {
                adapter.setDetails(details);
            }
        });

        binding.btnUpdateStatus.setOnClickListener(v -> showUpdateStatusDialog());
    }

    private void displayOrderInfo(Order order) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String info = "Mã đơn: #" + order.getId() + "\n" +
                     "Ngày đặt: " + sdf.format(new Date(order.getOrderDate())) + "\n" +
                     "Trạng thái: " + order.getStatus();
        binding.tvOrderInfo.setText(info);

        String delivery = "Người nhận: " + order.getReceiverName() + "\n" +
                          "SĐT: " + order.getPhoneNumber() + "\n" +
                          "Địa chỉ: " + order.getDeliveryAddress() + "\n" +
                          "Thanh toán: " + order.getPaymentMethod();
        binding.tvDeliveryInfo.setText(delivery);

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        binding.tvTotalAmount.setText(formatter.format(order.getTotalAmount()));
    }

    private void checkPermissions() {
        String role = SharedPrefManager.getInstance(this).getUserRole();
        if (("ADMIN".equals(role) || "STAFF".equals(role)) && !"Đã hoàn thành".equals(currentOrder.getStatus())) {
            binding.btnUpdateStatus.setVisibility(View.VISIBLE);
        } else {
            binding.btnUpdateStatus.setVisibility(View.GONE);
        }
    }

    private void showUpdateStatusDialog() {
        String nextStatus = "";
        if ("Chờ xác nhận".equals(currentOrder.getStatus())) {
            nextStatus = "Đang giao";
        } else if ("Đang giao".equals(currentOrder.getStatus())) {
            nextStatus = "Đã hoàn thành";
        }

        if (nextStatus.isEmpty()) return;

        final String statusToUpdate = nextStatus;
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật trạng thái")
                .setMessage("Chuyển trạng thái đơn hàng sang: " + statusToUpdate + "?")
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    currentOrder.setStatus(statusToUpdate);
                    orderRepository.updateOrderStatus(currentOrder);
                    Toast.makeText(this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
