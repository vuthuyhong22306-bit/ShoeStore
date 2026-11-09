package com.example.shoestore.ui.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.shoestore.databinding.ActivityProductDetailBinding;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.CartRepository;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {
    private ActivityProductDetailBinding binding;
    private ProductRepository productRepository;
    private CartRepository cartRepository;
    private Product currentProduct;
    private String selectedSize = "";
    private String selectedColor = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        int productId = getIntent().getIntExtra("PRODUCT_ID", -1);
        productRepository = new ProductRepository(getApplication());
        cartRepository = new CartRepository(getApplication());

        productRepository.getProductById(productId).observe(this, product -> {
            if (product != null) {
                currentProduct = product;
                displayProductDetails(product);
            }
        });

        binding.btnAddToCart.setOnClickListener(v -> addToCart());
    }

    private void displayProductDetails(Product product) {
        binding.tvProductName.setText(product.getName());
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        binding.tvProductPrice.setText(formatter.format(product.getPrice()));
        binding.tvProductDescription.setText(product.getDescription());
        binding.tvStock.setText("Tồn kho: " + product.getStock());

        Glide.with(this).load(product.getImage()).into(binding.ivProduct);

        binding.chipGroupSizes.removeAllViews();
        if (product.getSizes() != null && !product.getSizes().isEmpty()) {
            String[] sizes = product.getSizes().split(",");
            for (String size : sizes) {
                Chip chip = new Chip(this);
                chip.setId(View.generateViewId());
                chip.setText(size.trim());
                chip.setCheckable(true);
                binding.chipGroupSizes.addView(chip);
            }
        }
        binding.chipGroupSizes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) selectedSize = chip.getText().toString();
            } else {
                selectedSize = "";
            }
        });

        binding.chipGroupColors.removeAllViews();
        if (product.getColors() != null && !product.getColors().isEmpty()) {
            String[] colors = product.getColors().split(",");
            for (String color : colors) {
                Chip chip = new Chip(this);
                chip.setId(View.generateViewId());
                chip.setText(color.trim());
                chip.setCheckable(true);
                binding.chipGroupColors.addView(chip);
            }
        }
        binding.chipGroupColors.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) selectedColor = chip.getText().toString();
            } else {
                selectedColor = "";
            }
        });
    }

    private void addToCart() {
        if (selectedSize.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Vui lòng chọn size", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (selectedColor.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Vui lòng chọn màu", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (currentProduct == null || currentProduct.getStock() <= 0) {
            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Sản phẩm đã hết hàng", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            return;
        }

        int userId = SharedPrefManager.getInstance(this).getUserId();
        CartItem cartItem = new CartItem(userId, currentProduct.getId(), selectedSize, selectedColor, 1);
        cartRepository.addToCart(cartItem, currentProduct.getStock(), new CartRepository.OnCartActionCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Đã thêm vào giỏ hàng!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show());
            }
        });
    }
}
