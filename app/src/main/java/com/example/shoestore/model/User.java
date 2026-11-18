package com.example.shoestore.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@com.google.firebase.database.IgnoreExtraProperties
@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    private String address;
    private String role; // CUSTOMER, STAFF, ADMIN

    private boolean isLocked;
    private String gender; // Nam / Nữ / Khác
    private String dateOfBirth; // dd/MM/yyyy

    public User() {
    }

    public User(String fullName, String email, String password, String phoneNumber, String address, String role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.role = role;
        this.isLocked = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    @androidx.room.Ignore
    public void setId(long id) { this.id = (int) id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    // Fallback getter/setter for Firebase field "locked"
    @androidx.room.Ignore
    public boolean getLocked() { return isLocked; }
    @androidx.room.Ignore
    public void setLocked(Boolean locked) { if (locked != null) isLocked = locked; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    // Firebase mappings for "gioiTinh" and "ngaySinh"
    @androidx.room.Ignore
    @com.google.firebase.database.PropertyName("gioiTinh")
    public String getGioiTinh() { return gender; }

    @androidx.room.Ignore
    @com.google.firebase.database.PropertyName("gioiTinh")
    public void setGioiTinh(String gioiTinh) { this.gender = gioiTinh; }

    @androidx.room.Ignore
    @com.google.firebase.database.PropertyName("ngaySinh")
    public String getNgaySinh() { return dateOfBirth; }

    @androidx.room.Ignore
    @com.google.firebase.database.PropertyName("ngaySinh")
    public void setNgaySinh(String ngaySinh) { this.dateOfBirth = ngaySinh; }
}
