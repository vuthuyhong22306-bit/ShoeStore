package com.example.shoestore.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shoestore.databinding.FragmentStaffStockBinding;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.ui.adapter.StockAdapter;

public class StaffStockFragment extends Fragment {
    private FragmentStaffStockBinding binding;
    private ProductRepository productRepository;
    private StockAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStaffStockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() == null) return;

        productRepository = new ProductRepository(getActivity().getApplication());
        adapter = new StockAdapter(productRepository);
        binding.rvStockProducts.setAdapter(adapter);

        productRepository.getAllProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                adapter.setProducts(products);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
