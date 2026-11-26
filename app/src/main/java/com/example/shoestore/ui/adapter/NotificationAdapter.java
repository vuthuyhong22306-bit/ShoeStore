package com.example.shoestore.ui.adapter;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.model.NotificationItem;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem notification);
        void onNotificationDelete(NotificationItem notification);
    }

    private List<NotificationItem> list = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<NotificationItem> notifications) {
        this.list = notifications != null ? notifications : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final View dotUnread;
        private final TextView tvTitle;
        private final TextView tvTimestamp;
        private final TextView tvContent;
        private final TextView tvActionHint;
        private final View layoutContainer;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            dotUnread = itemView.findViewById(R.id.dotUnread);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvActionHint = itemView.findViewById(R.id.tvActionHint);
            layoutContainer = itemView.findViewById(R.id.layoutContainer);
        }

        public void bind(NotificationItem item) {
            if (tvTitle != null) tvTitle.setText(item.getTitle());
            if (tvContent != null) tvContent.setText(item.getContent());

            if (tvTimestamp != null) {
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                        item.getTimestamp(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                );
                tvTimestamp.setText(timeAgo);
            }

            if (!item.isRead()) {
                if (dotUnread != null) dotUnread.setVisibility(View.VISIBLE);
                if (layoutContainer != null) layoutContainer.setBackgroundColor(Color.parseColor("#FFF5F5"));
            } else {
                if (dotUnread != null) dotUnread.setVisibility(View.GONE);
                if (layoutContainer != null) layoutContainer.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }

            if (tvActionHint != null) {
                if (item.getOrderId() > 0) {
                    tvActionHint.setVisibility(View.VISIBLE);
                } else {
                    tvActionHint.setVisibility(View.GONE);
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });
        }
    }
}
