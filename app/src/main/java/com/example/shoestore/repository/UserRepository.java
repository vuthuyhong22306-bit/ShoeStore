package com.example.shoestore.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.shoestore.dao.UserDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.User;
import java.util.concurrent.ExecutorService;

public class UserRepository {
    private UserDao userDao;
    private ExecutorService executor;

    public UserRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        userDao = db.userDao();
        executor = AppDatabase.databaseWriteExecutor;
    }

    public void insert(User user, OnUserActionCallback callback) {
        executor.execute(() -> {
            long id = userDao.insert(user);
            if (callback != null) {
                if (id > 0) callback.onSuccess();
                else callback.onError("Đăng ký thất bại");
            }
        });
    }

    public void login(String email, String password, OnUserResultCallback callback) {
        executor.execute(() -> {
            User user = userDao.login(email, password);
            if (callback != null) {
                if (user != null) callback.onResult(user);
                else callback.onError("Email hoặc mật khẩu không đúng");
            }
        });
    }

    public void loginCustomer(String account, String password, OnUserResultCallback callback) {
        executor.execute(() -> {
            User user = userDao.loginByAccount(account, password);
            if (callback != null) {
                if (user != null) {
                    callback.onResult(user);
                } else {
                    callback.onError("Tài khoản chưa đăng ký hoặc mật khẩu không chính xác! Vui lòng đăng ký tài khoản.");
                }
            }
        });
    }

    public void registerCustomer(User user, OnUserActionCallback callback) {
        executor.execute(() -> {
            User existing = userDao.getUserByEmailOrPhone(user.getEmail(), user.getPhoneNumber());
            if (existing != null) {
                if (callback != null) callback.onError("Số điện thoại hoặc Email đã được đăng ký!");
            } else {
                long id = userDao.insert(user);
                user.setId((int) id);

                // Save registered customer to Firebase Realtime Database & Firestore
                try {
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(String.valueOf(id))
                            .setValue(user);

                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(String.valueOf(id))
                            .set(user);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (callback != null) {
                    if (id > 0) callback.onSuccess();
                    else callback.onError("Đăng ký thất bại");
                }
            }
        });
    }

    public void getUserByEmail(String email, OnUserResultCallback callback) {
        executor.execute(() -> {
            User user = userDao.getUserByEmail(email);
            if (callback != null) callback.onResult(user);
        });
    }

    public LiveData<User> getUserById(int id) {
        return userDao.getUserById(id);
    }

    public void update(User user) {
        executor.execute(() -> userDao.update(user));
    }

    public interface OnUserActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface OnUserResultCallback {
        void onResult(User user);
        void onError(String message);
    }
}
