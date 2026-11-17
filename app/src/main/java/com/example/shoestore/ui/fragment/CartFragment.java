package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Product;
import com.example.shoestore.ui.activity.CheckoutActivity;
import com.example.shoestore.ui.adapter.CartAdapter;
import com.example.shoestore.viewmodel.CartViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartFragment extends Fragment {
    private CartViewModel viewModel;
    private CartAdapter adapter;
    private double totalPrice = 0;
    private List<CartItem> currentItems = new ArrayList<>();
    private List<Product> currentProducts = new ArrayList<>();

    private RecyclerView rvCart;
    private TextView tvEmptyCart;
    private View layoutCheckout;
    private TextView tvTotalPrice;
    private Button btnCheckout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCart = view.findViewById(R.id.rvCart);
        tvEmptyCart = view.findViewById(R.id.tvEmptyCart);
        layoutCheckout = view.findViewById(R.id.layoutCheckout);
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice);
        btnCheckout = view.findViewById(R.id.btnCheckout);

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        adapter = new CartAdapter(viewModel);
        if (rvCart != null) rvCart.setAdapter(adapter);

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

        if (btnCheckout != null) {
            btnCheckout.setOnClickListener(v -> {
                if (currentItems.isEmpty()) {
                    Snackbar.make(view, "Giỏ hàng của bạn đang trống!", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(getContext(), CheckoutActivity.class);
                intent.putExtra("TOTAL_PRICE", totalPrice);
                startActivity(intent);
            });
        }
    }

    private void updateUI() {
        if (currentItems.isEmpty()) {
            if (tvEmptyCart != null) tvEmptyCart.setVisibility(View.VISIBLE);
            if (layoutCheckout != null) layoutCheckout.setVisibility(View.GONE);
            adapter.setCartItems(new ArrayList<>(), new ArrayList<>());
            totalPrice = 0;
            if (tvTotalPrice != null) tvTotalPrice.setText("0 ₫");
        } else {
            if (tvEmptyCart != null) tvEmptyCart.setVisibility(View.GONE);
            if (layoutCheckout != null) layoutCheckout.setVisibility(View.VISIBLE);
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
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale.Builder().setLanguage("vi").setRegion("VN").build());
        if (tvTotalPrice != null) tvTotalPrice.setText(formatter.format(totalPrice));
    }
}
