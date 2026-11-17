package com.example.shoestore.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoestore.R;
import com.example.shoestore.databinding.FragmentFilterSortBottomSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FilterSortBottomSheetFragment extends BottomSheetDialogFragment {
    private FragmentFilterSortBottomSheetBinding binding;
    private int selectedSortOrder = 0;
    private OnFilterApplyListener listener;

    public interface OnFilterApplyListener {
        void onApply(int sortOrder);
    }

    public static FilterSortBottomSheetFragment newInstance(int currentSortOrder, OnFilterApplyListener listener) {
        FilterSortBottomSheetFragment fragment = new FilterSortBottomSheetFragment();
        fragment.selectedSortOrder = currentSortOrder;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFilterSortBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (selectedSortOrder == 1) {
            binding.toggleSortGroup.check(R.id.btnSortLowToHigh);
        } else if (selectedSortOrder == 2) {
            binding.toggleSortGroup.check(R.id.btnSortHighToLow);
        } else {
            binding.toggleSortGroup.check(R.id.btnSortDefault);
        }

        binding.btnApplyFilter.setOnClickListener(v -> {
            int checkedId = binding.toggleSortGroup.getCheckedButtonId();
            int order = 0;
            if (checkedId == R.id.btnSortLowToHigh) {
                order = 1;
            } else if (checkedId == R.id.btnSortHighToLow) {
                order = 2;
            }
            if (listener != null) {
                listener.onApply(order);
            }
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
