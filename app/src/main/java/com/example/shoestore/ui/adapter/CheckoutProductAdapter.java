package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shoestore.R;
import com.example.shoestore.databinding.ItemCheckoutProductBinding;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Product;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckoutProductAdapter extends RecyclerView.Adapter<CheckoutProductAdapter.ProductViewHolder> {
    private List<CartItem> cartItems = new ArrayList<>();
    private Map<Integer, Product> productMap;
    private final DecimalFormat df = new DecimalFormat("#,###");

    public void setData(List<CartItem> items, Map<Integer, Product> map) {
        this.cartItems = items != null ? items : new ArrayList<>();
        this.productMap = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCheckoutProductBinding binding = ItemCheckoutProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(cartItems.get(position));
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ItemCheckoutProductBinding binding;

        public ProductViewHolder(ItemCheckoutProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CartItem item) {
            Product p = productMap != null ? productMap.get(item.getProductId()) : null;
            if (p != null) {
                binding.tvProductName.setText(p.getName());
                binding.tvProductPrice.setText(df.format(p.getPrice()).replace(',', '.') + "đ");

                if (p.getImage() != null && !p.getImage().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(p.getImage())
                            .placeholder(R.drawable.ic_sneaker_placeholder)
                            .error(R.drawable.ic_sneaker_placeholder)
                            .into(binding.ivProductImage);
                } else {
                    binding.ivProductImage.setImageResource(R.drawable.ic_sneaker_placeholder);
                }
            } else {
                binding.tvProductName.setText("Sản phẩm #" + item.getProductId());
                binding.tvProductPrice.setText("0đ");
                binding.ivProductImage.setImageResource(R.drawable.ic_sneaker_placeholder);
            }

            String specs = "Size: " + (item.getSize() != null ? item.getSize() : "N/A") +
                    " | Màu: " + (item.getColor() != null ? item.getColor() : "N/A");
            binding.tvProductSpecs.setText(specs);
            binding.tvProductQuantity.setText("x" + item.getQuantity());
        }
    }
}
