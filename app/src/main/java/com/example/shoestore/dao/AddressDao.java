package com.example.shoestore.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoestore.model.Address;

import java.util.List;

@Dao
public interface AddressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Address address);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Address> addresses);

    @Update
    void update(Address address);

    @Delete
    void delete(Address address);

    @Query("SELECT * FROM addresses WHERE userId = :userId")
    LiveData<List<Address>> getAddressesByUserId(String userId);

    @Query("SELECT * FROM addresses WHERE userId = :userId")
    List<Address> getAddressesByUserIdDirect(String userId);

    @Query("DELETE FROM addresses WHERE userId = :userId")
    void deleteAllByUserId(String userId);

    @Query("DELETE FROM addresses WHERE id = :id")
    void deleteById(String id);
}
