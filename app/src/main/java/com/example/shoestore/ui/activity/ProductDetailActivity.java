package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.shoestore.R;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.CartRepository;
import com.example.shoestore.repository.FavoriteRepository;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {
    private ProductRepository productRepository;
    private CartRepository cartRepository;
    private FavoriteRepository favoriteRepository;
    private Product currentProduct;
    private String selectedSize = "";
    private String selectedColor = "";
    private int quantity = 1;
    private boolean isFavorite = false;

    private ImageView ivProduct;
    private Toolbar toolbar;
    private TextView tvProductName;
    private TextView tvProductPrice;
    private TextView tvProductDescription;
    private ChipGroup chipGroupSizes;
    private ChipGroup chipGroupColors;
    private TextView tvStock;
    private View btnAddToCart;
    private View btnBuyNow;

    private FrameLayout btnBack;
    private FrameLayout btnFavorite;
    private ImageView ivFavorite;
    private FrameLayout btnHeaderCart;
    private TextView tvCartBadge;
    private TextView tvPageIndicator;

    private ImageView btnMinus;
    private ImageView btnPlus;
    private TextView tvQuantity;

    private FrameLayout thumb1, thumb2, thumb3, thumb4, thumb5;
    private ImageView ivThumb1, ivThumb2, ivThumb3, ivThumb4, ivThumb5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        ivProduct = findViewById(R.id.ivProduct);
        toolbar = findViewById(R.id.toolbar);
        tvProductName = findViewById(R.id.tvProductName);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvProductDescription = findViewById(R.id.tvProductDescription);
        chipGroupSizes = findViewById(R.id.chipGroupSizes);
        chipGroupColors = findViewById(R.id.chipGroupColors);
        tvStock = findViewById(R.id.tvStock);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);

        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        ivFavorite = findViewById(R.id.ivFavorite);
        btnHeaderCart = findViewById(R.id.btnHeaderCart);
        tvCartBadge = findViewById(R.id.tvCartBadge);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        tvQuantity = findViewById(R.id.tvQuantity);

        thumb1 = findViewById(R.id.thumb1);
        thumb2 = findViewById(R.id.thumb2);
        thumb3 = findViewById(R.id.thumb3);
        thumb4 = findViewById(R.id.thumb4);
        thumb5 = findViewById(R.id.thumb5);

        ivThumb1 = findViewById(R.id.ivThumb1);
        ivThumb2 = findViewById(R.id.ivThumb2);
        ivThumb3 = findViewById(R.id.ivThumb3);
        ivThumb4 = findViewById(R.id.ivThumb4);
        ivThumb5 = findViewById(R.id.ivThumb5);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(v -> finish());
            }
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        int userId = SharedPrefManager.getInstance(this).getUserId();
        cartRepository = new CartRepository(getApplication());
        productRepository = new ProductRepository(getApplication());
        favoriteRepository = new FavoriteRepository(getApplication());

        if (btnHeaderCart != null) {
            btnHeaderCart.setOnClickListener(v -> {
                Intent intent = new Intent(ProductDetailActivity.this, CustomerMainActivity.class);
                intent.putExtra(LoginActivity.EXTRA_TARGET_TAB, R.id.nav_cart);
                intent.putExtra("NAVIGATE_TO", "CART");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (tvCartBadge != null) {
            cartRepository.getCartItems(userId).observe(this, cartItems -> {
                if (cartItems != null && !cartItems.isEmpty()) {
                    int totalCount = 0;
                    for (CartItem item : cartItems) {
                        totalCount += item.getQuantity();
                    }
                    tvCartBadge.setText(String.valueOf(totalCount));
                    tvCartBadge.setVisibility(View.VISIBLE);
                } else {
                    tvCartBadge.setVisibility(View.GONE);
                }
            });
        }

        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (quantity > 1) {
                    quantity--;
                    if (tvQuantity != null) tvQuantity.setText(String.valueOf(quantity));
                }
            });
        }

        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                if (currentProduct != null && quantity < currentProduct.getStock()) {
                    quantity++;
                    if (tvQuantity != null) tvQuantity.setText(String.valueOf(quantity));
                } else if (currentProduct != null) {
                    Snackbar.make(findViewById(android.R.id.content), "Đã đạt số lượng tồn kho tối đa", Snackbar.LENGTH_SHORT).show();
                }
            });
        }

        int productId = getIntent().getIntExtra("PRODUCT_ID", -1);
        if (productId > 0) {
            if (SharedPrefManager.getInstance(this).isLoggedIn() && userId > 0) {
                favoriteRepository.isFavorite(userId, productId).observe(this, count -> {
                    isFavorite = (count != null && count > 0);
                    if (ivFavorite != null) {
                        ivFavorite.setImageResource(isFavorite ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
                        ivFavorite.setColorFilter(isFavorite ? 0xFFEF4444 : 0xFF0F172A);
                    }
                });
            } else {
                isFavorite = false;
                if (ivFavorite != null) {
                    ivFavorite.setImageResource(R.drawable.ic_heart_outline);
                    ivFavorite.setColorFilter(0xFF0F172A);
                }
            }

            if (btnFavorite != null) {
                btnFavorite.setOnClickListener(v -> {
                    if (!SharedPrefManager.getInstance(this).isLoggedIn()) {
                        Toast.makeText(this, R.string.msg_please_login, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    favoriteRepository.toggleFavorite(userId, productId);
                    Snackbar.make(findViewById(android.R.id.content),
                            !isFavorite ? "Đã thêm vào yêu thích!" : "Đã xóa khỏi yêu thích!",
                            Snackbar.LENGTH_SHORT).show();
                });
            }
        }

        productRepository.getProductById(productId).observe(this, product -> {
            if (product != null) {
                currentProduct = product;
                displayProductDetails(product);
            }
        });

        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                if (!SharedPrefManager.getInstance(this).isLoggedIn()) {
                    Toast.makeText(this, R.string.msg_please_login, Toast.LENGTH_SHORT).show();
                    return;
                }
                addToCart(null);
            });
        }

        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                if (!SharedPrefManager.getInstance(this).isLoggedIn()) {
                    Toast.makeText(this, R.string.msg_please_login, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedSize == null || selectedSize.trim().isEmpty()) {
                    Toast.makeText(ProductDetailActivity.this, "Vui lòng chọn size", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedColor == null || selectedColor.trim().isEmpty()) {
                    Toast.makeText(ProductDetailActivity.this, "Vui lòng chọn màu", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentProduct == null || currentProduct.getStock() <= 0) {
                    Toast.makeText(ProductDetailActivity.this, "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(ProductDetailActivity.this, CheckoutActivity.class);
                intent.putExtra(CheckoutActivity.EXTRA_MODE, CheckoutActivity.MODE_BUY_NOW);
                intent.putExtra(CheckoutActivity.EXTRA_PRODUCT_ID, currentProduct.getId());
                intent.putExtra(CheckoutActivity.EXTRA_SIZE, selectedSize);
                intent.putExtra(CheckoutActivity.EXTRA_COLOR, selectedColor);
                intent.putExtra(CheckoutActivity.EXTRA_QUANTITY, quantity);
                intent.putExtra(CheckoutActivity.EXTRA_PRICE, currentProduct.getPrice());
                startActivity(intent);
            });
        }
    }

    private void displayProductDetails(Product product) {
        if (tvProductName != null) tvProductName.setText(product.getName());

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale.Builder().setLanguage("vi").setRegion("VN").build());
        if (tvProductPrice != null) tvProductPrice.setText(formatter.format(product.getPrice()));
        if (tvProductDescription != null) tvProductDescription.setText(product.getDescription());
        if (tvStock != null) tvStock.setText("Tồn kho: " + product.getStock());

        String imageUrl = product.getImage();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80";
        }

        if (ivProduct != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_sneaker_placeholder)
                    .error(R.drawable.ic_sneaker_placeholder)
                    .fitCenter()
                    .into(ivProduct);
        }

        setupThumbnails(imageUrl);
        setupSizeChips(product);
        setupColorChips(product);
    }

    private void setupThumbnails(String mainImageUrl) {
        ImageView[] thumbImageViews = new ImageView[]{ivThumb1, ivThumb2, ivThumb3, ivThumb4, ivThumb5};
        FrameLayout[] thumbFrames = new FrameLayout[]{thumb1, thumb2, thumb3, thumb4, thumb5};

        for (int i = 0; i < thumbImageViews.length; i++) {
            if (thumbImageViews[i] != null) {
                Glide.with(this)
                        .load(mainImageUrl)
                        .placeholder(R.drawable.ic_sneaker_placeholder)
                        .fitCenter()
                        .into(thumbImageViews[i]);
            }
        }

        for (int i = 0; i < thumbFrames.length; i++) {
            final int index = i;
            if (thumbFrames[i] != null) {
                thumbFrames[i].setOnClickListener(v -> {
                    for (int j = 0; j < thumbFrames.length; j++) {
                        if (thumbFrames[j] != null) {
                            thumbFrames[j].setBackgroundResource(j == index ? R.drawable.bg_thumbnail_selected : R.drawable.bg_thumbnail_unselected);
                        }
                    }
                    if (tvPageIndicator != null) {
                        tvPageIndicator.setText((index + 1) + "/5");
                    }
                });
            }
        }
    }

    private void setupSizeChips(Product product) {
        if (chipGroupSizes == null) return;
        chipGroupSizes.removeAllViews();

        if (product.getSizes() != null && !product.getSizes().isEmpty()) {
            String[] sizes = product.getSizes().split(",");
            for (int i = 0; i < sizes.length; i++) {
                String sizeStr = sizes[i].trim();
                Chip chip = new Chip(this);
                chip.setId(View.generateViewId());
                chip.setText(sizeStr);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setShapeAppearanceModel(chip.getShapeAppearanceModel().toBuilder().setAllCornerSizes(dpToPx(20)).build());
                chip.setChipBackgroundColor(getColorStateList(R.color.chip_size_bg_color));
                chip.setChipStrokeColor(getColorStateList(R.color.chip_size_stroke_color));
                chip.setChipStrokeWidth(dpToPx(1));
                chip.setTextColor(getColorStateList(R.color.chip_size_text_color));
                chip.setEnsureMinTouchTargetSize(false);
                chipGroupSizes.addView(chip);

                if (i == 0) {
                    chip.setChecked(true);
                    selectedSize = sizeStr;
                }
            }
        }

        chipGroupSizes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds != null && !checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) selectedSize = chip.getText().toString();
            } else {
                selectedSize = "";
            }
        });
    }

    private void setupColorChips(Product product) {
        if (chipGroupColors == null) return;
        chipGroupColors.removeAllViews();

        if (product.getColors() != null && !product.getColors().isEmpty()) {
            String[] colors = product.getColors().split(",");
            for (int i = 0; i < colors.length; i++) {
                String colorStr = colors[i].trim();
                Chip chip = new Chip(this);
                chip.setId(View.generateViewId());
                chip.setText(colorStr);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setShapeAppearanceModel(chip.getShapeAppearanceModel().toBuilder().setAllCornerSizes(dpToPx(20)).build());
                chip.setChipBackgroundColor(getColorStateList(R.color.chip_color_bg_color));
                chip.setChipStrokeColor(getColorStateList(R.color.chip_color_stroke_color));
                chip.setChipStrokeWidth(dpToPx(1));
                chip.setTextColor(getColorStateList(R.color.chip_color_text_color));
                chip.setEnsureMinTouchTargetSize(false);
                chipGroupColors.addView(chip);

                if (i == 0) {
                    chip.setChecked(true);
                    selectedColor = colorStr;
                }
            }
        }

        chipGroupColors.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds != null && !checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) selectedColor = chip.getText().toString();
            } else {
                selectedColor = "";
            }
        });
    }

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void addToCart(Runnable onSuccessRunnable) {
        if (selectedSize == null || selectedSize.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn size", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedColor == null || selectedColor.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn màu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentProduct == null || currentProduct.getStock() <= 0) {
            Toast.makeText(this, "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = SharedPrefManager.getInstance(this).getUserId();
        CartItem cartItem = new CartItem(userId, currentProduct.getId(), selectedSize, selectedColor, quantity);
        cartRepository.addToCart(cartItem, currentProduct.getStock(), new CartRepository.OnCartActionCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content), "Đã thêm vào giỏ hàng!", Snackbar.LENGTH_SHORT).show();
                    if (onSuccessRunnable != null) {
                        onSuccessRunnable.run();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show());
            }
        });
    }
}
