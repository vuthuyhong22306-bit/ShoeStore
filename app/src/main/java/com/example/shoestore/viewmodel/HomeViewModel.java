package com.example.shoestore.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.shoestore.model.Category;
import com.example.shoestore.model.Product;
import com.example.shoestore.repository.ProductRepository;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    private ProductRepository repository;
    private LiveData<List<Product>> allProducts;
    private LiveData<List<Category>> allCategories;
    private MutableLiveData<Integer> selectedCategoryId = new MutableLiveData<>(-1);
    private MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private MutableLiveData<Integer> sortOrder = new MutableLiveData<>(0);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(application);
        allCategories = repository.getAllCategories();
        
        allProducts = Transformations.switchMap(selectedCategoryId, categoryId -> {
            if (categoryId == -1) {
                return repository.getAllProducts();
            } else {
                return repository.getProductsByCategory(categoryId);
            }
        });
    }

    public LiveData<List<Product>> getFilteredProducts() {
        return allProducts;
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public void setCategory(int categoryId) {
        selectedCategoryId.setValue(categoryId);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }
    
    public void setSortOrder(int order) {
        sortOrder.setValue(order);
    }
}
