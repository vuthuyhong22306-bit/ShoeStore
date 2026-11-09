package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.shoestore.databinding.ItemCartBinding;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Product;
import com.example.shoestore.viewmodel.CartViewModel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private List<CartItem> cartItems = new ArrayList<>();
    private Map<Integer, Product> productMap = new HashMap<>();
    private CartViewModel viewModel;

    public CartAdapter(CartViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void setCartItems(List<CartItem> cartItems, List<Product> products) {
        this.cartItems = cartItems != null ? cartItems : new ArrayList<>();
        this.productMap.clear();
        if (products != null) {
            for (Product p : products) {
                this.productMap.put(p.getId(), p);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        holder.bind(cartItems.get(position));
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        private ItemCartBinding binding;

        public CartViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CartItem item) {
            Product product = productMap.get(item.getProductId());
            if (product != null) {
                binding.tvProductName.setText(product.getName());
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                binding.tvProductPrice.setText(formatter.format(product.getPrice()));
                Glide.with(binding.ivProduct.getContext()).load(product.getImage()).into(binding.ivProduct);
            } else {
                binding.tvProductName.setText("Sản phẩm #" + item.getProductId());
            }

            binding.tvVariant.setText("Size: " + item.getSize() + " | Màu: " + item.getColor());
            binding.tvQuantity.setText(String.valueOf(item.getQuantity()));

            int stock = product != null ? product.getStock() : 99;

            binding.btnMinus.setOnClickListener(v -> viewModel.updateQuantity(item, -1, stock));
            binding.btnPlus.setOnClickListener(v -> viewModel.updateQuantity(item, 1, stock));

            binding.btnRemove.setOnClickListener(v -> {
                new AlertDialog.Builder(binding.getRoot().getContext())
                        .setTitle("Xóa sản phẩm")
                        .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này khỏi giỏ hàng?")
                        .setPositiveButton("Xóa", (dialog, which) -> viewModel.removeItem(item))
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }
    }
}
