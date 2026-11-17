package com.example.shoestore.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.databinding.ActivityManageCategoriesBinding;
import com.example.shoestore.databinding.ItemCategoryBinding;
import com.example.shoestore.model.Category;
import com.example.shoestore.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public class ManageCategoriesActivity extends AppCompatActivity {
    private ActivityManageCategoriesBinding binding;
    private ProductRepository productRepository;
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageCategoriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý danh mục");
        }

        productRepository = new ProductRepository(getApplication());
        adapter = new CategoryAdapter();
        binding.rvCategories.setAdapter(adapter);

        productRepository.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                adapter.setCategories(categories);
            }
        });

        binding.fabAddCategory.setOnClickListener(v -> showCategoryDialog(null));
    }

    private void showCategoryDialog(Category category) {
        EditText etName = new EditText(this);
        if (category != null) etName.setText(category.getName());
        etName.setHint("Tên danh mục");

        new AlertDialog.Builder(this)
                .setTitle(category == null ? "Thêm danh mục" : "Sửa danh mục")
                .setView(etName)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        if (category == null) {
                            productRepository.insertCategory(new Category(name));
                            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Đã thêm danh mục", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                        } else {
                            category.setName(name);
                            productRepository.updateCategory(category);
                            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(), "Đã cập nhật danh mục", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        private List<Category> categories = new ArrayList<>();

        public void setCategories(List<Category> categories) {
            this.categories = categories;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCategoryBinding binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new CategoryViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            holder.bind(categories.get(position));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class CategoryViewHolder extends RecyclerView.ViewHolder {
            private ItemCategoryBinding binding;

            public CategoryViewHolder(ItemCategoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Category category) {
                binding.tvCategoryName.setText(category.getName());
                binding.btnEdit.setOnClickListener(v -> showCategoryDialog(category));
                binding.btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(ManageCategoriesActivity.this)
                            .setTitle("Xóa danh mục")
                            .setMessage("Bạn có chắc chắn muốn xóa danh mục này?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                productRepository.deleteCategory(category, new ProductRepository.OnDeleteCategoryCallback() {
                                    @Override
                                    public void onSuccess() {
                                        runOnUiThread(() -> Toast.makeText(ManageCategoriesActivity.this, "Đã xóa danh mục", Toast.LENGTH_SHORT).show());
                                    }

                                    @Override
                                    public void onError(String message) {
                                        runOnUiThread(() -> Toast.makeText(ManageCategoriesActivity.this, message, Toast.LENGTH_LONG).show());
                                    }
                                });
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            }
        }
    }
}
