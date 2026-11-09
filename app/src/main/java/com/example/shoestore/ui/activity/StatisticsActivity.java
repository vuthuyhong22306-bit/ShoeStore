package com.example.shoestore.ui.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityStatisticsBinding;
import com.example.shoestore.model.RevenueByDate;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.ui.adapter.RevenueAdapter;
import com.example.shoestore.utils.SharedPrefManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {
    private ActivityStatisticsBinding binding;
    private OrderRepository orderRepository;
    private RevenueAdapter adapter;
    private List<RevenueByDate> dailyData = new ArrayList<>();
    private List<RevenueByDate> monthlyData = new ArrayList<>();
    private boolean isDailySelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String role = SharedPrefManager.getInstance(this).getUserRole();
        if (!"ADMIN".equals(role)) {
            finish();
            return;
        }

        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thống kê doanh thu");
        }

        orderRepository = new OrderRepository(getApplication());
        adapter = new RevenueAdapter();
        binding.rvRevenue.setAdapter(adapter);

        orderRepository.getTotalOrdersCount().observe(this, count -> {
            binding.tvTotalOrders.setText(String.valueOf(count != null ? count : 0));
        });

        orderRepository.getTotalRevenue().observe(this, revenue -> {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvTotalRevenue.setText(formatter.format(revenue != null ? revenue : 0));
        });

        orderRepository.getDailyRevenue().observe(this, data -> {
            dailyData = data != null ? data : new ArrayList<>();
            if (isDailySelected) {
                updateChartAndList(dailyData, "Biểu đồ doanh thu theo ngày");
            }
        });

        orderRepository.getMonthlyRevenue().observe(this, data -> {
            monthlyData = data != null ? data : new ArrayList<>();
            if (!isDailySelected) {
                updateChartAndList(monthlyData, "Biểu đồ doanh thu theo tháng");
            }
        });

        binding.toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnDaily) {
                    isDailySelected = true;
                    updateChartAndList(dailyData, "Biểu đồ doanh thu theo ngày");
                } else if (checkedId == R.id.btnMonthly) {
                    isDailySelected = false;
                    updateChartAndList(monthlyData, "Biểu đồ doanh thu theo tháng");
                }
            }
        });
    }

    private void updateChartAndList(List<RevenueByDate> list, String title) {
        binding.tvChartTitle.setText(title);
        binding.barChartView.setData(list);
        adapter.setData(list);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
