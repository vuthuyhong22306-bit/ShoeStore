package com.example.shoestore.ui.activity;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivityCheckoutBinding;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.CartRepository;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {
    private ActivityCheckoutBinding binding;
    private OrderRepository orderRepository;
    private CartRepository cartRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private double totalAmount = 0;
    private List<CartItem> cartItemsList = new ArrayList<>();
    private List<Product> allProductsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thanh toán");
        }

        orderRepository = new OrderRepository(getApplication());
        cartRepository = new CartRepository(getApplication());
        productRepository = new ProductRepository(getApplication());
        userRepository = new UserRepository(getApplication());

        totalAmount = getIntent().getDoubleExtra("TOTAL_PRICE", 0);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        binding.tvTotalAmount.setText(formatter.format(totalAmount));

        int userId = SharedPrefManager.getInstance(this).getUserId();
        userRepository.getUserById(userId).observe(this, user -> {
            if (user != null) {
                if (binding.etReceiverName.getText().toString().isEmpty()) {
                    binding.etReceiverName.setText(user.getFullName());
                }
                if (binding.etPhone.getText().toString().isEmpty()) {
                    binding.etPhone.setText(user.getPhoneNumber());
                }
                if (binding.etAddress.getText().toString().isEmpty()) {
                    binding.etAddress.setText(user.getAddress());
                }
            }
        });

        cartRepository.getCartItems(userId).observe(this, items -> {
            if (items != null) {
                cartItemsList = items;
            }
        });

        productRepository.getAllProducts().observe(this, products -> {
            if (products != null) {
                allProductsList = products;
            }
        });

        binding.btnConfirmOrder.setOnClickListener(v -> confirmOrder());
    }

    private void confirmOrder() {
        String name = binding.etReceiverName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String payment = binding.rbCod.isChecked() ? "COD" : "Chuyển khoản";

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Vui lòng nhập đầy đủ thông tin giao hàng", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (cartItemsList == null || cartItemsList.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Giỏ hàng của bạn đang trống!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Map<Integer, Product> pMap = new HashMap<>();
        for (Product p : allProductsList) {
            pMap.put(p.getId(), p);
        }

        List<Double> prices = new ArrayList<>();
        double calculatedTotal = 0;

        for (CartItem item : cartItemsList) {
            Product p = pMap.get(item.getProductId());
            if (p == null) {
                Snackbar.make(binding.getRoot(), "Sản phẩm không tồn tại!", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (p.getStock() < item.getQuantity()) {
                Snackbar.make(binding.getRoot(), "Sản phẩm \"" + p.getName() + "\" không đủ số lượng tồn kho (Còn: " + p.getStock() + ")", Snackbar.LENGTH_LONG).show();
                return;
            }
            prices.add(p.getPrice());
            calculatedTotal += p.getPrice() * item.getQuantity();
        }

        int userId = SharedPrefManager.getInstance(this).getUserId();
        Order order = new Order(userId, name, phone, address, payment, calculatedTotal > 0 ? calculatedTotal : totalAmount, System.currentTimeMillis(), "Chờ xác nhận");

        orderRepository.placeOrder(order, cartItemsList, prices, () -> {
            runOnUiThread(() -> {
                new AlertDialog.Builder(CheckoutActivity.this)
                        .setTitle("Đặt hàng thành công")
                        .setMessage("Đơn hàng của bạn đã được khởi tạo với trạng thái: Chờ xác nhận.")
                        .setPositiveButton("Đồng ý", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
