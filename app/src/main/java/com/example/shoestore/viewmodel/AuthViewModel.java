package com.example.shoestore.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;

public class AuthViewModel extends AndroidViewModel {
    private UserRepository repository;
    private MutableLiveData<User> loggedInUser = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> registrationSuccess = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new UserRepository(application);
    }

    public LiveData<User> getLoggedInUser() { return loggedInUser; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getRegistrationSuccess() { return registrationSuccess; }

    public void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage.postValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        repository.login(email, password, new UserRepository.OnUserResultCallback() {
            @Override
            public void onResult(User user) {
                loggedInUser.postValue(user);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }

    public void register(String name, String email, String password, String phone, String address) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            errorMessage.postValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        if (password.length() < 6) {
            errorMessage.postValue("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.postValue("Email không hợp lệ");
            return;
        }

        repository.getUserByEmail(email, new UserRepository.OnUserResultCallback() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    errorMessage.postValue("Email đã tồn tại");
                } else {
                    User newUser = new User(name, email, password, phone, address, "CUSTOMER");
                    repository.insert(newUser, new UserRepository.OnUserActionCallback() {
                        @Override
                        public void onSuccess() {
                            registrationSuccess.postValue(true);
                        }

                        @Override
                        public void onError(String message) {
                            errorMessage.postValue(message);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }
}
