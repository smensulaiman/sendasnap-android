package com.sendajapan.sendasnap.models;

import com.google.gson.annotations.SerializedName;

public class LoginData {
    @SerializedName("user")
    private UserData user;

    @SerializedName("vendor")
    private Vendor vendor;

    @SerializedName("token")
    private String token;

    public LoginData() {
    }

    public UserData getUser() {
        return user;
    }

    public void setUser(UserData user) {
        this.user = user;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

