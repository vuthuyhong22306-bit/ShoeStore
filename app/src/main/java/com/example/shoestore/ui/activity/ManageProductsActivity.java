package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shoestore.databinding.ActivityManageProductsBinding;
import com.example.shoestore.databinding.ItemManageProductBinding;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.ProductRepository;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageProductsActivity extends AppCompatActivity {
    private ActivityManageProductsBinding binding;
    private ProductRepository productRepository;
    private AdminProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý sản phẩm");
        }

        productRepository = new ProductRepository(getApplication());
        adapter = new AdminProductAdapter();
        binding.rvProducts.setAdapter(adapter);

        productRepository.getAllProducts().observe(this, products -> {
            if (products != null) {
                adapter.setProducts(products);
            }
        });

        binding.fabAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditProductActivity.class));
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.AdminProductViewHolder> {
        private List<Product> products = new ArrayList<>();

        public void setProducts(List<Product> products) {
            this.products = products;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AdminProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemManageProductBinding binding = ItemManageProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AdminProductViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminProductViewHolder holder, int position) {
            holder.bind(products.get(position));
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class AdminProductViewHolder extends RecyclerView.ViewHolder {
            private ItemManageProductBinding binding;

            public AdminProductViewHolder(ItemManageProductBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Product product) {
                binding.tvProductName.setText(product.getName());
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                binding.tvProductPrice.setText(formatter.format(product.getPrice()));
                Glide.with(binding.ivProduct.getContext()).load(product.getImage()).into(binding.ivProduct);

                binding.btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(ManageProductsActivity.this, AddEditProductActivity.class);
                    intent.putExtra("PRODUCT_ID", product.getId());
                    startActivity(intent);
                });

                binding.btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(ManageProductsActivity.this)
                            .setTitle("Xóa sản phẩm")
                            .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                productRepository.deleteProduct(product);
                                Toast.makeText(ManageProductsActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            }
        }
    }
}
