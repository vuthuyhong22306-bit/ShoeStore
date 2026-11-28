package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityCheckoutBinding;
import com.example.shoestore.model.Address;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Coupon;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.Product;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.CartRepository;
import com.example.shoestore.repository.CouponRepository;
import com.example.shoestore.repository.OrderRepository;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.ui.adapter.CheckoutProductAdapter;
import com.example.shoestore.ui.adapter.CouponAdapter;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {
    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String MODE_BUY_NOW = "BUY_NOW";
    public static final String MODE_CART = "CART";

    public static final String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";
    public static final String EXTRA_SIZE = "EXTRA_SIZE";
    public static final String EXTRA_COLOR = "EXTRA_COLOR";
    public static final String EXTRA_QUANTITY = "EXTRA_QUANTITY";
    public static final String EXTRA_PRICE = "EXTRA_PRICE";
    public static final String EXTRA_CART_ITEM_IDS = "EXTRA_CART_ITEM_IDS";

    private static final double SHIPPING_STANDARD = 15000.0;
    private static final double SHIPPING_EXPRESS = 35000.0;
    private static final double FREE_SHIPPING_MIN_ORDER = 500000.0;

    private ActivityCheckoutBinding binding;
    private OrderRepository orderRepository;
    private CartRepository cartRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private CouponRepository couponRepository;

    private CheckoutProductAdapter productAdapter;
    private final DecimalFormat df = new DecimalFormat("#,###");

    private User currentUser;
    private Address currentAddress = null;
    private Address userSelectedAddress = null;
    private List<Address> userAddressList = new ArrayList<>();
    private boolean isAddressLoading = true;
    private ValueEventListener firebaseAddressListener = null;
    private String userIdStr = "";

    private String currentReceiverName = "";
    private String currentPhoneNumber = "";
    private String currentFullAddress = "";

    private String mode = MODE_CART;
    private boolean isBuyNowMode = false;

    private List<CartItem> cartItemsList = new ArrayList<>();
    private List<Product> allProductsList = new ArrayList<>();
    private Map<Integer, Product> productMap = new HashMap<>();

    private double subtotalAmount = 0.0;
    private double shippingFee = SHIPPING_STANDARD;
    private double discountAmount = 0.0;
    private double finalTotalAmount = 0.0;

    private Coupon appliedCoupon = null;
    private List<Coupon> availableCoupons = new ArrayList<>();

    private final ActivityResultLauncher<Intent> addressSelectionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Address selected = (Address) result.getData().getSerializableExtra("EXTRA_SELECTED_ADDRESS");
                    if (selected != null) {
                        userSelectedAddress = selected;
                        bindAddressData(selected);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null && getIntent().hasExtra(EXTRA_MODE)) {
            mode = getIntent().getStringExtra(EXTRA_MODE);
        }
        isBuyNowMode = MODE_BUY_NOW.equalsIgnoreCase(mode);

        binding.btnBack.setOnClickListener(v -> finish());

        orderRepository = new OrderRepository(getApplication());
        cartRepository = new CartRepository(getApplication());
        productRepository = new ProductRepository(getApplication());
        userRepository = new UserRepository(getApplication());
        couponRepository = new CouponRepository();

        setupRecyclerView();
        setupListeners();
        loadAddressData();
        loadCartAndProducts();
        loadAvailableCoupons();
    }

    private void setupRecyclerView() {
        productAdapter = new CheckoutProductAdapter();
        binding.rvOrderProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderProducts.setAdapter(productAdapter);
    }

    private void setupListeners() {
        // Address Card click -> open Address List in select mode or Add Address screen if no addresses exist
        binding.cardAddress.setOnClickListener(v -> {
            if (isAddressLoading) return;

            if (userAddressList.isEmpty() && currentAddress == null) {
                Intent intent = new Intent(CheckoutActivity.this, AddEditAddressActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(CheckoutActivity.this, AddressListActivity.class);
                intent.putExtra("EXTRA_SELECT_MODE", true);
                if (currentAddress != null) {
                    intent.putExtra("EXTRA_SELECTED_ADDRESS_ID", currentAddress.getId());
                }
                addressSelectionLauncher.launch(intent);
            }
        });

        // Shipping method radio change
        binding.rgShippingMethod.setOnCheckedChangeListener((group, checkedId) -> recalculateOrderSummary());

        // Coupon section click -> open BottomSheet
        binding.cardCouponSelect.setOnClickListener(v -> openCouponBottomSheet());

        // Remove applied coupon button
        binding.btnRemoveCoupon.setOnClickListener(v -> removeAppliedCoupon());

        // Place Order Button click
        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void loadAddressData() {
        int userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId <= 0) {
            bindAddressData((Address) null);
            return;
        }
        userIdStr = String.valueOf(userId);

        showAddressLoadingState();

        userRepository.listenUserFromFirebase(userId, new UserRepository.OnUserResultCallback() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    currentUser = user;
                }
            }

            @Override
            public void onError(String message) {}
        });

        if (firebaseAddressListener != null) return;

        firebaseAddressListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Address> list = new ArrayList<>();
                Address defaultAddr = null;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Address address = null;
                    try {
                        address = child.getValue(Address.class);
                    } catch (Exception ignored) {}

                    if (address == null) {
                        String name = child.hasChild("recipientName") ? String.valueOf(child.child("recipientName").getValue()) : "";
                        String phone = child.hasChild("phoneNumber") ? String.valueOf(child.child("phoneNumber").getValue()) : "";
                        String full = child.hasChild("fullAddress") ? String.valueOf(child.child("fullAddress").getValue()) : "";
                        String detail = child.hasChild("detailAddress") ? String.valueOf(child.child("detailAddress").getValue()) : "";
                        String label = child.hasChild("label") ? String.valueOf(child.child("label").getValue()) : "";
                        boolean isDef = false;
                        if (child.hasChild("isDefault")) {
                            Object val = child.child("isDefault").getValue();
                            if (val instanceof Boolean) isDef = (Boolean) val;
                            else if (val != null) isDef = "true".equalsIgnoreCase(String.valueOf(val));
                        } else if (child.hasChild("default")) {
                            Object val = child.child("default").getValue();
                            if (val instanceof Boolean) isDef = (Boolean) val;
                            else if (val != null) isDef = "true".equalsIgnoreCase(String.valueOf(val));
                        }
                        address = new Address(child.getKey(), userIdStr, name, phone, full, detail, label, isDef);
                    }

                    if (address != null) {
                        if (address.getId() == null || address.getId().trim().isEmpty()) {
                            address.setId(child.getKey());
                        }
                        list.add(address);
                        if (address.isDefault()) {
                            defaultAddr = address;
                        }
                    }
                }

                userAddressList = list;

                final Address finalDefault = defaultAddr;
                runOnUiThread(() -> {
                    // 1. If user explicitly selected an address in this session, match it from list
                    if (userSelectedAddress != null) {
                        Address match = null;
                        for (Address a : list) {
                            if (a.getId().equals(userSelectedAddress.getId())) {
                                match = a;
                                break;
                            }
                        }
                        if (match != null) {
                            userSelectedAddress = match;
                            bindAddressData(userSelectedAddress);
                            return;
                        }
                    }

                    // 2. Requirement 2: Get address with isDefault = true
                    if (finalDefault != null) {
                        userSelectedAddress = null;
                        bindAddressData(finalDefault);
                        return;
                    }

                    // 3. Requirement 3: If no default address but has other addresses, use first address
                    if (!list.isEmpty()) {
                        userSelectedAddress = null;
                        bindAddressData(list.get(0));
                        return;
                    }

                    // 4. Fallback check: user profile address if available
                    if (currentUser != null && currentUser.getAddress() != null
                            && !currentUser.getAddress().trim().isEmpty()
                            && !currentUser.getAddress().equalsIgnoreCase("Việt Nam")) {
                        Address fallbackAddr = new Address(
                                "profile_default",
                                userIdStr,
                                currentUser.getFullName() != null ? currentUser.getFullName() : "",
                                currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "",
                                currentUser.getAddress(),
                                "",
                                "Mặc định",
                                true
                        );
                        bindAddressData(fallbackAddr);
                        return;
                    }

                    // 5. Requirement 3: No address available
                    bindAddressData((Address) null);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isAddressLoading = false;
                runOnUiThread(() -> {
                    if (currentAddress == null) {
                        bindAddressData((Address) null);
                    }
                });
            }
        };

        userRepository.listenAddressesFromFirebase(userIdStr, firebaseAddressListener);
    }

    private void showAddressLoadingState() {
        isAddressLoading = true;
        binding.tvReceiverNamePhone.setText("Đang tải địa chỉ giao hàng...");
        binding.tvReceiverNamePhone.setVisibility(View.VISIBLE);
        binding.tvFullAddress.setVisibility(View.GONE);
        binding.tvMissingAddressWarning.setVisibility(View.GONE);
        if (binding.pbAddressLoading != null) {
            binding.pbAddressLoading.setVisibility(View.VISIBLE);
        }
        binding.btnPlaceOrder.setEnabled(false);
        binding.btnPlaceOrder.setAlpha(0.5f);
    }

    private void bindAddressData(Address address) {
        isAddressLoading = false;
        if (binding.pbAddressLoading != null) {
            binding.pbAddressLoading.setVisibility(View.GONE);
        }

        currentAddress = address;

        if (address != null) {
            currentReceiverName = address.getRecipientName() != null ? address.getRecipientName() : "";
            currentPhoneNumber = address.getPhoneNumber() != null ? address.getPhoneNumber() : "";

            String full = address.getFullAddress() != null ? address.getFullAddress() : "";
            String detail = address.getDetailAddress() != null ? address.getDetailAddress() : "";
            if (!detail.trim().isEmpty()) {
                currentFullAddress = detail + ", " + full;
            } else {
                currentFullAddress = full;
            }

            String namePhone = currentReceiverName;
            if (!currentPhoneNumber.trim().isEmpty()) {
                namePhone += " (" + currentPhoneNumber + ")";
            }

            binding.tvReceiverNamePhone.setText(namePhone);
            binding.tvReceiverNamePhone.setVisibility(View.VISIBLE);
            binding.tvFullAddress.setText(currentFullAddress);
            binding.tvFullAddress.setVisibility(View.VISIBLE);
            binding.tvMissingAddressWarning.setVisibility(View.GONE);

            binding.btnPlaceOrder.setEnabled(true);
            binding.btnPlaceOrder.setAlpha(1.0f);
        } else {
            currentReceiverName = "";
            currentPhoneNumber = "";
            currentFullAddress = "";

            binding.tvReceiverNamePhone.setVisibility(View.GONE);
            binding.tvFullAddress.setVisibility(View.GONE);
            binding.tvMissingAddressWarning.setText("⚠️ Vui lòng thêm địa chỉ giao hàng");
            binding.tvMissingAddressWarning.setVisibility(View.VISIBLE);

            binding.btnPlaceOrder.setEnabled(false);
            binding.btnPlaceOrder.setAlpha(0.5f);
        }
    }

    private void loadCartAndProducts() {
        int userId = SharedPrefManager.getInstance(this).getUserId();

        productRepository.getAllProducts().observe(this, products -> {
            if (products != null) {
                allProductsList = products;
                productMap.clear();
                for (Product p : products) {
                    productMap.put(p.getId(), p);
                }
                updateProductsList();
            }
        });

        if (isBuyNowMode) {
            int productId = getIntent().getIntExtra(EXTRA_PRODUCT_ID, -1);
            String size = getIntent().getStringExtra(EXTRA_SIZE);
            String color = getIntent().getStringExtra(EXTRA_COLOR);
            int quantity = getIntent().getIntExtra(EXTRA_QUANTITY, 1);

            if (productId <= 0) {
                com.example.shoestore.utils.GuestDataManager guestPref = com.example.shoestore.utils.GuestDataManager.getInstance(this);
                productId = guestPref.getPendingProductId();
                if (size == null || size.isEmpty()) size = guestPref.getPendingSize();
                if (color == null || color.isEmpty()) color = guestPref.getPendingColor();
                if (quantity <= 0) quantity = guestPref.getPendingQuantity();
            }

            CartItem buyNowItem = new CartItem(userId, productId, size, color, quantity);
            cartItemsList = new ArrayList<>();
            cartItemsList.add(buyNowItem);
            updateProductsList();
        } else {
            ArrayList<Integer> selectedCartItemIds = getIntent().getIntegerArrayListExtra(EXTRA_CART_ITEM_IDS);

            cartRepository.getCartItems(userId).observe(this, items -> {
                if (items != null) {
                    if (selectedCartItemIds != null && !selectedCartItemIds.isEmpty()) {
                        List<CartItem> filtered = new ArrayList<>();
                        for (CartItem item : items) {
                            if (selectedCartItemIds.contains(item.getId())) {
                                filtered.add(item);
                            }
                        }
                        cartItemsList = !filtered.isEmpty() ? filtered : items;
                    } else {
                        cartItemsList = items;
                    }
                    updateProductsList();
                }
            });
        }
    }

    private void updateProductsList() {
        if (cartItemsList != null && productMap != null) {
            productAdapter.setData(cartItemsList, productMap);
            productAdapter.notifyDataSetChanged();
            recalculateOrderSummary();
        }
    }

    private void loadAvailableCoupons() {
        couponRepository.listenCoupons(new CouponRepository.OnCouponsLoadedListener() {
            @Override
            public void onLoaded(List<Coupon> coupons) {
                availableCoupons = coupons != null ? coupons : new ArrayList<>();
                if (availableCoupons.isEmpty()) {
                    availableCoupons.add(new Coupon("SHOE10", 10, 100000, "Giảm 10% cho đơn từ 100k", "31/12/2026", true));
                    availableCoupons.add(new Coupon("SHOE20", 20, 200000, "Giảm 20% cho đơn từ 200k", "31/12/2026", true));
                    availableCoupons.add(new Coupon("VIP50", 50, 1000000, "Giảm 50% cho đơn từ 1 triệu", "31/12/2026", true));
                }
            }

            @Override
            public void onError(String message) {
                availableCoupons = new ArrayList<>();
                availableCoupons.add(new Coupon("SHOE10", 10, 100000, "Giảm 10% cho đơn từ 100k", "31/12/2026", true));
                availableCoupons.add(new Coupon("SHOE20", 20, 200000, "Giảm 20% cho đơn từ 200k", "31/12/2026", true));
            }
        });
    }

    private void recalculateOrderSummary() {
        subtotalAmount = 0.0;
        if (cartItemsList != null && productMap != null) {
            for (CartItem item : cartItemsList) {
                Product p = productMap.get(item.getProductId());
                if (p != null) {
                    subtotalAmount += p.getPrice() * item.getQuantity();
                }
            }
        }

        if (subtotalAmount >= FREE_SHIPPING_MIN_ORDER) {
            shippingFee = 0.0;
            binding.tvFreeshipNotice.setText("🎉 Đơn hàng từ " + df.format(FREE_SHIPPING_MIN_ORDER).replace(',', '.') + "đ -> Bạn được Miễn phí vận chuyển!");
            binding.tvFreeshipNotice.setVisibility(View.VISIBLE);
        } else {
            shippingFee = binding.rbShippingStandard.isChecked() ? SHIPPING_STANDARD : SHIPPING_EXPRESS;
            double remainingForFreeship = FREE_SHIPPING_MIN_ORDER - subtotalAmount;
            binding.tvFreeshipNotice.setText("Mua thêm " + df.format(remainingForFreeship).replace(',', '.') + "đ để được Miễn phí vận chuyển!");
            binding.tvFreeshipNotice.setVisibility(View.VISIBLE);
        }

        if (appliedCoupon != null) {
            String checkError = validateCoupon(appliedCoupon);
            if (checkError != null) {
                removeAppliedCoupon();
                Toast.makeText(this, "Mã giảm giá đã bị bỏ: " + checkError, Toast.LENGTH_SHORT).show();
            } else {
                if (appliedCoupon.getDiscountPercent() > 0) {
                    discountAmount = subtotalAmount * (appliedCoupon.getDiscountPercent() / 100.0);
                } else {
                    discountAmount = 0.0;
                }
            }
        } else {
            discountAmount = 0.0;
        }

        finalTotalAmount = Math.max(0.0, subtotalAmount + shippingFee - discountAmount);

        binding.tvSubtotalAmount.setText(df.format(subtotalAmount).replace(',', '.') + "đ");
        binding.tvShippingFeeAmount.setText(shippingFee == 0 ? "Miễn phí (0đ)" : df.format(shippingFee).replace(',', '.') + "đ");
        binding.tvDiscountAmount.setText("-" + df.format(discountAmount).replace(',', '.') + "đ");
        binding.tvSummaryTotalAmount.setText(df.format(finalTotalAmount).replace(',', '.') + "đ");
        binding.tvBottomTotalAmount.setText(df.format(finalTotalAmount).replace(',', '.') + "đ");
    }

    private void openCouponBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_bottom_sheet_coupon, null);
        dialog.setContentView(dialogView);

        ImageView btnClose = dialogView.findViewById(R.id.btnCloseBottomSheet);
        EditText etInput = dialogView.findViewById(R.id.etCouponCodeInput);
        TextView btnApplyInput = dialogView.findViewById(R.id.btnApplyEnteredCode);
        TextView tvError = dialogView.findViewById(R.id.tvCouponErrorMsg);
        RecyclerView rvCoupons = dialogView.findViewById(R.id.rvCoupons);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        CouponAdapter adapter = new CouponAdapter(coupon -> {
            String errorMsg = validateCoupon(coupon);
            if (errorMsg != null) {
                if (tvError != null) {
                    tvError.setText(errorMsg);
                    tvError.setVisibility(View.VISIBLE);
                }
            } else {
                applyCoupon(coupon);
                dialog.dismiss();
            }
        });

        if (rvCoupons != null) {
            rvCoupons.setLayoutManager(new LinearLayoutManager(this));
            rvCoupons.setAdapter(adapter);
            adapter.setCoupons(availableCoupons, appliedCoupon != null ? appliedCoupon.getCode() : "");
        }

        if (btnApplyInput != null && etInput != null) {
            btnApplyInput.setOnClickListener(v -> {
                String code = etInput.getText().toString().trim().toUpperCase(Locale.ROOT);
                if (code.isEmpty()) {
                    if (tvError != null) {
                        tvError.setText("Vui lòng nhập mã giảm giá!");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    return;
                }

                Coupon found = null;
                for (Coupon c : availableCoupons) {
                    if (c.getCode().equalsIgnoreCase(code)) {
                        found = c;
                        break;
                    }
                }

                if (found == null) {
                    if (tvError != null) {
                        tvError.setText("Mã giảm giá \"" + code + "\" không tồn tại!");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    return;
                }

                String errorMsg = validateCoupon(found);
                if (errorMsg != null) {
                    if (tvError != null) {
                        tvError.setText(errorMsg);
                        tvError.setVisibility(View.VISIBLE);
                    }
                } else {
                    applyCoupon(found);
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }

    private String validateCoupon(Coupon coupon) {
        if (coupon == null) return "Mã giảm giá không hợp lệ";
        if (!coupon.isAvailable()) return "Mã giảm giá này hiện không khả dụng";

        if (subtotalAmount < coupon.getMinOrderValue()) {
            return "Đơn hàng phải từ " + df.format(coupon.getMinOrderValue()).replace(',', '.') + "đ để dùng mã này!";
        }

        if (coupon.getExpiryDate() != null && !coupon.getExpiryDate().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date expiry = sdf.parse(coupon.getExpiryDate());
                Date today = sdf.parse(sdf.format(new Date()));
                if (expiry != null && today != null && today.after(expiry)) {
                    return "Mã giảm giá đã hết hạn sử dụng (" + coupon.getExpiryDate() + ")";
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void applyCoupon(Coupon coupon) {
        appliedCoupon = coupon;
        recalculateOrderSummary();

        binding.tvAppliedCouponText.setText(coupon.getCode() + " (-" + df.format(discountAmount).replace(',', '.') + "đ)");
        binding.btnRemoveCoupon.setVisibility(View.VISIBLE);
        binding.ivCouponChevron.setVisibility(View.GONE);

        Toast.makeText(this, "Đã áp dụng mã " + coupon.getCode() + "!", Toast.LENGTH_SHORT).show();
    }

    private void removeAppliedCoupon() {
        appliedCoupon = null;
        discountAmount = 0.0;
        recalculateOrderSummary();

        binding.tvAppliedCouponText.setText("Chọn hoặc nhập mã");
        binding.btnRemoveCoupon.setVisibility(View.GONE);
        binding.ivCouponChevron.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Đã bỏ chọn mã giảm giá", Toast.LENGTH_SHORT).show();
    }

    private void placeOrder() {
        if (currentAddress == null || currentFullAddress.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm địa chỉ giao hàng trước khi đặt hàng!", Toast.LENGTH_SHORT).show();
            if (userAddressList.isEmpty()) {
                Intent intent = new Intent(CheckoutActivity.this, AddEditAddressActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(CheckoutActivity.this, AddressListActivity.class);
                intent.putExtra("EXTRA_SELECT_MODE", true);
                addressSelectionLauncher.launch(intent);
            }
            return;
        }

        if (cartItemsList == null || cartItemsList.isEmpty()) {
            Snackbar.make(binding.getRoot(), "Giỏ hàng của bạn đang trống!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        for (CartItem item : cartItemsList) {
            Product p = productMap.get(item.getProductId());
            if (p == null) {
                Snackbar.make(binding.getRoot(), "Sản phẩm trong giỏ không tồn tại!", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (p.getStock() < item.getQuantity()) {
                Snackbar.make(binding.getRoot(), "Sản phẩm \"" + p.getName() + "\" không đủ số lượng tồn kho (Còn: " + p.getStock() + ")", Snackbar.LENGTH_LONG).show();
                return;
            }
        }

        String paymentMethod = binding.rbCod.isChecked() ? "COD" : "Chuyển khoản";
        String shippingMethod = binding.rbShippingStandard.isChecked() ? "Giao tiêu chuẩn" : "Giao nhanh";
        int userId = SharedPrefManager.getInstance(this).getUserId();

        Order order = new Order(
                userId,
                currentReceiverName,
                currentPhoneNumber,
                currentFullAddress,
                paymentMethod,
                finalTotalAmount,
                System.currentTimeMillis(),
                "Chờ xác nhận"
        );
        order.setShippingMethod(shippingMethod);
        order.setShippingFee(shippingFee);
        order.setCouponCode(appliedCoupon != null ? appliedCoupon.getCode() : "");
        order.setDiscountAmount(discountAmount);
        order.setSubtotal(subtotalAmount);

        List<Double> prices = new ArrayList<>();
        for (CartItem item : cartItemsList) {
            Product p = productMap.get(item.getProductId());
            prices.add(p != null ? p.getPrice() : 0.0);
        }

        binding.layoutLoadingOverlay.setVisibility(View.VISIBLE);

        orderRepository.placeOrder(order, cartItemsList, prices, isBuyNowMode, new OrderRepository.OnOrderCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    binding.layoutLoadingOverlay.setVisibility(View.GONE);
                    new AlertDialog.Builder(CheckoutActivity.this)
                            .setTitle("Đặt hàng thành công! 🎉")
                            .setMessage("Đơn hàng của bạn đã được khởi tạo thành công với trạng thái: Chờ xác nhận.")
                            .setPositiveButton("Xem đơn hàng", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    binding.layoutLoadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(CheckoutActivity.this, "Lỗi khi đặt hàng: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
