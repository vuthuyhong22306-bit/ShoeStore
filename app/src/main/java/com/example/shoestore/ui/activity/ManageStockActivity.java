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

import com.bumptech.glide.Glide;
import com.example.shoestore.databinding.ActivityManageStockBinding;
import com.example.shoestore.databinding.ItemStockBinding;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public class ManageStockActivity extends AppCompatActivity {
    private ActivityManageStockBinding binding;
    private ProductRepository productRepository;
    private StockAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageStockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý tồn kho");
        }

        productRepository = new ProductRepository(getApplication());
        adapter = new StockAdapter();
        binding.rvStock.setAdapter(adapter);

        productRepository.getAllProducts().observe(this, products -> {
            if (products != null) {
                adapter.setProducts(products);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {
        private List<Product> products = new ArrayList<>();

        public void setProducts(List<Product> products) {
            this.products = products;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemStockBinding binding = ItemStockBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StockViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
            holder.bind(products.get(position));
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class StockViewHolder extends RecyclerView.ViewHolder {
            private ItemStockBinding binding;

            public StockViewHolder(ItemStockBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Product product) {
                binding.tvProductName.setText(product.getName());
                binding.tvStock.setText("Tồn kho: " + product.getStock());
                Glide.with(binding.ivProduct.getContext()).load(product.getImage()).into(binding.ivProduct);

                binding.btnUpdateStock.setOnClickListener(v -> {
                    EditText etStock = new EditText(ManageStockActivity.this);
                    etStock.setText(String.valueOf(product.getStock()));
                    etStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

                    new AlertDialog.Builder(ManageStockActivity.this)
                            .setTitle("Cập nhật tồn kho")
                            .setView(etStock)
                            .setPositiveButton("Lưu", (dialog, which) -> {
                                String input = etStock.getText().toString();
                                if (!input.isEmpty()) {
                                    product.setStock(Integer.parseInt(input));
                                    productRepository.updateProduct(product);
                                    Toast.makeText(ManageStockActivity.this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });
            }
        }
    }
}
