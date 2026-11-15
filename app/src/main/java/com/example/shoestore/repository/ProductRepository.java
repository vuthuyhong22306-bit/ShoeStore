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
        ensureProductsPopulatedAndSynced();
    }

    private void ensureProductsPopulatedAndSynced() {
        executor.execute(() -> {
            List<Product> products = productDao.getAllProductsList();
            if (products == null || products.isEmpty()) {
                List<Category> categories = categoryDao.getAllCategoriesList();
                long nikeId = 1, adidasId = 2, pumaId = 3, converseId = 4;
                if (categories == null || categories.isEmpty()) {
                    nikeId = categoryDao.insert(new Category("Nike"));
                    adidasId = categoryDao.insert(new Category("Adidas"));
                    pumaId = categoryDao.insert(new Category("Puma"));
                    converseId = categoryDao.insert(new Category("Converse"));
                } else {
                    for (Category c : categories) {
                        if ("Nike".equalsIgnoreCase(c.getName())) nikeId = c.getId();
                        else if ("Adidas".equalsIgnoreCase(c.getName())) adidasId = c.getId();
                        else if ("Puma".equalsIgnoreCase(c.getName())) pumaId = c.getId();
                        else if ("Converse".equalsIgnoreCase(c.getName())) converseId = c.getId();
                    }
                }

                String sizes = "38,39,40,41,42,43";
                String colors = "Đen,Trắng,Xanh,Đỏ";

                insertAndSyncProduct(new Product("Nike Air Force 1", "Giày sneaker huyền thoại của Nike.", 2500000, "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80", (int)nikeId, 50, sizes, colors));
                insertAndSyncProduct(new Product("Nike Air Jordan 1", "Giày bóng rổ biểu tượng.", 4500000, "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=800&q=80", (int)nikeId, 30, sizes, colors));
                insertAndSyncProduct(new Product("Nike Zoom Fly", "Giày chạy bộ chuyên nghiệp.", 3200000, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80", (int)nikeId, 25, sizes, colors));

                insertAndSyncProduct(new Product("Adidas UltraBoost", "Êm ái vượt trội với công nghệ Boost.", 3800000, "https://images.unsplash.com/photo-1582588678413-dbf45f4823e9?auto=format&fit=crop&w=800&q=80", (int)adidasId, 40, sizes, colors));
                insertAndSyncProduct(new Product("Adidas Stan Smith", "Thiết kế tối giản và thanh lịch.", 2200000, "https://images.unsplash.com/photo-1587563899343-9da745295d94?auto=format&fit=crop&w=800&q=80", (int)adidasId, 60, sizes, colors));
                insertAndSyncProduct(new Product("Adidas NMD", "Phong cách hiện đại cho đường phố.", 3000000, "https://images.unsplash.com/photo-1518002171953-a080ee817e1f?auto=format&fit=crop&w=800&q=80", (int)adidasId, 35, sizes, colors));

                insertAndSyncProduct(new Product("Puma RS-X", "Kiểu dáng retro và hầm hố.", 2800000, "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=800&q=80", (int)pumaId, 20, sizes, colors));
                insertAndSyncProduct(new Product("Puma Suede", "Chất liệu da lộn cổ điển.", 1900000, "https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?auto=format&fit=crop&w=800&q=80", (int)pumaId, 45, sizes, colors));
                insertAndSyncProduct(new Product("Puma Cali", "Phong cách năng động từ California.", 2100000, "https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&w=800&q=80", (int)pumaId, 30, sizes, colors));

                insertAndSyncProduct(new Product("Converse Chuck 70", "Mẫu giày cổ điển không bao giờ lỗi mốt.", 1800000, "https://images.unsplash.com/photo-1607522370275-f14206abe5d3?auto=format&fit=crop&w=800&q=80", (int)converseId, 80, sizes, colors));
                insertAndSyncProduct(new Product("Converse Jack Purcell", "Nụ cười đặc trưng ở mũi giày.", 1700000, "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=800&q=80", (int)converseId, 50, sizes, colors));
                insertAndSyncProduct(new Product("Converse One Star", "Phong cách skate đường phố.", 1600000, "https://images.unsplash.com/photo-1560769629-975ec94e6a86?auto=format&fit=crop&w=800&q=80", (int)converseId, 40, sizes, colors));
            } else {
                for (Product p : products) {
                    String img = p.getImage();
                    String realUrl = getRealSneakerImageUrl(p.getName());
                    if (img == null || !img.equals(realUrl)) {
                        p.setImage(realUrl);
                        productDao.update(p);
                        syncProductToFirebase(p);
                    }
                }
            }
        });
    }

    private void insertAndSyncProduct(Product product) {
        long id = productDao.insert(product);
        product.setId((int) id);
        syncProductToFirebase(product);
    }

    private void syncProductToFirebase(Product product) {
        try {
            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("products")
                    .child(String.valueOf(product.getId()))
                    .setValue(product);

            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("products")
                    .document(String.valueOf(product.getId()))
                    .set(product);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRealSneakerImageUrl(String name) {
        if (name == null) return "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80";
        String lower = name.toLowerCase();
        if (lower.contains("jordan")) {
            return "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=800&q=80";
        } else if (lower.contains("air force")) {
            return "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80";
        } else if (lower.contains("zoom")) {
            return "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80";
        } else if (lower.contains("ultraboost") || lower.contains("adidas")) {
            return "https://images.unsplash.com/photo-1582588678413-dbf45f4823e9?auto=format&fit=crop&w=800&q=80";
        } else if (lower.contains("stan smith")) {
            return "https://images.unsplash.com/photo-1587563899343-9da745295d94?auto=format&fit=crop&w=800&q=80";
        } else if (lower.contains("nmd")) {
            return "https://images.unsplash.com/photo-1518002171953-a080ee817e1f?auto=format&fit=crop&w=800&q=80";
        } else if (lower.contains("puma")) {
            return "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=800&q=80";
        } else if (lower.contains("converse")) {
            return "https://images.unsplash.com/photo-1607522370275-f14206abe5d3?auto=format&fit=crop&w=800&q=80";
        }
        return "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80";
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
