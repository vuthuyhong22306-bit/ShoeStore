package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.shoestore.databinding.ItemOrderDetailBinding;
import com.example.shoestore.model.OrderDetail;
import com.example.shoestore.repository.ProductRepository;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.OrderDetailViewHolder> {
    private List<OrderDetail> details = new ArrayList<>();
    private ProductRepository productRepository;
    private LifecycleOwner lifecycleOwner;

    public OrderDetailAdapter(ProductRepository productRepository, LifecycleOwner lifecycleOwner) {
        this.productRepository = productRepository;
        this.lifecycleOwner = lifecycleOwner;
    }

    public void setDetails(List<OrderDetail> details) {
        this.details = details;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderDetailBinding binding = ItemOrderDetailBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new OrderDetailViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderDetailViewHolder holder, int position) {
        holder.bind(details.get(position));
    }

    @Override
    public int getItemCount() {
        return details.size();
    }

    class OrderDetailViewHolder extends RecyclerView.ViewHolder {
        private ItemOrderDetailBinding binding;

        public OrderDetailViewHolder(ItemOrderDetailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OrderDetail detail) {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            
            productRepository.getProductById(detail.getProductId()).observe(lifecycleOwner, product -> {
                if (product != null) {
                    binding.tvProductName.setText(product.getName());
                    Glide.with(binding.ivProduct.getContext()).load(product.getImage()).into(binding.ivProduct);
                }
            });

            binding.tvVariant.setText("Size: " + detail.getSize() + " | Màu: " + detail.getColor());
            binding.tvPriceQuantity.setText(formatter.format(detail.getPrice()) + " x " + detail.getQuantity());
            binding.tvSubtotal.setText(formatter.format(detail.getPrice() * detail.getQuantity()));
        }
    }
}
