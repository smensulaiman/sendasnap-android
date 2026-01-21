package com.sendajapan.sendasnap.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sendajapan.sendasnap.models.UserData;
import com.sendajapan.sendasnap.models.Vendor;
import com.sendajapan.sendasnap.models.Vehicle;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPrefsManager {

    private static final String PREFS_NAME = "SendaSnapPrefs";
    private static final String KEY_USER = "user";
    private static final String KEY_RECENT_VEHICLES = "recent_vehicles";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_HAPTIC_ENABLED = "haptic_enabled";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_CACHE_SIZE = "cache_size";
    private static final String KEY_SAVED_USERNAME = "saved_username";
    private static final String KEY_SAVED_PASSWORD = "saved_password";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_VENDOR = "vendor";

    private static SharedPrefsManager instance;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    private SharedPrefsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized SharedPrefsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefsManager(context.getApplicationContext());
        }
        return instance;
    }

    // User management
    public void saveUser(UserData user) {
        String userJson = gson.toJson(user);
        sharedPreferences.edit()
                .putString(KEY_USER, userJson)
                .putBoolean(KEY_IS_LOGGED_IN, user.isLoggedIn())
                .putString(KEY_USERNAME, user.getName())
                .putString(KEY_EMAIL, user.getEmail())
                .apply();
    }

    public UserData getUser() {
        String userJson = sharedPreferences.getString(KEY_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, UserData.class);
        }
        return null;
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void logout() {
        sharedPreferences.edit()
                .remove(KEY_USER)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_USERNAME)
                .remove(KEY_EMAIL)
                .remove(KEY_TOKEN)
                .remove(KEY_VENDOR)
                .apply();
    }

    public void saveVendor(Vendor vendor) {
        if (vendor != null) {
            String vendorJson = gson.toJson(vendor);
            sharedPreferences.edit()
                    .putString(KEY_VENDOR, vendorJson)
                    .apply();
        } else {
            sharedPreferences.edit()
                    .remove(KEY_VENDOR)
                    .apply();
        }
    }

    public Vendor getVendor() {
        String vendorJson = sharedPreferences.getString(KEY_VENDOR, null);
        if (vendorJson != null) {
            return gson.fromJson(vendorJson, Vendor.class);
        }
        return null;
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, "");
    }

    // Vehicle cache management
    public void saveRecentVehicles(List<Vehicle> vehicles) {
        String vehiclesJson = gson.toJson(vehicles);
        sharedPreferences.edit()
                .putString(KEY_RECENT_VEHICLES, vehiclesJson)
                .putInt(KEY_CACHE_SIZE, vehicles.size())
                .apply();
    }

    public List<Vehicle> getRecentVehicles() {
        String vehiclesJson = sharedPreferences.getString(KEY_RECENT_VEHICLES, null);

        if (vehiclesJson != null) {
            Type listType = new TypeToken<List<Vehicle>>() {
            }.getType();
            return gson.fromJson(vehiclesJson, listType);
        }

        return new ArrayList<>();
    }

    public void addVehicleToCache(Vehicle vehicle) {
        List<Vehicle> vehicles = getRecentVehicles();
        vehicles.removeIf(v -> v.getId() != null && v.getId().equals(vehicle.getId()));
        vehicles.add(0, vehicle);

        if (vehicles.size() > 100) {
            vehicles = new ArrayList<>(vehicles.subList(0, 100));
        }

        saveRecentVehicles(vehicles);
    }

    public void clearVehicleCache() {
        sharedPreferences.edit()
                .remove(KEY_RECENT_VEHICLES)
                .putInt(KEY_CACHE_SIZE, 0)
                .apply();
    }

    public int getCacheSize() {
        return sharedPreferences.getInt(KEY_CACHE_SIZE, 0);
    }

    // Settings
    public void setHapticEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply();
    }

    public boolean isHapticEnabled() {
        return sharedPreferences.getBoolean(KEY_HAPTIC_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    // Clear all data
    public void clearAllData() {
        sharedPreferences.edit().clear().apply();
    }

    // Remember Me functionality
    public void saveCredentials(String username, String password, boolean rememberMe) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (rememberMe) {
            editor.putString(KEY_SAVED_USERNAME, username);
            editor.putString(KEY_SAVED_PASSWORD, password);
        } else {
            editor.remove(KEY_SAVED_USERNAME);
            editor.remove(KEY_SAVED_PASSWORD);
        }
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.apply();
    }

    public String getSavedUsername() {
        return sharedPreferences.getString(KEY_SAVED_USERNAME, "");
    }

    public String getSavedPassword() {
        return sharedPreferences.getString(KEY_SAVED_PASSWORD, "");
    }

    public boolean isRememberMeEnabled() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    // Token management
    public void saveToken(String token) {
        sharedPreferences.edit()
                .putString(KEY_TOKEN, token)
                .apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public void clearToken() {
        sharedPreferences.edit()
                .remove(KEY_TOKEN)
                .apply();
    }

    // Generic string preference methods
    public void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }
}
