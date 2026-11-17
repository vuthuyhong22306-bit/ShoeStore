package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shoestore.databinding.ItemStockBinding;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {
    private List<Product> products = new ArrayList<>();
    private ProductRepository productRepository;

    public StockAdapter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void setProducts(List<Product> products) {
        this.products = products != null ? products : new ArrayList<>();
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
            binding.tvStock.setText("Tồn kho: " + product.getStock() + " đôi");
            String imageUrl = product.getImage();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80";
            }

            Glide.with(binding.ivProduct.getContext())
                    .load(imageUrl)
                    .placeholder(com.example.shoestore.R.drawable.ic_sneaker_placeholder)
                    .error(com.example.shoestore.R.drawable.ic_sneaker_placeholder)
                    .centerCrop()
                    .into(binding.ivProduct);

            binding.btnUpdateStock.setOnClickListener(v -> {
                EditText etStock = new EditText(binding.getRoot().getContext());
                etStock.setText(String.valueOf(product.getStock()));
                etStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

                new AlertDialog.Builder(binding.getRoot().getContext())
                        .setTitle("Cập nhật tồn kho")
                        .setMessage("Sửa số lượng tồn kho cho: " + product.getName())
                        .setView(etStock)
                        .setPositiveButton("Lưu", (dialog, which) -> {
                            String input = etStock.getText().toString().trim();
                            if (!input.isEmpty()) {
                                product.setStock(Integer.parseInt(input));
                                if (productRepository != null) {
                                    productRepository.updateProduct(product);
                                }
                                Toast.makeText(binding.getRoot().getContext(), "Đã cập nhật số lượng tồn kho!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }
    }
}
