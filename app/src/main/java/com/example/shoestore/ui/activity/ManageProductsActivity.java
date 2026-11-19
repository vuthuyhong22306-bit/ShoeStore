package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityManageProductsBinding;
import com.example.shoestore.databinding.ItemManageProductBinding;
import com.example.shoestore.model.Category;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.ProductRepository;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageProductsActivity extends AppCompatActivity {
    private ActivityManageProductsBinding binding;
    private ProductRepository productRepository;
    private AdminProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();
    private Map<Integer, String> categoryNameMap = new HashMap<>();
    private int selectedCategoryId = -1; // -1 means All
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        productRepository = new ProductRepository(getApplication());
        adapter = new AdminProductAdapter();
        binding.rvProducts.setAdapter(adapter);

        productRepository.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                allCategories = categories;
                categoryNameMap.clear();
                for (Category c : categories) {
                    categoryNameMap.put(c.getId(), c.getName());
                }
                buildCategoryChips();
            }
        });

        productRepository.getAllProducts().observe(this, products -> {
            if (products != null) {
                allProducts = products;
                filterAndDisplayProducts();
            }
        });

        binding.etSearchProduct.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                filterAndDisplayProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.fabAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditProductActivity.class));
        });
    }

    private void buildCategoryChips() {
        binding.layoutCategoryChips.removeAllViews();

        // "Tất cả" Chip
        addChipView("Tất cả", -1);

        for (Category c : allCategories) {
            addChipView(c.getName(), c.getId());
        }
    }

    private void addChipView(String label, int categoryId) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(14);
        chip.setPadding(36, 18, 36, 18);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 16, 0);
        chip.setLayoutParams(params);

        boolean isSelected = (selectedCategoryId == categoryId);
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_admin_chip_selected);
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_admin_chip_unselected);
            chip.setTextColor(Color.parseColor("#1C1C1E"));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        chip.setOnClickListener(v -> {
            selectedCategoryId = categoryId;
            buildCategoryChips();
            filterAndDisplayProducts();
        });

        binding.layoutCategoryChips.addView(chip);
    }

    private void filterAndDisplayProducts() {
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            boolean matchesCategory = (selectedCategoryId == -1) || (p.getCategoryId() == selectedCategoryId);
            boolean matchesSearch = searchQuery.isEmpty() || (p.getName() != null && p.getName().toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                filtered.add(p);
            }
        }
        adapter.setProducts(filtered);
    }

    class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.AdminProductViewHolder> {
        private List<Product> products = new ArrayList<>();

        public void setProducts(List<Product> products) {
            this.products = products != null ? products : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AdminProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemManageProductBinding binding = ItemManageProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AdminProductViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminProductViewHolder holder, int position) {
            holder.bind(products.get(position));
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class AdminProductViewHolder extends RecyclerView.ViewHolder {
            private ItemManageProductBinding binding;

            public AdminProductViewHolder(ItemManageProductBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Product product) {
                binding.tvProductName.setText(product.getName());

                String categoryName = categoryNameMap.getOrDefault(product.getCategoryId(), "Giày Sneaker");
                int stock = product.getStock();

                if (stock <= ProductRepository.LOW_STOCK_THRESHOLD) {
                    String formattedStock = categoryName + " · Tồn: <font color='#D0343C'><b>" + stock + " (sắp hết)</b></font>";
                    binding.tvProductCategoryAndStock.setText(Html.fromHtml(formattedStock, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    binding.tvProductCategoryAndStock.setText(categoryName + " · Tồn: " + stock);
                }

                DecimalFormat formatter = new DecimalFormat("#,###");
                String formattedPrice = formatter.format(product.getPrice()).replace(",", ".") + "đ";
                binding.tvProductPrice.setText(formattedPrice);

                Glide.with(binding.ivProduct.getContext())
                        .load(product.getImage())
                        .placeholder(R.drawable.ic_sneaker_placeholder)
                        .error(R.drawable.ic_sneaker_placeholder)
                        .into(binding.ivProduct);

                binding.btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(ManageProductsActivity.this, AddEditProductActivity.class);
                    intent.putExtra("PRODUCT_ID", product.getId());
                    startActivity(intent);
                });

                binding.btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(ManageProductsActivity.this)
                            .setTitle("Xóa sản phẩm")
                            .setMessage("Bạn có chắc chắn muốn xóa sản phẩm \"" + product.getName() + "\"?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                productRepository.deleteProduct(product);
                                Toast.makeText(ManageProductsActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            }
        }
    }
}