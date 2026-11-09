package com.example.shoestore.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.shoestore.dao.CategoryDao;
import com.example.shoestore.dao.ProductDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.Category;
import com.example.shoestore.model.Product;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ProductRepository {
    private ProductDao productDao;
    private CategoryDao categoryDao;
    private ExecutorService executor;

    public ProductRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        productDao = db.productDao();
        categoryDao = db.categoryDao();
        executor = AppDatabase.databaseWriteExecutor;
    }

    public LiveData<List<Product>> getAllProducts() {
        return productDao.getAllProducts();
    }

    public LiveData<List<Category>> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    public LiveData<List<Product>> getProductsByCategory(int categoryId) {
        return productDao.getProductsByCategory(categoryId);
    }

    public LiveData<List<Product>> searchProducts(String query) {
        return productDao.searchProducts(query);
    }

    public LiveData<Product> getProductById(int id) {
        return productDao.getProductById(id);
    }

    public void insertProduct(Product product) {
        executor.execute(() -> productDao.insert(product));
    }

    public void updateProduct(Product product) {
        executor.execute(() -> productDao.update(product));
    }

    public void deleteProduct(Product product) {
        executor.execute(() -> productDao.delete(product));
    }

    public void insertCategory(Category category) {
        executor.execute(() -> categoryDao.insert(category));
    }

    public void updateCategory(Category category) {
        executor.execute(() -> categoryDao.update(category));
    }

    public void deleteCategory(Category category, OnDeleteCategoryCallback callback) {
        executor.execute(() -> {
            int count = categoryDao.getProductCountByCategory(category.getId());
            if (count > 0) {
                callback.onError("Không thể xóa danh mục đang có sản phẩm");
            } else {
                categoryDao.delete(category);
                callback.onSuccess();
            }
        });
    }

    public interface OnDeleteCategoryCallback {
        void onSuccess();
        void onError(String message);
    }
}
