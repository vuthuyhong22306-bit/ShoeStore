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
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.ProductRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageCategoriesActivity extends AppCompatActivity {
    private ActivityManageCategoriesBinding binding;
    private ProductRepository productRepository;
    private CategoryAdapter adapter;
    private List<Category> currentCategories = new ArrayList<>();
    private Map<Integer, Integer> categoryProductCounts = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageCategoriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        productRepository = new ProductRepository(getApplication());
        adapter = new CategoryAdapter();
        binding.rvCategories.setAdapter(adapter);

        productRepository.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                currentCategories = categories;
                adapter.setCategories(categories, categoryProductCounts);
            }
        });

        productRepository.getAllProducts().observe(this, products -> {
            categoryProductCounts.clear();
            if (products != null) {
                for (Product p : products) {
                    int catId = p.getCategoryId();
                    int count = categoryProductCounts.getOrDefault(catId, 0);
                    categoryProductCounts.put(catId, count + 1);
                }
            }
            adapter.setCategories(currentCategories, categoryProductCounts);
        });

        binding.btnAddCategoryInline.setOnClickListener(v -> {
            String name = binding.etCategoryName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên danh mục!", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Category c : currentCategories) {
                if (c.getName() != null && c.getName().equalsIgnoreCase(name)) {
                    Toast.makeText(this, "Danh mục này đã tồn tại!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            productRepository.insertCategory(new Category(name));
            binding.etCategoryName.setText("");
            Toast.makeText(this, "Đã thêm danh mục thành công", Toast.LENGTH_SHORT).show();
        });
    }

    private void showEditCategoryDialog(Category category) {
        EditText etName = new EditText(this);
        etName.setText(category.getName());
        etName.setHint("Tên danh mục");
        etName.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle("Sửa danh mục")
                .setView(etName)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        for (Category c : currentCategories) {
                            if (c.getId() != category.getId() && c.getName() != null && c.getName().equalsIgnoreCase(newName)) {
                                Toast.makeText(ManageCategoriesActivity.this, "Tên danh mục đã tồn tại!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        category.setName(newName);
                        productRepository.updateCategory(category);
                        Toast.makeText(ManageCategoriesActivity.this, "Đã cập nhật danh mục", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        private List<Category> categories = new ArrayList<>();
        private Map<Integer, Integer> productCounts = new HashMap<>();

        public void setCategories(List<Category> categories, Map<Integer, Integer> productCounts) {
            this.categories = categories != null ? categories : new ArrayList<>();
            this.productCounts = productCounts != null ? productCounts : new HashMap<>();
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
                int count = productCounts.getOrDefault(category.getId(), 0);
                binding.tvProductCount.setText(count + " sản phẩm");

                binding.btnEdit.setOnClickListener(v -> showEditCategoryDialog(category));
                binding.btnDelete.setOnClickListener(v -> {
                    if (count > 0) {
                        Toast.makeText(ManageCategoriesActivity.this, "Không thể xóa danh mục đang có " + count + " sản phẩm!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    new AlertDialog.Builder(ManageCategoriesActivity.this)
                            .setTitle("Xóa danh mục")
                            .setMessage("Bạn có chắc chắn muốn xóa danh mục \"" + category.getName() + "\"?")
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