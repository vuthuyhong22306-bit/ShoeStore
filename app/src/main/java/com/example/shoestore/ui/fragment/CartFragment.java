package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.shoestore.R;
import com.example.shoestore.databinding.FragmentCartBinding;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Product;
import com.example.shoestore.ui.activity.CheckoutActivity;
import com.example.shoestore.ui.activity.LoginActivity;
import com.example.shoestore.ui.adapter.CartAdapter;
import com.example.shoestore.utils.GuestDataManager;
import com.example.shoestore.utils.SharedPrefManager;
import com.example.shoestore.viewmodel.CartViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment {
    private FragmentCartBinding binding;
    private CartViewModel viewModel;
    private CartAdapter adapter;
    private double totalPrice = 0;
    private List<CartItem> currentItems = new ArrayList<>();
    private List<Product> currentProducts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean isLoggedIn = SharedPrefManager.getInstance(requireContext()).isLoggedIn();
        if (!isLoggedIn) {
            GuestDataManager.getInstance(requireContext()).savePendingAction("MAIN", "NAV_CART", -1, "", "", 1, 0);
            Toast.makeText(requireContext(), R.string.msg_please_login, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
            return;
        }

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        adapter = new CartAdapter(viewModel);
        binding.rvCart.setAdapter(adapter);

        viewModel.getCartItems().observe(getViewLifecycleOwner(), items -> {
            currentItems = items != null ? items : new ArrayList<>();
            updateUI();
        });

        viewModel.getAllProducts().observe(getViewLifecycleOwner(), products -> {
            currentProducts = products != null ? products : new ArrayList<>();
            updateUI();
        });

        viewModel.getActionMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.btnCheckout.setOnClickListener(v -> {
            if (currentItems.isEmpty()) {
                Snackbar.make(view, "Giỏ hàng của bạn đang trống!", Snackbar.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(getContext(), CheckoutActivity.class);
            intent.putExtra(CheckoutActivity.EXTRA_MODE, CheckoutActivity.MODE_CART);
            intent.putExtra("TOTAL_PRICE", totalPrice);

            ArrayList<Integer> cartItemIds = new ArrayList<>();
            for (CartItem item : currentItems) {
                cartItemIds.add(item.getId());
            }
            intent.putIntegerArrayListExtra(CheckoutActivity.EXTRA_CART_ITEM_IDS, cartItemIds);

            startActivity(intent);
        });
    }

    private void updateUI() {
        if (currentItems.isEmpty()) {
            binding.tvEmptyCart.setVisibility(View.VISIBLE);
            binding.layoutCheckout.setVisibility(View.GONE);
            binding.tvCartSubtitle.setText("0 sản phẩm");
            adapter.setCartItems(new ArrayList<>(), new ArrayList<>());
            totalPrice = 0;
            binding.tvTotalPrice.setText("0đ");
        } else {
            binding.tvEmptyCart.setVisibility(View.GONE);
            binding.layoutCheckout.setVisibility(View.VISIBLE);

            int totalQty = 0;
            for (CartItem item : currentItems) {
                totalQty += item.getQuantity();
            }
            binding.tvCartSubtitle.setText(totalQty + " sản phẩm");

            adapter.setCartItems(currentItems, currentProducts);
            calculateTotal();
        }
    }

    private void calculateTotal() {
        totalPrice = 0;
        Map<Integer, Product> pMap = new HashMap<>();
        for (Product p : currentProducts) {
            pMap.put(p.getId(), p);
        }
        for (CartItem item : currentItems) {
            Product p = pMap.get(item.getProductId());
            if (p != null) {
                totalPrice += p.getPrice() * item.getQuantity();
            }
        }
        DecimalFormat df = new DecimalFormat("#,###");
        String formattedPrice = df.format(totalPrice).replace(',', '.') + "đ";
        binding.tvTotalPrice.setText(formattedPrice);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
