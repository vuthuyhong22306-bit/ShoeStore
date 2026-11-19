package com.example.shoestore.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityAdminReviewsBinding;
import com.example.shoestore.databinding.ItemAdminReviewBinding;
import com.example.shoestore.model.Product;
import com.example.shoestore.model.Review;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.repository.ReviewRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminReviewsActivity extends AppCompatActivity {
    private ActivityAdminReviewsBinding binding;
    private ProductRepository productRepository;
    private ReviewRepository reviewRepository;
    private AdminReviewAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private List<Review> allReviews = new ArrayList<>();
    private int selectedProductId = -1; // -1 for all products

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        productRepository = new ProductRepository(getApplication());
        reviewRepository = new ReviewRepository(getApplication());
        adapter = new AdminReviewAdapter();
        binding.rvAdminReviews.setAdapter(adapter);

        // Load Products into Spinner
        productRepository.getAllProducts().observe(this, products -> {
            if (products != null) {
                productList = products;
                setupSpinner();
            }
        });

        // Load Reviews
        reviewRepository.getAllReviews().observe(this, reviews -> {
            allReviews = reviews != null ? reviews : new ArrayList<>();
            // Populate sample reviews if database is empty for demo/testing matching screenshot
            if (allReviews.isEmpty() && !productList.isEmpty()) {
                seedInitialReviews();
            }
            filterAndDisplayReviews();
        });
    }

    private void seedInitialReviews() {
        int pId1 = productList.get(0).getId();
        int pId2 = productList.size() > 1 ? productList.get(1).getId() : pId1;

        Review r1 = new Review(pId1, "Sneaker Runner 01", 101, "Nguyễn Văn A", 5.0f, "Giày đẹp, đi êm, đúng size.", "Size 42 · Đen", System.currentTimeMillis() - 86400000, false);
        Review r2 = new Review(pId1, "Sneaker Runner 01", 102, "Trần Thị B", 2.0f, "[Nội dung đánh giá không phù hợp]", "Size 40 · Đỏ", System.currentTimeMillis() - 172800000, true);
        Review r3 = new Review(pId1, "Sneaker Runner 01", 103, "Lê Văn C", 4.0f, "Giao hàng nhanh, chất lượng ổn.", "Size 41 · Trắng", System.currentTimeMillis() - 259200000, false);

        reviewRepository.insertReview(r1);
        reviewRepository.insertReview(r2);
        reviewRepository.insertReview(r3);
    }

    private void setupSpinner() {
        List<String> spinnerNames = new ArrayList<>();
        spinnerNames.add("Tất cả sản phẩm");
        for (Product p : productList) {
            spinnerNames.add(p.getName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerProducts.setAdapter(spinnerAdapter);

        binding.spinnerProducts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedProductId = -1;
                } else {
                    selectedProductId = productList.get(position - 1).getId();
                }
                filterAndDisplayReviews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void filterAndDisplayReviews() {
        List<Review> filtered = new ArrayList<>();
        float sumRating = 0;
        int count = 0;

        for (Review r : allReviews) {
            boolean matchesProduct = (selectedProductId == -1) || (r.getProductId() == selectedProductId);
            if (matchesProduct) {
                filtered.add(r);
                sumRating += r.getRating();
                count++;
            }
        }

        adapter.setReviews(filtered);

        if (count > 0) {
            float avg = sumRating / count;
            binding.tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f ★ trung bình", avg));
            binding.tvTotalReviewCount.setText(count + " đánh giá");
        } else {
            binding.tvAverageRating.setText("0.0 ★ trung bình");
            binding.tvTotalReviewCount.setText("0 đánh giá");
        }
    }

    class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.AdminReviewViewHolder> {
        private List<Review> reviews = new ArrayList<>();

        public void setReviews(List<Review> reviews) {
            this.reviews = reviews != null ? reviews : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AdminReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAdminReviewBinding binding = ItemAdminReviewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AdminReviewViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminReviewViewHolder holder, int position) {
            holder.bind(reviews.get(position));
        }

        @Override
        public int getItemCount() {
            return reviews.size();
        }

        class AdminReviewViewHolder extends RecyclerView.ViewHolder {
            private ItemAdminReviewBinding binding;

            public AdminReviewViewHolder(ItemAdminReviewBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Review review) {
                binding.tvReviewerName.setText(review.getUserName() != null ? review.getUserName() : "Khách hàng");
                binding.tvVariantInfo.setText(review.getVariantInfo() != null ? review.getVariantInfo() : "Size 40 · Đen");

                // Render Star Rating (filled red stars vs empty grey stars)
                int ratingInt = Math.round(review.getRating());
                StringBuilder starsBuilder = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    if (i < ratingInt) {
                        starsBuilder.append("★ ");
                    } else {
                        starsBuilder.append("☆ ");
                    }
                }
                binding.tvRatingStars.setText(starsBuilder.toString().trim());

                if (review.isHidden()) {
                    binding.tvHiddenBadge.setVisibility(View.VISIBLE);
                    binding.layoutRatingStars.setVisibility(View.VISIBLE);
                    binding.tvReviewComment.setText("[Nội dung đánh giá không phù hợp]");
                    binding.tvReviewComment.setTextColor(Color.parseColor("#6B6B6B"));

                    binding.btnToggleHide.setText("Hiện lại");
                    binding.btnToggleHide.setBackgroundResource(R.drawable.bg_btn_admin_black);
                    binding.btnToggleHide.setTextColor(Color.WHITE);
                } else {
                    binding.tvHiddenBadge.setVisibility(View.GONE);
                    binding.layoutRatingStars.setVisibility(View.VISIBLE);
                    binding.tvReviewComment.setText(review.getComment() != null ? review.getComment() : "");
                    binding.tvReviewComment.setTextColor(Color.parseColor("#1C1C1E"));

                    binding.btnToggleHide.setText("Ẩn");
                    binding.btnToggleHide.setBackgroundResource(R.drawable.bg_btn_admin_outlined);
                    binding.btnToggleHide.setTextColor(Color.parseColor("#1C1C1E"));
                }

                binding.btnToggleHide.setOnClickListener(v -> {
                    review.setHidden(!review.isHidden());
                    reviewRepository.updateReview(review);
                    Toast.makeText(AdminReviewsActivity.this, review.isHidden() ? "Đã ẩn đánh giá" : "Đã hiện lại đánh giá", Toast.LENGTH_SHORT).show();
                });

                binding.btnDeleteReview.setOnClickListener(v -> {
                    new AlertDialog.Builder(AdminReviewsActivity.this)
                            .setTitle("Xóa đánh giá")
                            .setMessage("Bạn có chắc chắn muốn xóa đánh giá của \"" + review.getUserName() + "\"?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                reviewRepository.deleteReview(review);
                                Toast.makeText(AdminReviewsActivity.this, "Đã xóa đánh giá", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            }
        }
    }
}