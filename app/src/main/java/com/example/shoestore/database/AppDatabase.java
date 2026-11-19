package com.example.shoestore.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.shoestore.dao.AddressDao;
import com.example.shoestore.dao.CartItemDao;
import com.example.shoestore.dao.CategoryDao;
import com.example.shoestore.dao.FavoriteDao;
import com.example.shoestore.dao.OrderDao;
import com.example.shoestore.dao.OrderDetailDao;
import com.example.shoestore.dao.ProductDao;
import com.example.shoestore.dao.UserDao;
import com.example.shoestore.model.Address;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Category;
import com.example.shoestore.model.Favorite;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.OrderDetail;
import com.example.shoestore.model.Product;
import com.example.shoestore.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {User.class, Category.class, Product.class, CartItem.class, Order.class, OrderDetail.class, Address.class, Favorite.class}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();
    public abstract CartItemDao cartItemDao();
    public abstract OrderDao orderDao();
    public abstract OrderDetailDao orderDetailDao();
    public abstract AddressDao addressDao();
    public abstract FavoriteDao favoriteDao();

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

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                UserDao userDao = INSTANCE.userDao();
                userDao.insert(new User("Admin", "admin@shoestore.com", "123456", "0123456789", "Hà Nội", "ADMIN"));
                userDao.insert(new User("Nguyễn Văn Staff", "staff@shoestore.com", "123456", "0987654321", "TP. HCM", "STAFF"));
                long customerId = userDao.insert(new User("Nguyễn Văn A", "customer@gmail.com", "123456", "0912345678", "Đà Nẵng", "CUSTOMER"));
                int userId = (int) customerId;

                CategoryDao categoryDao = INSTANCE.categoryDao();
                long nikeId = categoryDao.insert(new Category("Nike"));
                long adidasId = categoryDao.insert(new Category("Adidas"));
                long pumaId = categoryDao.insert(new Category("Puma"));
                long converseId = categoryDao.insert(new Category("Converse"));

                ProductDao productDao = INSTANCE.productDao();
                String sizes = "38,39,40,41,42,43";
                String colors = "Đen,Trắng,Xanh,Đỏ";

                // Products matching prompt screenshot
                long p1Id = productDao.insert(new Product("Sneaker Runner 01", "Giày chạy bộ cao cấp.", 1290000, "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=800&q=80", (int)nikeId, 10, sizes, colors));
                long p2Id = productDao.insert(new Product("Sneaker Street 02", "Phong cách đường phố trẻ trung.", 1590000, "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=800&q=80", (int)nikeId, 2, sizes, colors)); // stock = 2
                long p3Id = productDao.insert(new Product("Sneaker Classic 04", "Thiết kế cổ điển tinh tế.", 990000, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80", (int)adidasId, 15, sizes, colors));
                productDao.insert(new Product("Nike Air Jordan 1", "Giày bóng rổ biểu tượng.", 4500000, "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=800&q=80", (int)nikeId, 50, sizes, colors));
                productDao.insert(new Product("Adidas UltraBoost", "Êm ái vượt trội với công nghệ Boost.", 3800000, "https://images.unsplash.com/photo-1582588678413-dbf45f4823e9?auto=format&fit=crop&w=800&q=80", (int)adidasId, 40, sizes, colors));

                // Cart Items matching Screen 2 in prompt screenshot
                CartItemDao cartItemDao = INSTANCE.cartItemDao();
                cartItemDao.insert(new CartItem(userId, (int)p1Id, "42", "Đen", 1));
                cartItemDao.insert(new CartItem(userId, (int)p2Id, "41", "Trắng", 2));
                cartItemDao.insert(new CartItem(userId, (int)p3Id, "40", "Đỏ", 1));

                // Orders matching Screen 1 in prompt screenshot
                OrderDao orderDao = INSTANCE.orderDao();
                OrderDetailDao orderDetailDao = INSTANCE.orderDetailDao();

                // Order #DH0125 (Chờ xác nhận, 10/11/2026, 2 items, 2.880.000đ)
                Order o1 = new Order(userId, "Nguyễn Văn A", "0912345678", "Đà Nẵng", "COD", 2880000, 1794268800000L, "Chờ xác nhận");
                o1.setId(125);
                orderDao.insert(o1);
                orderDetailDao.insert(new OrderDetail(125, (int)p1Id, "42", "Đen", 1, 1290000));
                orderDetailDao.insert(new OrderDetail(125, (int)p2Id, "41", "Trắng", 1, 1590000));

                // Order #DH0120 (Đang giao, 08/11/2026, 1 item, 1.590.000đ)
                Order o2 = new Order(userId, "Nguyễn Văn A", "0912345678", "Đà Nẵng", "COD", 1590000, 1794096000000L, "Đang giao");
                o2.setId(120);
                orderDao.insert(o2);
                orderDetailDao.insert(new OrderDetail(120, (int)p2Id, "41", "Trắng", 1, 1590000));

                // Order #DH0112 (Yêu cầu trả hàng, Chờ duyệt: Nguyễn Văn A · Sneaker Runner 01 · Size 42 · Lý do: Sai size)
                Order o3 = new Order(userId, "Nguyễn Văn A", "0912345678", "Đà Nẵng", "COD", 1290000, 1793577600000L, "Yêu cầu trả hàng");
                o3.setId(112);
                o3.setNote("Sai size");
                orderDao.insert(o3);
                orderDetailDao.insert(new OrderDetail(112, (int)p1Id, "42", "Đen", 1, 1290000));

                // Order #DH0108 (Đã chấp nhận: Trần Thị B · Sneaker Street 02 · Lý do: Sản phẩm lỗi)
                Order o4 = new Order(userId, "Trần Thị B", "0987654321", "Hà Nội", "COD", 1590000, 1793145600000L, "Đã chấp nhận");
                o4.setId(108);
                o4.setNote("Sản phẩm lỗi");
                orderDao.insert(o4);
                orderDetailDao.insert(new OrderDetail(108, (int)p2Id, "41", "Trắng", 1, 1590000));

                // Order #DH0101 (Đã nhận hàng trả: Lê Văn C · Sneaker Classic 04 · Lý do: Không đúng mô tả)
                Order o5 = new Order(userId, "Lê Văn C", "0933445566", "TP. HCM", "COD", 990000, 1792900000000L, "Đã nhận hàng trả");
                o5.setId(101);
                o5.setNote("Không đúng mô tả");
                orderDao.insert(o5);
                orderDetailDao.insert(new OrderDetail(101, (int)p3Id, "40", "Đỏ", 1, 990000));
            });
        }
    };
}
