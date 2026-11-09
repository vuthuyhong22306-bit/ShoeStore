package com.example.shoestore.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoestore.model.User;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User login(String email, String password);

    @Query("SELECT * FROM users WHERE (email = :account OR phoneNumber = :account) AND password = :password LIMIT 1")
    User loginByAccount(String account, String password);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE email = :email OR phoneNumber = :phone LIMIT 1")
    User getUserByEmailOrPhone(String email, String phone);

    @Query("SELECT * FROM users WHERE id = :id")
    LiveData<User> getUserById(int id);
}
