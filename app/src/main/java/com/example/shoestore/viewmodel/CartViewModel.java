package com.example.shoestore.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.CartRepository;
import com.example.shoestore.repository.ProductRepository;
import com.example.shoestore.utils.SharedPrefManager;

import java.util.List;

public class CartViewModel extends AndroidViewModel {
    private CartRepository cartRepository;
    private ProductRepository productRepository;
    private int userId;
    private LiveData<List<Product>> allProducts;
    private LiveData<List<CartItem>> cartItems;
    private MutableLiveData<String> actionMessage = new MutableLiveData<>();

    public CartViewModel(@NonNull Application application) {
        super(application);
        cartRepository = new CartRepository(application);
        productRepository = new ProductRepository(application);
        userId = SharedPrefManager.getInstance(application).getUserId();
        allProducts = productRepository.getAllProducts();
        cartItems = cartRepository.getCartItems(userId);
    }

    public LiveData<List<CartItem>> getCartItems() { return cartItems; }
    public LiveData<List<Product>> getAllProducts() { return allProducts; }
    public LiveData<String> getActionMessage() { return actionMessage; }

    public LiveData<Product> getProductById(int productId) {
        return productRepository.getProductById(productId);
    }

    public void updateQuantity(CartItem item, int delta, int productStock) {
        int newQty = item.getQuantity() + delta;
        if (newQty <= 0) return;
        if (newQty > productStock) {
            actionMessage.postValue("Số lượng vượt quá tồn kho (Tồn kho: " + productStock + ")");
            return;
        }
        item.setQuantity(newQty);
        cartRepository.updateCartItem(item, productStock, new CartRepository.OnCartActionCallback() {
            @Override
            public void onSuccess() {}

            @Override
            public void onError(String message) {
                actionMessage.postValue(message);
            }
        });
    }

    public void removeItem(CartItem item) {
        cartRepository.removeCartItem(item);
    }
}
