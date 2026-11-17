package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;

import java.util.Arrays;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    public static class BannerItem {
        public String tag;
        public String title;
        public String subtitle;
        public int bgColor;

        public BannerItem(String tag, String title, String subtitle, int bgColor) {
            this.tag = tag;
            this.title = title;
            this.subtitle = subtitle;
            this.bgColor = bgColor;
        }
    }

    private List<BannerItem> banners = Arrays.asList(
            new BannerItem("NEW ARRIVAL 2026", "NIKE AIR JORDAN 1", "Giảm 20% cho đơn hàng đầu tiên", 0xFF111111),
            new BannerItem("SUMMER COLLECTION", "ADIDAS ULTRABOOST", "Êm ái bứt phá mọi giới hạn", 0xFFFF4500),
            new BannerItem("HOT TRENDING", "PUMA RS-X RETRO", "Phong cách streetwear cực chất", 0xFF37474F)
    );

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.bind(banners.get(position));
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        private View container;
        private TextView tvTag;
        private TextView tvTitle;
        private TextView tvSubtitle;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.bannerContainer);
            tvTag = itemView.findViewById(R.id.tvBannerTag);
            tvTitle = itemView.findViewById(R.id.tvBannerTitle);
            tvSubtitle = itemView.findViewById(R.id.tvBannerSubtitle);
        }

        public void bind(BannerItem banner) {
            container.setBackgroundColor(banner.bgColor);
            tvTag.setText(banner.tag);
            tvTitle.setText(banner.title);
            tvSubtitle.setText(banner.subtitle);
        }
    }
}
