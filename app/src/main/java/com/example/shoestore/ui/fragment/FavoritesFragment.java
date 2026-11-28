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

import com.example.shoestore.R;
import com.example.shoestore.databinding.FragmentFavoritesBinding;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.FavoriteRepository;
import com.example.shoestore.ui.activity.LoginActivity;
import com.example.shoestore.ui.activity.ProductDetailActivity;
import com.example.shoestore.ui.adapter.ProductAdapter;
import com.example.shoestore.utils.GuestDataManager;
import com.example.shoestore.utils.SharedPrefManager;

import java.util.ArrayList;

public class FavoritesFragment extends Fragment implements ProductAdapter.OnProductClickListener {
    private FragmentFavoritesBinding binding;
    private FavoriteRepository favoriteRepository;
    private ProductAdapter adapter;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean isLoggedIn = SharedPrefManager.getInstance(requireContext()).isLoggedIn();
        if (!isLoggedIn) {
            GuestDataManager.getInstance(requireContext()).savePendingAction("MAIN", "NAV_FAVORITES", -1, "", "", 1, 0);
            Toast.makeText(requireContext(), R.string.msg_please_login, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
            return;
        }

        favoriteRepository = new FavoriteRepository(requireActivity().getApplication());
        userId = SharedPrefManager.getInstance(requireContext()).getUserId();

        adapter = new ProductAdapter(this);
        binding.rvFavorites.setAdapter(adapter);

        adapter.setFavoriteClickListener((product, isFavorite) ->
                favoriteRepository.toggleFavorite(userId, product.getId())
        );

        favoriteRepository.getFavoriteProductIds(userId).observe(getViewLifecycleOwner(), favIds ->
                adapter.setFavoriteProductIds(favIds != null ? favIds : new ArrayList<>())
        );

        favoriteRepository.getFavoriteProducts(userId).observe(getViewLifecycleOwner(), products -> {
            if (products != null && !products.isEmpty()) {
                adapter.setProducts(products);
                binding.rvFavorites.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                adapter.setProducts(new ArrayList<>());
                binding.rvFavorites.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
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
