package com.example.shoestore.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.databinding.ItemAddressBinding;
import com.example.shoestore.model.Address;

import java.util.ArrayList;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {
    private List<Address> addressList = new ArrayList<>();
    private OnAddressActionListener listener;
    private boolean isSelectMode = false;
    private String selectedAddressId = "";

    public interface OnAddressActionListener {
        void onEdit(Address address);
        void onDelete(Address address);
        void onSetDefault(Address address);
        void onSelectAddress(Address address);
    }

    public AddressAdapter(OnAddressActionListener listener) {
        this.listener = listener;
    }

    public AddressAdapter(OnAddressActionListener listener, boolean isSelectMode, String selectedAddressId) {
        this.listener = listener;
        this.isSelectMode = isSelectMode;
        this.selectedAddressId = selectedAddressId != null ? selectedAddressId : "";
    }

    public void setSelectMode(boolean isSelectMode, String selectedAddressId) {
        this.isSelectMode = isSelectMode;
        this.selectedAddressId = selectedAddressId != null ? selectedAddressId : "";
        notifyDataSetChanged();
    }

    public void setAddresses(List<Address> list) {
        this.addressList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAddressBinding binding = ItemAddressBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AddressViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        holder.bind(addressList.get(position));
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    class AddressViewHolder extends RecyclerView.ViewHolder {
        private final ItemAddressBinding binding;

        public AddressViewHolder(ItemAddressBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Address address) {
            boolean isSelected = isSelectMode && selectedAddressId.equals(address.getId());

            if (isSelected) {
                binding.tvRecipientName.setText("✓  " + address.getRecipientName());
            } else {
                binding.tvRecipientName.setText(address.getRecipientName());
            }

            String phone = address.getPhoneNumber();
            if (phone != null && phone.length() >= 8) {
                if (!phone.startsWith("(+84)")) {
                    phone = "(+84)" + (phone.startsWith("0") ? phone.substring(1) : phone);
                }
                if (phone.length() >= 12) {
                    phone = phone.substring(0, 7) + "*****" + phone.substring(phone.length() - 2);
                }
            }
            binding.tvPhone.setText(phone != null ? phone : "");

            String full = (address.getDetailAddress() != null && !address.getDetailAddress().isEmpty()
                    ? address.getDetailAddress() + "\n"
                    : "") + (address.getFullAddress() != null ? address.getFullAddress() : "");
            binding.tvFullAddress.setText(full);

            if (isSelectMode) {
                if (isSelected) {
                    binding.tvDefaultBadge.setText("✓ Đã chọn");
                    binding.tvDefaultBadge.setVisibility(View.VISIBLE);
                } else if (address.isDefault()) {
                    binding.tvDefaultBadge.setText("Mặc định");
                    binding.tvDefaultBadge.setVisibility(View.VISIBLE);
                } else {
                    binding.tvDefaultBadge.setVisibility(View.GONE);
                }
                binding.layoutNoticeBanner.setVisibility(View.GONE);
            } else {
                if (address.isDefault()) {
                    binding.tvDefaultBadge.setText("Mặc định");
                    binding.tvDefaultBadge.setVisibility(View.VISIBLE);
                    binding.layoutNoticeBanner.setVisibility(View.VISIBLE);
                } else {
                    binding.tvDefaultBadge.setVisibility(View.GONE);
                    binding.layoutNoticeBanner.setVisibility(View.GONE);
                }
            }

            binding.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(address);
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    if (isSelectMode) {
                        listener.onSelectAddress(address);
                    } else if (!address.isDefault()) {
                        listener.onSetDefault(address);
                    }
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) listener.onDelete(address);
                return true;
            });
        }
    }
}
