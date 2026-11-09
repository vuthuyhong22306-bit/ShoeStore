package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.shoestore.databinding.FragmentHomeBinding;
import com.example.shoestore.model.Category;
import com.example.shoestore.model.Product;
import com.example.shoestore.ui.activity.ProductDetailActivity;
import com.example.shoestore.ui.adapter.BannerAdapter;
import com.example.shoestore.ui.adapter.ProductAdapter;
import com.example.shoestore.viewmodel.HomeViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements ProductAdapter.OnProductClickListener {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private ProductAdapter adapter;
    private List<Product> currentProducts = new ArrayList<>();
    private int currentSortOrder = 0; // 0: Default, 1: Low-High, 2: High-Low

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        adapter = new ProductAdapter(this);
        binding.rvProducts.setAdapter(adapter);

        binding.viewPagerBanner.setAdapter(new BannerAdapter());

        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories == null) return;
            binding.chipGroupCategories.removeAllViews();
            Chip allChip = new Chip(getContext());
            allChip.setText("Tất cả");
            allChip.setCheckable(true);
            allChip.setChecked(true);
            allChip.setOnClickListener(v -> viewModel.setCategory(-1));
            binding.chipGroupCategories.addView(allChip);

            for (Category category : categories) {
                Chip chip = new Chip(getContext());
                chip.setText(category.getName());
                chip.setCheckable(true);
                chip.setOnClickListener(v -> viewModel.setCategory(category.getId()));
                binding.chipGroupCategories.addView(chip);
            }
        });

        viewModel.getFilteredProducts().observe(getViewLifecycleOwner(), products -> {
            currentProducts = products != null ? new ArrayList<>(products) : new ArrayList<>();
            applyFiltersAndSort();
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndSort();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnFilterSort.setOnClickListener(v -> {
            FilterSortBottomSheetFragment bottomSheet = FilterSortBottomSheetFragment.newInstance(currentSortOrder, sortOrder -> {
                currentSortOrder = sortOrder;
                updateSortLabelText();
                applyFiltersAndSort();
            });
            bottomSheet.show(getParentFragmentManager(), "FilterSortBottomSheet");
        });
    }

    private void updateSortLabelText() {
        if (currentSortOrder == 1) {
            binding.tvSortLabel.setText("Giá: Thấp -> Cao");
        } else if (currentSortOrder == 2) {
            binding.tvSortLabel.setText("Giá: Cao -> Thấp");
        } else {
            binding.tvSortLabel.setText("Mặc định");
        }
    }

    private void applyFiltersAndSort() {
        String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().toLowerCase().trim() : "";
        List<Product> filtered = new ArrayList<>();
        for (Product p : currentProducts) {
            if (p.getName().toLowerCase().contains(query)) {
                filtered.add(p);
            }
        }

        if (currentSortOrder == 1) {
            Collections.sort(filtered, (o1, o2) -> Double.compare(o1.getPrice(), o2.getPrice()));
        } else if (currentSortOrder == 2) {
            Collections.sort(filtered, (o1, o2) -> Double.compare(o2.getPrice(), o1.getPrice()));
        }

        adapter.setProducts(filtered);

        if (filtered.isEmpty()) {
            binding.rvProducts.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.rvProducts.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(getContext(), ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getId());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
