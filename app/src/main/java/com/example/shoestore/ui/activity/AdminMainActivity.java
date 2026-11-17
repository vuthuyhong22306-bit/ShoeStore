package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityAdminMainBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.Product;
import com.example.shoestore.model.RevenueByDate;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.utils.SharedPrefManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminMainActivity extends AppCompatActivity {

    private ActivityAdminMainBinding binding;
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private List<RevenueByDate> dailyData = new ArrayList<>();
    private List<RevenueByDate> monthlyData = new ArrayList<>();
    private boolean isDailySelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderRepository = new OrderRepository(getApplication());
        productRepository = new ProductRepository(getApplication());

        // Header Greeting & Logout
        String adminName = SharedPrefManager.getInstance(this).getUserName();
        if (adminName != null && !adminName.isEmpty()) {
            binding.tvAdminGreeting.setText("Bảng điều khiển - " + adminName);
        }

        binding.btnLogoutAdmin.setOnClickListener(v -> {
            SharedPrefManager.getInstance(this).logout();
            Intent intent = new Intent(AdminMainActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Overview Statistics from Database (0 if no orders exist)
        orderRepository.getTotalOrdersCount().observe(this, count -> {
            int total = count != null ? count : 0;
            binding.tvTotalOrdersCount.setText(String.valueOf(total));
        });

        orderRepository.getTotalRevenue().observe(this, revenue -> {
            double total = revenue != null ? revenue : 0.0;
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvTotalRevenueAmount.setText(formatter.format(total));
        });

        // Revenue Chart Data Observation
        orderRepository.getDailyRevenue().observe(this, data -> {
            dailyData = data != null ? data : new ArrayList<>();
            if (isDailySelected) {
                binding.adminBarChartView.setData(dailyData);
            }
        });

        orderRepository.getMonthlyRevenue().observe(this, data -> {
            monthlyData = data != null ? data : new ArrayList<>();
            if (!isDailySelected) {
                binding.adminBarChartView.setData(monthlyData);
            }
        });

        binding.toggleChartGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnChartDaily) {
                    isDailySelected = true;
                    binding.adminBarChartView.setData(dailyData);
                } else if (checkedId == R.id.btnChartMonthly) {
                    isDailySelected = false;
                    binding.adminBarChartView.setData(monthlyData);
                }
            }
        });

        // Real Stock Monitoring for Low Stock Warning (Stock <= 5)
        productRepository.getAllProducts().observe(this, products -> {
            if (products != null && !products.isEmpty()) {
                StringBuilder warningBuilder = new StringBuilder();
                int lowStockCount = 0;
                for (Product p : products) {
                    if (p.getStock() <= 5) {
                        lowStockCount++;
                        warningBuilder.append("• ").append(p.getName()).append(" — còn ").append(p.getStock()).append(" đôi\n");
                    }
                }
                if (lowStockCount > 0) {
                    binding.tvLowStockWarningText.setText(warningBuilder.toString().trim());
                } else {
                    binding.tvLowStockWarningText.setText("Tất cả sản phẩm còn đủ hàng");
                }
            } else {
                binding.tvLowStockWarningText.setText("Chưa có sản phẩm trong kho");
            }
        });

        // Real Order Returns Monitoring
        orderRepository.getAllOrders().observe(this, orders -> {
            int pendingReturns = 0;
            if (orders != null) {
                for (Order o : orders) {
                    if ("Yêu cầu trả hàng".equalsIgnoreCase(o.getStatus())) {
                        pendingReturns++;
                    }
                }
            }
            binding.tvPendingReturnsBadge.setText(String.valueOf(pendingReturns));
        });

        // Wire 8 Grid Management Menu Buttons
        binding.btnMenuProducts.setOnClickListener(v -> startActivity(new Intent(this, ManageProductsActivity.class)));
        binding.btnMenuCategories.setOnClickListener(v -> startActivity(new Intent(this, ManageCategoriesActivity.class)));
        binding.btnMenuAccounts.setOnClickListener(v -> startActivity(new Intent(this, AdminAccountsActivity.class)));
        binding.btnMenuOrders.setOnClickListener(v -> startActivity(new Intent(this, ManageOrdersActivity.class)));
        binding.btnMenuReturns.setOnClickListener(v -> startActivity(new Intent(this, AdminReturnsActivity.class)));
        binding.btnMenuDiscounts.setOnClickListener(v -> startActivity(new Intent(this, AdminDiscountsActivity.class)));
        binding.btnMenuReviews.setOnClickListener(v -> startActivity(new Intent(this, AdminReviewsActivity.class)));
        binding.btnMenuStatistics.setOnClickListener(v -> startActivity(new Intent(this, StatisticsActivity.class)));

        // Returns Card Action
        binding.cardReturnsQuickSummary.setOnClickListener(v -> startActivity(new Intent(this, AdminReturnsActivity.class)));
    }
}
