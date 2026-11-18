package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.shoestore.R;
import com.example.shoestore.databinding.ItemProductBinding;
import com.example.shoestore.model.Product;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> products = new ArrayList<>();
    private List<Integer> favoriteProductIds = new ArrayList<>();
    private OnProductClickListener listener;
    private OnFavoriteClickListener favoriteClickListener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Product product, boolean isFavorite);
    }

    public ProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setFavoriteClickListener(OnFavoriteClickListener favoriteClickListener) {
        this.favoriteClickListener = favoriteClickListener;
    }

    public void setProducts(List<Product> products) {
        this.products = products != null ? products : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setFavoriteProductIds(List<Integer> favoriteProductIds) {
        this.favoriteProductIds = favoriteProductIds != null ? favoriteProductIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private ItemProductBinding binding;

        public ProductViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION && position < products.size()) {
                    listener.onProductClick(products.get(position));
                }
            });

            binding.btnFavorite.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < products.size()) {
                    Product p = products.get(position);
                    boolean isFav = favoriteProductIds.contains(p.getId());
                    if (favoriteClickListener != null) {
                        favoriteClickListener.onFavoriteClick(p, !isFav);
                    }
                }
            });
        }

        public void bind(Product product) {
            binding.tvProductName.setText(product.getName());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvProductPrice.setText(formatter.format(product.getPrice()));

            boolean isFav = favoriteProductIds.contains(product.getId());
            if (isFav) {
                binding.btnFavorite.setImageResource(R.drawable.ic_heart);
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            }

            String imageUrl = product.getImage();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80";
            }

            Glide.with(binding.ivProduct.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_sneaker_placeholder)
                    .error(R.drawable.ic_sneaker_placeholder)
                    .centerCrop()
                    .into(binding.ivProduct);
        }
    }
}
