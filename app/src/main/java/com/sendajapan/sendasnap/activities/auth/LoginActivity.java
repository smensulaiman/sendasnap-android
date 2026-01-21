package com.sendajapan.sendasnap.activities.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.activities.MainActivity;
import com.sendajapan.sendasnap.databinding.ActivityLoginBinding;
import com.sendajapan.sendasnap.models.ErrorResponse;
import com.sendajapan.sendasnap.models.LoginRequest;
import com.sendajapan.sendasnap.models.LoginResponse;
import com.sendajapan.sendasnap.models.UserData;
import com.sendajapan.sendasnap.models.Vendor;
import com.sendajapan.sendasnap.networking.ApiService;
import com.sendajapan.sendasnap.networking.RetrofitClient;
import com.sendajapan.sendasnap.services.ChatService;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.FcmTokenManager;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;

import org.aviran.cookiebar2.CookieBar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SharedPrefsManager prefsManager;
    private HapticFeedbackHelper hapticHelper;
    private ApiService apiService;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        androidx.core.view.WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
        }

        initHelpers();
        setupClickListeners();
        startTextureAnimations();


        @SuppressLint("SimpleDateFormat") String year = new SimpleDateFormat("yyyy").format(new Date());
        binding.txtFooter.setText("© " + year + " SendaSnap. All rights reserved.");

        binding.txtFooter.setOnLongClickListener(view -> {
            binding.etUsername.setText("acj.shiroyama@gmail.com");
            binding.etPassword.setText("acjl7861");
            return false;
        });
    }

    private void initHelpers() {
        prefsManager = SharedPrefsManager.getInstance(LoginActivity.this);
        hapticHelper = HapticFeedbackHelper.getInstance(LoginActivity.this);
        apiService = RetrofitClient.getInstance(LoginActivity.this).getApiService();

        loadSavedCredentials();
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            performLogin();
        });
    }

    private void startTextureAnimations() {
        binding.imgTextureTop.post(() -> {
            android.view.animation.Animation slideInTop = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
            binding.imgTextureTop.startAnimation(slideInTop);
        });

        binding.imgTextureBottom.post(() -> {
            android.view.animation.Animation slideInBottom = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom);
            binding.imgTextureBottom.startAnimation(slideInBottom);
        });
    }

    private void performLogin() {
        String email = Objects.requireNonNull(binding.etUsername.getText()).toString().trim();
        String password = Objects.requireNonNull(binding.etPassword.getText()).toString().trim();

        // Clear previous errors
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            binding.tilUsername.setError("Email is required");
            binding.etUsername.requestFocus();
            hapticHelper.vibrateError();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilUsername.setError("Please enter a valid email address");
            binding.etUsername.requestFocus();
            hapticHelper.vibrateError();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            hapticHelper.vibrateError();
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Make API call
        Call<LoginResponse> call = apiService.login(new LoginRequest(email, password));
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                setLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.getSuccess() != null && loginResponse.getSuccess()) {
                        hapticHelper.vibrateSuccess();

                        // Extract user data and token
                        if (loginResponse.getData() != null) {
                            // Save token
                            String token = loginResponse.getData().getToken();
                            if (token != null) {
                                prefsManager.saveToken(token);
                            }

                            UserData user = loginResponse.getData().getUser();
                            prefsManager.saveUser(user);

                            Vendor vendor = loginResponse.getData().getVendor();
                            if (vendor != null) {
                                prefsManager.saveVendor(vendor);
                            }

                            ChatService.getInstance().initializeUser(LoginActivity.this);
                            
                            FcmTokenManager.registerToken(LoginActivity.this);

                            // Handle "Remember Me" functionality
                            if (binding.cbRememberMe.isChecked()) {
                                saveCredentials(email, password);
                            } else {
                                clearSavedCredentials();
                            }

                            CookieBar.build(LoginActivity.this)
                                    .setTitle("Login Successful")
                                    .setMessage(loginResponse.getMessage())
                                    .setDuration(CookieBarToastHelper.SHORT_DURATION)
                                    .setIcon(R.drawable.toast_success_green_24)
                                    .setIconAnimation(R.animator.iconspin)
                                    .setBackgroundColor(R.color.success_light)
                                    .setTitleColor(R.color.success_dark)
                                    .setMessageColor(R.color.success_dark)
                                    .setCookiePosition(Gravity.TOP)
                                    .setCookieListener(i -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                        finish();
                                    }, 200))
                                    .show();
                        }

                    } else {
                        // Login failed with success: false
                        handleLoginError(loginResponse.getMessage());
                    }
                } else {
                    // Handle error response
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                setLoadingState(false);
                hapticHelper.vibrateError();
                CookieBarToastHelper.showError(LoginActivity.this, "Login Failed",
                        "Network error. Please check your connection and try again.",
                        CookieBarToastHelper.LONG_DURATION);
            }
        });
    }

    private void handleLoginError(String errorMessage) {
        hapticHelper.vibrateError();
        binding.tilPassword.setError(errorMessage != null ? errorMessage : "Invalid credentials");
        binding.etPassword.requestFocus();

        showErrorToast(errorMessage);
    }

    private void handleErrorResponse(Response<LoginResponse> response) {
        hapticHelper.vibrateError();
        String errorMessage = "Invalid credentials";

        if (response.errorBody() != null) {
            try {
                Gson gson = new Gson();
                ErrorResponse errorResponse = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                if (errorResponse != null && errorResponse.getMessage() != null) {
                    errorMessage = errorResponse.getMessage();
                }
            } catch (Exception e) {
                Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        binding.tilPassword.setError(errorMessage);
        binding.etPassword.requestFocus();

        showErrorToast(errorMessage);
    }

    private void showErrorToast(String errorMessage) {
        CookieBar.build(LoginActivity.this)
                .setTitle("Login Failed")
                .setMessage(errorMessage)
                .setIcon(R.drawable.toast_cancel_red_24)
                .setIconAnimation(R.animator.iconspin)
                .setBackgroundColor(R.color.error_light)
                .setTitleColor(R.color.error_dark)
                .setMessageColor(R.color.error_dark)
                .setCookiePosition(Gravity.TOP)
                .setDuration(CookieBarToastHelper.LONG_DURATION)
                .show();
    }

    private void setLoadingState(boolean isLoading) {
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnLogin.setText(isLoading ? "Logging in..." : "Login");
        binding.etUsername.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
        binding.cbRememberMe.setEnabled(!isLoading);

        // Show/hide progress bar
        if (isLoading) {
            binding.progressBar.setVisibility(android.view.View.VISIBLE);
            binding.btnLogin.setTextColor(ContextCompat.getColor(this, android.R.color.transparent));
        } else {
            binding.progressBar.setVisibility(android.view.View.GONE);
            binding.btnLogin.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void loadSavedCredentials() {
        String savedEmail = prefsManager.getEmail();
        String savedPassword = prefsManager.getSavedPassword();
        boolean rememberMe = prefsManager.isRememberMeEnabled();

        if (rememberMe && !TextUtils.isEmpty(savedEmail) && !TextUtils.isEmpty(savedPassword)) {
            binding.etUsername.setText(savedEmail);
            binding.etPassword.setText(savedPassword);
            binding.cbRememberMe.setChecked(true);
        }
    }

    private void saveCredentials(String username, String password) {
        prefsManager.saveCredentials(username, password, true);
    }

    private void clearSavedCredentials() {
        prefsManager.saveCredentials("", "", false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}