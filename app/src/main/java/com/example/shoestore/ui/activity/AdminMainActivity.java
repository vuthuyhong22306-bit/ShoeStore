package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityAdminMainBinding;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.OrderDetail;
import com.example.shoestore.model.Product;
import com.example.shoestore.model.RevenueByDate;
import com.example.shoestore.repository.NotificationRepository;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.utils.SharedPrefManager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdminMainActivity extends AppCompatActivity {

    private ActivityAdminMainBinding binding;
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private List<RevenueByDate> dailyData = new ArrayList<>();
    private List<RevenueByDate> monthlyData = new ArrayList<>();
    private boolean isDailySelected = true;

    private List<Order> cachedOrders = new ArrayList<>();
    private List<OrderDetail> cachedOrderDetails = new ArrayList<>();
    private List<Product> cachedProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        orderRepository = new OrderRepository(getApplication());
        productRepository = new ProductRepository(getApplication());

        // Header Logout
        binding.btnLogoutAdmin.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.msg_confirm_logout_title)
                    .setMessage(R.string.msg_confirm_logout_admin)
                    .setPositiveButton(R.string.btn_logout, (dialog, which) -> {
                        SharedPrefManager.getInstance(this).logout();
                        Intent intent = new Intent(AdminMainActivity.this, CustomerMainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        });

        // Notification Center & Unread Count Badge
        binding.btnAdminNotification.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMainActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        NotificationRepository notificationRepository = new NotificationRepository(getApplication());
        String currentUserIdStr = String.valueOf(SharedPrefManager.getInstance(this).getUserId());
        String currentRoleStr = SharedPrefManager.getInstance(this).getUserRole();

        notificationRepository.getUnreadCountForUser(currentUserIdStr, currentRoleStr).observe(this, unreadCount -> {
            if (binding != null && binding.tvAdminNotifBadge != null) {
                if (unreadCount != null && unreadCount > 0) {
                    binding.tvAdminNotifBadge.setText(String.valueOf(unreadCount));
                    binding.tvAdminNotifBadge.setVisibility(View.VISIBLE);
                } else {
                    binding.tvAdminNotifBadge.setVisibility(View.GONE);
                }
            }
        });

        // Total Orders Count
        orderRepository.getTotalOrdersCount().observe(this, count -> {
            int total = count != null ? count : 0;
            DecimalFormat fmt = new DecimalFormat("#,###");
            binding.tvTotalOrdersCount.setText(fmt.format(total));
        });

        // Total Net Revenue
        orderRepository.getTotalRevenue().observe(this, revenue -> {
            double total = revenue != null ? revenue : 0.0;
            DecimalFormat fmt = new DecimalFormat("#,###");
            binding.tvTotalRevenueAmount.setText(fmt.format(total).replace(",", ".") + "đ");
        });

        // Revenue Chart Data & Toggle Chips
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

        setupChartToggleChips();

        // Observe Orders for Returns Count & Top Selling
        orderRepository.getAllOrders().observe(this, orders -> {
            cachedOrders = orders != null ? orders : new ArrayList<>();
            updatePendingReturnsCount();
            calculateTopSellingProducts();
        });

        // Observe OrderDetails for Top Selling
        orderRepository.getAllOrderDetails().observe(this, details -> {
            cachedOrderDetails = details != null ? details : new ArrayList<>();
            calculateTopSellingProducts();
        });

        // Observe Products for Low Stock Warnings & Top Selling names
        productRepository.getAllProducts().observe(this, products -> {
            cachedProducts = products != null ? products : new ArrayList<>();
            updateLowStockWarnings();
            calculateTopSellingProducts();
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

    private void updatePendingReturnsCount() {
        int returnCount = 0;
        for (Order o : cachedOrders) {
            String st = o.getStatus() != null ? o.getStatus() : "";
            if (st.contains("trả hàng") || st.contains("hoàn") || st.contains("từ chối") || st.equalsIgnoreCase("Chờ duyệt")) {
                returnCount++;
            }
        }
        binding.tvPendingReturnsBadge.setText(String.valueOf(returnCount));
    }

    private void updateLowStockWarnings() {
        List<Product> lowStockProducts = new ArrayList<>();
        for (Product p : cachedProducts) {
            if (p.getStock() <= ProductRepository.LOW_STOCK_THRESHOLD) {
                lowStockProducts.add(p);
            }
        }

        if (lowStockProducts.isEmpty()) {
            binding.cardLowStockWarning.setVisibility(View.GONE);
            return;
        }

        binding.cardLowStockWarning.setVisibility(View.VISIBLE);
        Collections.sort(lowStockProducts, (p1, p2) -> Integer.compare(p1.getStock(), p2.getStock()));

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(lowStockProducts.size(), 5);
        for (int i = 0; i < limit; i++) {
            Product p = lowStockProducts.get(i);
            if (p.getStock() <= 0) {
                sb.append(p.getName()).append(" — hết hàng\n");
            } else {
                sb.append(p.getName()).append(" · Size 42 · Đỏ — còn ").append(p.getStock()).append("\n");
            }
        }
        binding.tvLowStockWarningText.setText(sb.toString().trim());
    }

    private void calculateTopSellingProducts() {
        if (cachedOrders.isEmpty() || cachedProducts.isEmpty()) {
            displayTopSelling(new ArrayList<>());
            return;
        }

        // Set of completed order IDs
        Set<Integer> completedOrderIds = new HashSet<>();
        for (Order o : cachedOrders) {
            String st = o.getStatus() != null ? o.getStatus() : "";
            if ("Đã hoàn thành".equalsIgnoreCase(st)) {
                String note = o.getNote() != null ? o.getNote() : "";
                if (!note.contains("hoàn tiền") && !note.contains("Trả hàng")) {
                    completedOrderIds.add(o.getId());
                }
            }
        }

        // Aggregate sales per product
        Map<Integer, Integer> productSalesMap = new HashMap<>();
        for (OrderDetail detail : cachedOrderDetails) {
            if (completedOrderIds.contains(detail.getOrderId())) {
                int pId = detail.getProductId();
                int qty = detail.getQuantity();
                productSalesMap.put(pId, productSalesMap.getOrDefault(pId, 0) + qty);
            }
        }

        // Map product ID to Product model
        Map<Integer, Product> productMap = new HashMap<>();
        for (Product p : cachedProducts) {
            productMap.put(p.getId(), p);
        }

        List<ProductSales> salesList = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : productSalesMap.entrySet()) {
            Product p = productMap.get(entry.getKey());
            if (p != null && entry.getValue() > 0) {
                salesList.add(new ProductSales(p.getName(), entry.getValue()));
            }
        }

        // Sort descending by sales count, then A-Z by name
        Collections.sort(salesList, (o1, o2) -> {
            int comp = Integer.compare(o2.salesCount, o1.salesCount);
            if (comp != 0) return comp;
            return o1.productName.compareToIgnoreCase(o2.productName);
        });

        displayTopSelling(salesList);
    }

    private void displayTopSelling(List<ProductSales> topList) {
        View layoutTop1 = (View) binding.tvTop1Name.getParent();
        View layoutTop2 = (View) binding.tvTop2Name.getParent();
        View layoutTop3 = (View) binding.tvTop3Name.getParent();

        if (topList.isEmpty()) {
            binding.tvTop1Name.setText("Chưa có dữ liệu bán hàng");
            binding.tvTop1Name.setTextColor(Color.parseColor("#8A8A8E"));
            binding.tvTop1Count.setVisibility(View.GONE);
            if (layoutTop1 != null) layoutTop1.setVisibility(View.VISIBLE);
            if (layoutTop2 != null) layoutTop2.setVisibility(View.GONE);
            if (layoutTop3 != null) layoutTop3.setVisibility(View.GONE);
            return;
        }

        binding.tvTop1Name.setTextColor(Color.parseColor("#1F2023"));

        // Rank 1
        if (topList.size() >= 1) {
            ProductSales item1 = topList.get(0);
            String formattedName = "<font color='#DC2626'><b>1. </b></font>" + item1.productName;
            binding.tvTop1Name.setText(Html.fromHtml(formattedName, Html.FROM_HTML_MODE_LEGACY));
            binding.tvTop1Count.setText(item1.salesCount + " đã bán");
            binding.tvTop1Count.setVisibility(View.VISIBLE);
            if (layoutTop1 != null) layoutTop1.setVisibility(View.VISIBLE);
        }

        // Rank 2
        if (topList.size() >= 2) {
            ProductSales item2 = topList.get(1);
            String formattedName = "<font color='#DC2626'><b>2. </b></font>" + item2.productName;
            binding.tvTop2Name.setText(Html.fromHtml(formattedName, Html.FROM_HTML_MODE_LEGACY));
            binding.tvTop2Count.setText(item2.salesCount + " đã bán");
            if (layoutTop2 != null) layoutTop2.setVisibility(View.VISIBLE);
        } else {
            if (layoutTop2 != null) layoutTop2.setVisibility(View.GONE);
        }

        // Rank 3
        if (topList.size() >= 3) {
            ProductSales item3 = topList.get(2);
            String formattedName = "<font color='#DC2626'><b>3. </b></font>" + item3.productName;
            binding.tvTop3Name.setText(Html.fromHtml(formattedName, Html.FROM_HTML_MODE_LEGACY));
            binding.tvTop3Count.setText(item3.salesCount + " đã bán");
            if (layoutTop3 != null) layoutTop3.setVisibility(View.VISIBLE);
        } else {
            if (layoutTop3 != null) layoutTop3.setVisibility(View.GONE);
        }
    }

    private void setupChartToggleChips() {
        binding.btnChartDaily.setOnClickListener(v -> {
            isDailySelected = true;
            binding.btnChartDaily.setBackgroundResource(R.drawable.bg_admin_chip_selected);
            binding.btnChartDaily.setTextColor(Color.WHITE);
            binding.btnChartDaily.setTypeface(null, android.graphics.Typeface.BOLD);

            binding.btnChartMonthly.setBackgroundResource(R.drawable.bg_admin_chip_unselected);
            binding.btnChartMonthly.setTextColor(Color.parseColor("#1F2023"));
            binding.btnChartMonthly.setTypeface(null, android.graphics.Typeface.NORMAL);

            binding.adminBarChartView.setData(dailyData);
        });

        binding.btnChartMonthly.setOnClickListener(v -> {
            isDailySelected = false;
            binding.btnChartMonthly.setBackgroundResource(R.drawable.bg_admin_chip_selected);
            binding.btnChartMonthly.setTextColor(Color.WHITE);
            binding.btnChartMonthly.setTypeface(null, android.graphics.Typeface.BOLD);

            binding.btnChartDaily.setBackgroundResource(R.drawable.bg_admin_chip_unselected);
            binding.btnChartDaily.setTextColor(Color.parseColor("#1F2023"));
            binding.btnChartDaily.setTypeface(null, android.graphics.Typeface.NORMAL);

            binding.adminBarChartView.setData(monthlyData);
        });
    }

    private static class ProductSales {
        String productName;
        int salesCount;

        ProductSales(String productName, int salesCount) {
            this.productName = productName;
            this.salesCount = salesCount;
        }
    }
}