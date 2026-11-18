package com.example.shoestore.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.shoestore.dao.AddressDao;
import com.example.shoestore.dao.UserDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.User;
import java.util.concurrent.ExecutorService;

public class UserRepository {
    private final UserDao userDao;
    private final AddressDao addressDao;
    private final ExecutorService executor;

    public UserRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        userDao = db.userDao();
        addressDao = db.addressDao();
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

    public void syncUserFromFirebase(User user, OnUserResultCallback callback) {
        executor.execute(() -> {
            User existing = userDao.getUserByEmail(user.getEmail());
            if (existing == null && user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                existing = userDao.getUserByEmailOrPhone(user.getEmail(), user.getPhoneNumber());
            }
            if (existing != null) {
                user.setId(existing.getId());
                existing.setFullName(user.getFullName());
                existing.setEmail(user.getEmail());
                existing.setPassword(user.getPassword());
                existing.setPhoneNumber(user.getPhoneNumber());
                existing.setAddress(user.getAddress());
                existing.setRole(user.getRole());
                existing.setLocked(user.isLocked());
                userDao.update(existing);
                if (callback != null) callback.onResult(existing);
            } else {
                long id = userDao.insert(user);
                user.setId((int) id);
                if (callback != null) callback.onResult(user);
            }
        });
    }

    public LiveData<User> getUserById(int id) {
        return userDao.getUserById(id);
    }

    public LiveData<java.util.List<User>> getAllUsers() {
        return userDao.getAllUsers();
    }

    public void update(User user) {
        executor.execute(() -> userDao.update(user));
    }

    // ==========================================
    // Firebase Realtime Database Integration
    // ==========================================

    public void updateProfileFirebase(User user, OnUserActionCallback callback) {
        String userIdStr = String.valueOf(user.getId());
        com.google.firebase.database.DatabaseReference userRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(userIdStr);
        userRef.setValue(user).addOnSuccessListener(aVoid -> {
            if (user.getGender() != null) {
                userRef.child("gioiTinh").setValue(user.getGender());
                userRef.child("gender").setValue(user.getGender());
            }
            if (user.getDateOfBirth() != null) {
                userRef.child("ngaySinh").setValue(user.getDateOfBirth());
                userRef.child("dateOfBirth").setValue(user.getDateOfBirth());
            }
            executor.execute(() -> {
                userDao.update(user);
                if (callback != null) callback.onSuccess();
            });
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Lỗi cập nhật trên Firebase: " + e.getMessage());
        });
    }

    public void listenUserFromFirebase(int userId, OnUserResultCallback callback) {
        if (userId <= 0) return;
        com.google.firebase.database.DatabaseReference userRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(String.valueOf(userId));
        userRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = null;
                    try {
                        user = snapshot.getValue(User.class);
                    } catch (Exception ignored) {}

                    if (user == null) {
                        String name = getSnapString(snapshot, "fullName", "name");
                        String email = getSnapString(snapshot, "email");
                        String pwd = getSnapString(snapshot, "password");
                        String phone = getSnapString(snapshot, "phoneNumber", "phone");
                        String addr = getSnapString(snapshot, "address");
                        String role = getSnapString(snapshot, "role");
                        boolean locked = getSnapBoolean(snapshot, "locked", "isLocked");
                        String gender = getSnapString(snapshot, "gender", "gioiTinh");
                        String dob = getSnapString(snapshot, "dateOfBirth", "dob", "ngaySinh");

                        user = new User(
                                name != null ? name : "",
                                email != null ? email : "",
                                pwd != null ? pwd : "",
                                phone != null ? phone : "",
                                addr != null ? addr : "",
                                role != null ? role : "CUSTOMER"
                        );
                        user.setLocked(locked);
                        user.setGender(gender);
                        user.setDateOfBirth(dob);
                    } else {
                        String gender = getSnapString(snapshot, "gender", "gioiTinh");
                        String dob = getSnapString(snapshot, "dateOfBirth", "dob", "ngaySinh");
                        if (gender != null) user.setGender(gender);
                        if (dob != null) user.setDateOfBirth(dob);
                    }

                    user.setId(userId);
                    User finalUser = user;
                    executor.execute(() -> userDao.update(finalUser));
                    if (callback != null) callback.onResult(finalUser);
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                if (callback != null) callback.onError("Lỗi kết nối Firebase: " + error.getMessage());
            }
        });
    }

    private static String getSnapString(com.google.firebase.database.DataSnapshot snap, String... keys) {
        for (String key : keys) {
            if (snap.hasChild(key)) {
                Object val = snap.child(key).getValue();
                if (val != null) return String.valueOf(val);
            }
        }
        return null;
    }

    private static boolean getSnapBoolean(com.google.firebase.database.DataSnapshot snap, String... keys) {
        for (String key : keys) {
            if (snap.hasChild(key)) {
                Object val = snap.child(key).getValue();
                if (val instanceof Boolean) return (Boolean) val;
                if (val instanceof Number) return ((Number) val).intValue() != 0;
                if (val != null) return "true".equalsIgnoreCase(String.valueOf(val));
            }
        }
        return false;
    }

    public void addAddressFirebase(com.example.shoestore.model.Address address, OnUserActionCallback callback) {
        if (address.getUserId() == null || address.getUserId().trim().isEmpty() || "-1".equals(address.getUserId().trim())) {
            if (callback != null) callback.onError("UserId không hợp lệ. Vui lòng đăng nhập lại!");
            return;
        }
        com.google.firebase.database.DatabaseReference addrRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("addresses").child(address.getUserId());
        if (address.getId() == null || address.getId().trim().isEmpty()) {
            String newKey = addrRef.push().getKey();
            if (newKey == null || newKey.isEmpty()) {
                newKey = String.valueOf(System.currentTimeMillis());
            }
            address.setId(newKey);
        }

        if (address.isDefault()) {
            addrRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        child.getRef().child("isDefault").setValue(false);
                        child.getRef().child("default").setValue(false);
                    }
                    addrRef.child(address.getId()).setValue(address).addOnSuccessListener(aVoid -> {
                        String fullAddrStr = address.getFullAddress() + (address.getDetailAddress() != null && !address.getDetailAddress().isEmpty() ? ", " + address.getDetailAddress() : "");
                        updateUserDefaultAddressFirebase(address.getUserId(), fullAddrStr);
                        executor.execute(() -> addressDao.insert(address));
                        if (callback != null) callback.onSuccess();
                    }).addOnFailureListener(e -> {
                        if (callback != null) callback.onError("Lỗi thêm địa chỉ trên Firebase: " + e.getMessage());
                    });
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    if (callback != null) callback.onError("Lỗi kết nối Firebase: " + error.getMessage());
                }
            });
        } else {
            addrRef.child(address.getId()).setValue(address).addOnSuccessListener(aVoid -> {
                executor.execute(() -> addressDao.insert(address));
                if (callback != null) callback.onSuccess();
            }).addOnFailureListener(e -> {
                if (callback != null) callback.onError("Lỗi thêm địa chỉ trên Firebase: " + e.getMessage());
            });
        }
    }

    public void updateAddressFirebase(com.example.shoestore.model.Address address, OnUserActionCallback callback) {
        if (address.getUserId() == null || address.getUserId().trim().isEmpty() || "-1".equals(address.getUserId().trim())) {
            if (callback != null) callback.onError("UserId không hợp lệ. Vui lòng đăng nhập lại!");
            return;
        }
        com.google.firebase.database.DatabaseReference addrRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("addresses").child(address.getUserId());
        if (address.getId() == null || address.getId().trim().isEmpty()) {
            String newKey = addrRef.push().getKey();
            if (newKey == null || newKey.isEmpty()) {
                newKey = String.valueOf(System.currentTimeMillis());
            }
            address.setId(newKey);
        }

        if (address.isDefault()) {
            addrRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        if (!child.getKey().equals(address.getId())) {
                            child.getRef().child("isDefault").setValue(false);
                            child.getRef().child("default").setValue(false);
                        }
                    }
                    addrRef.child(address.getId()).setValue(address).addOnSuccessListener(aVoid -> {
                        String fullAddrStr = address.getFullAddress() + (address.getDetailAddress() != null && !address.getDetailAddress().isEmpty() ? ", " + address.getDetailAddress() : "");
                        updateUserDefaultAddressFirebase(address.getUserId(), fullAddrStr);
                        executor.execute(() -> addressDao.update(address));
                        if (callback != null) callback.onSuccess();
                    }).addOnFailureListener(e -> {
                        if (callback != null) callback.onError("Lỗi cập nhật địa chỉ trên Firebase: " + e.getMessage());
                    });
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    if (callback != null) callback.onError("Lỗi kết nối Firebase: " + error.getMessage());
                }
            });
        } else {
            addrRef.child(address.getId()).setValue(address).addOnSuccessListener(aVoid -> {
                executor.execute(() -> addressDao.update(address));
                if (callback != null) callback.onSuccess();
            }).addOnFailureListener(e -> {
                if (callback != null) callback.onError("Lỗi cập nhật địa chỉ trên Firebase: " + e.getMessage());
            });
        }
    }

    public void deleteAddressFirebase(String userId, String addressId, OnUserActionCallback callback) {
        if (userId == null || addressId == null) return;
        com.google.firebase.database.DatabaseReference addrRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("addresses").child(userId).child(addressId);
        addrRef.removeValue().addOnSuccessListener(aVoid -> {
            executor.execute(() -> addressDao.deleteById(addressId));
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError("Lỗi xóa địa chỉ trên Firebase: " + e.getMessage());
        });
    }

    public void setDefaultAddressFirebase(String userId, String targetAddressId, OnUserActionCallback callback) {
        if (userId == null || targetAddressId == null) return;
        com.google.firebase.database.DatabaseReference addrRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("addresses").child(userId);
        addrRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                String fullAddressStr = "";
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    boolean isTarget = child.getKey().equals(targetAddressId);
                    child.getRef().child("isDefault").setValue(isTarget);
                    child.getRef().child("default").setValue(isTarget);
                    if (isTarget) {
                        String full = getSnapString(child, "fullAddress");
                        String detail = getSnapString(child, "detailAddress");
                        if (full != null) {
                            fullAddressStr = full + (detail != null && !detail.isEmpty() ? ", " + detail : "");
                        }
                    }
                }
                if (!fullAddressStr.isEmpty()) {
                    updateUserDefaultAddressFirebase(userId, fullAddressStr);
                }
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                if (callback != null) callback.onError("Lỗi đặt địa chỉ mặc định trên Firebase: " + error.getMessage());
            }
        });
    }

    private void updateUserDefaultAddressFirebase(String userId, String newAddress) {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(userId).child("address").setValue(newAddress);
        try {
            int uId = Integer.parseInt(userId);
            executor.execute(() -> {
                User user = userDao.getUserByIdDirect(uId);
                if (user != null) {
                    user.setAddress(newAddress);
                    userDao.update(user);
                }
            });
        } catch (Exception ignored) {}
    }

    public void listenAddressesFromFirebase(String userId, com.google.firebase.database.ValueEventListener listener) {
        if (userId == null || userId.trim().isEmpty() || "-1".equals(userId.trim())) return;
        com.google.firebase.database.DatabaseReference addrRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("addresses").child(userId);
        addrRef.addValueEventListener(listener);
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
