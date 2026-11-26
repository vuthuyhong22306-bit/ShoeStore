package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.shoestore.R;
import com.example.shoestore.model.Category;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.FavoriteRepository;
import com.example.shoestore.repository.NotificationRepository;
import com.example.shoestore.ui.activity.NotificationActivity;
import com.example.shoestore.ui.activity.ProductDetailActivity;
import com.example.shoestore.ui.adapter.BannerAdapter;
import com.example.shoestore.ui.adapter.ProductAdapter;
import com.example.shoestore.utils.SharedPrefManager;
import com.example.shoestore.viewmodel.HomeViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements ProductAdapter.OnProductClickListener {
    private HomeViewModel viewModel;
    private FavoriteRepository favoriteRepository;
    private ProductAdapter adapter;
    private List<Product> currentProducts = new ArrayList<>();
    private int currentSortOrder = 0; // 0: Default, 1: Low-High, 2: High-Low
    private int userId;

    private RecyclerView rvProducts;
    private ViewPager2 viewPagerBanner;
    private ChipGroup chipGroupCategories;
    private EditText etSearch;
    private View btnFilterSort;
    private TextView tvSortLabel;
    private View layoutEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvProducts = view.findViewById(R.id.rvProducts);
        viewPagerBanner = view.findViewById(R.id.viewPagerBanner);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        etSearch = view.findViewById(R.id.etSearch);
        btnFilterSort = view.findViewById(R.id.btnFilterSort);
        tvSortLabel = view.findViewById(R.id.tvSortLabel);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        favoriteRepository = new FavoriteRepository(requireActivity().getApplication());
        userId = SharedPrefManager.getInstance(requireContext()).getUserId();

        adapter = new ProductAdapter(this);
        adapter.setFavoriteClickListener((product, isFavorite) -> {
            if (!SharedPrefManager.getInstance(requireContext()).isLoggedIn()) {
                android.widget.Toast.makeText(requireContext(), R.string.msg_please_login, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            favoriteRepository.toggleFavorite(userId, product.getId());
        });

        if (SharedPrefManager.getInstance(requireContext()).isLoggedIn() && userId > 0) {
            favoriteRepository.getFavoriteProductIds(userId).observe(getViewLifecycleOwner(), favIds -> {
                adapter.setFavoriteProductIds(favIds != null ? favIds : new ArrayList<>());
            });
        } else {
            adapter.setFavoriteProductIds(new ArrayList<>());
        }

        if (rvProducts != null) rvProducts.setAdapter(adapter);
        if (viewPagerBanner != null) viewPagerBanner.setAdapter(new BannerAdapter());

        View btnNotif = view.findViewById(R.id.btnNotificationHeader);
        TextView tvNotifBadge = view.findViewById(R.id.tvNotifBadge);

        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), NotificationActivity.class);
                startActivity(intent);
            });
        }

        NotificationRepository notificationRepository = new NotificationRepository(requireActivity().getApplication());
        String currentUserIdStr = String.valueOf(userId);
        String currentRoleStr = SharedPrefManager.getInstance(requireContext()).getUserRole();

        notificationRepository.getUnreadCountForUser(currentUserIdStr, currentRoleStr).observe(getViewLifecycleOwner(), unreadCount -> {
            if (tvNotifBadge != null) {
                if (unreadCount != null && unreadCount > 0) {
                    tvNotifBadge.setText(String.valueOf(unreadCount));
                    tvNotifBadge.setVisibility(View.VISIBLE);
                } else {
                    tvNotifBadge.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories == null || chipGroupCategories == null) return;
            chipGroupCategories.removeAllViews();
            Chip allChip = new Chip(getContext());
            allChip.setText("Tất cả");
            allChip.setCheckable(true);
            allChip.setChecked(true);
            allChip.setOnClickListener(v -> viewModel.setCategory(-1));
            chipGroupCategories.addView(allChip);

            for (Category category : categories) {
                Chip chip = new Chip(getContext());
                chip.setText(category.getName());
                chip.setCheckable(true);
                chip.setOnClickListener(v -> viewModel.setCategory(category.getId()));
                chipGroupCategories.addView(chip);
            }
        });

        viewModel.getFilteredProducts().observe(getViewLifecycleOwner(), products -> {
            currentProducts = products != null ? new ArrayList<>(products) : new ArrayList<>();
            applyFiltersAndSort();
        });

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFiltersAndSort();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnFilterSort != null) {
            btnFilterSort.setOnClickListener(v -> {
                FilterSortBottomSheetFragment bottomSheet = FilterSortBottomSheetFragment.newInstance(currentSortOrder, sortOrder -> {
                    currentSortOrder = sortOrder;
                    updateSortLabelText();
                    applyFiltersAndSort();
                });
                bottomSheet.show(getParentFragmentManager(), "FilterSortBottomSheet");
            });
        }
    }

    private void updateSortLabelText() {
        if (tvSortLabel == null) return;
        if (currentSortOrder == 1) {
            tvSortLabel.setText("Giá: Thấp -> Cao");
        } else if (currentSortOrder == 2) {
            tvSortLabel.setText("Giá: Cao -> Thấp");
        } else {
            tvSortLabel.setText("Mặc định");
        }
    }

    private void applyFiltersAndSort() {
        String query = "";
        if (etSearch != null && etSearch.getText() != null) {
            String raw = etSearch.getText().toString().trim().toLowerCase();
            if (!raw.contains("tìm kiếm") && !raw.contains("tim kiem")) {
                query = raw;
            }
        }
        List<Product> filtered = new ArrayList<>();
        for (Product p : currentProducts) {
            if (query.isEmpty() || p.getName().toLowerCase().contains(query)) {
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
            if (rvProducts != null) rvProducts.setVisibility(View.GONE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            if (rvProducts != null) rvProducts.setVisibility(View.VISIBLE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(getContext(), ProductDetailActivity.class);
        intent.putExtra("PRODUCT_ID", product.getId());
        startActivity(intent);
    }
}
