package com.example.shoestore.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.shoestore.dao.CartItemDao;
import com.example.shoestore.dao.CategoryDao;
import com.example.shoestore.dao.OrderDao;
import com.example.shoestore.dao.OrderDetailDao;
import com.example.shoestore.dao.ProductDao;
import com.example.shoestore.dao.UserDao;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Category;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.OrderDetail;
import com.example.shoestore.model.Product;
import com.example.shoestore.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {User.class, Category.class, Product.class, CartItem.class, Order.class, OrderDetail.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();
    public abstract CartItemDao cartItemDao();
    public abstract OrderDao orderDao();
    public abstract OrderDetailDao orderDetailDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "shoe_store_db")
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                UserDao userDao = INSTANCE.userDao();
                userDao.insert(new User("Admin", "admin@shoestore.com", "123456", "0123456789", "Hà Nội", "ADMIN"));
                userDao.insert(new User("Nguyễn Văn Staff", "staff@shoestore.com", "123456", "0987654321", "TP. HCM", "STAFF"));
                userDao.insert(new User("Nguyễn Văn A", "customer@gmail.com", "123456", "0912345678", "Đà Nẵng", "CUSTOMER"));

                CategoryDao categoryDao = INSTANCE.categoryDao();
                long nikeId = categoryDao.insert(new Category("Nike"));
                long adidasId = categoryDao.insert(new Category("Adidas"));
                long pumaId = categoryDao.insert(new Category("Puma"));
                long converseId = categoryDao.insert(new Category("Converse"));

                ProductDao productDao = INSTANCE.productDao();
                String sizes = "38,39,40,41,42,43";
                String colors = "Đen,Trắng,Xanh,Đỏ";

                // High-resolution Unsplash Sneaker Image URLs for each specific product
                productDao.insert(new Product("Nike Air Force 1", "Giày sneaker huyền thoại của Nike.", 2500000, "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80", (int)nikeId, 50, sizes, colors));
                productDao.insert(new Product("Nike Air Jordan 1", "Giày bóng rổ biểu tượng.", 4500000, "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=800&q=80", (int)nikeId, 4, sizes, colors)); // 4 items -> triggers low stock warning (<=5)
                productDao.insert(new Product("Nike Zoom Fly", "Giày chạy bộ chuyên nghiệp.", 3200000, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80", (int)nikeId, 25, sizes, colors));

                productDao.insert(new Product("Adidas UltraBoost", "Êm ái vượt trội với công nghệ Boost.", 3800000, "https://images.unsplash.com/photo-1582588678413-dbf45f4823e9?auto=format&fit=crop&w=800&q=80", (int)adidasId, 40, sizes, colors));
                productDao.insert(new Product("Adidas Stan Smith", "Thiết kế tối giản và thanh lịch.", 2200000, "https://images.unsplash.com/photo-1587563899343-9da745295d94?auto=format&fit=crop&w=800&q=80", (int)adidasId, 60, sizes, colors));
                productDao.insert(new Product("Adidas NMD", "Phong cách hiện đại cho đường phố.", 3000000, "https://images.unsplash.com/photo-1518002171953-a080ee817e1f?auto=format&fit=crop&w=800&q=80", (int)adidasId, 3, sizes, colors)); // 3 items -> triggers low stock warning (<=5)

                productDao.insert(new Product("Puma RS-X", "Kiểu dáng retro và hầm hố.", 2800000, "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=800&q=80", (int)pumaId, 20, sizes, colors));
                productDao.insert(new Product("Puma Suede", "Chất liệu da lộn cổ điển.", 1900000, "https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?auto=format&fit=crop&w=800&q=80", (int)pumaId, 45, sizes, colors));
                productDao.insert(new Product("Puma Cali", "Phong cách năng động từ California.", 2100000, "https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&w=800&q=80", (int)pumaId, 30, sizes, colors));

                productDao.insert(new Product("Converse Chuck 70", "Mẫu giày cổ điển không bao giờ lỗi mốt.", 1800000, "https://images.unsplash.com/photo-1607522370275-f14206abe5d3?auto=format&fit=crop&w=800&q=80", (int)converseId, 80, sizes, colors));
                productDao.insert(new Product("Converse Jack Purcell", "Nụ cười đặc trưng ở mũi giày.", 1700000, "https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?auto=format&fit=crop&w=800&q=80", (int)converseId, 50, sizes, colors));
                productDao.insert(new Product("Converse One Star", "Phong cách skate đường phố.", 1600000, "https://images.unsplash.com/photo-1560769629-975ec94e6a86?auto=format&fit=crop&w=800&q=80", (int)converseId, 40, sizes, colors));
            });
        }
    };
}
