package com.sendajapan.sendasnap.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sendajapan.sendasnap.models.shipment.Port;
import com.sendajapan.sendasnap.models.shipment.ShippingCompany;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ScheduleCache {

    private static final String PREFS_NAME = "ScheduleCachePrefs";
    private static final String KEY_PORTS = "cached_ports";
    private static final String KEY_SHIPPING_COMPANIES = "cached_shipping_companies";
    private static final String KEY_PORTS_TIMESTAMP = "ports_cache_timestamp";
    private static final String KEY_SHIPPING_COMPANIES_TIMESTAMP = "shipping_companies_cache_timestamp";

    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000;

    private final SharedPreferences prefs;
    private final Gson gson;

    public ScheduleCache(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void savePorts(List<Port> ports) {
        String portsJson = gson.toJson(ports);
        prefs.edit()
                .putString(KEY_PORTS, portsJson)
                .putLong(KEY_PORTS_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    public List<Port> getPorts() {
        String portsJson = prefs.getString(KEY_PORTS, null);
        if (portsJson != null) {
            Type listType = new TypeToken<List<Port>>() {
            }.getType();
            return gson.fromJson(portsJson, listType);
        }
        return new ArrayList<>();
    }

    public boolean isPortsCacheValid() {
        long timestamp = prefs.getLong(KEY_PORTS_TIMESTAMP, 0);
        return (System.currentTimeMillis() - timestamp) < CACHE_DURATION_MS;
    }

    public void clearPortsCache() {
        prefs.edit()
                .remove(KEY_PORTS)
                .remove(KEY_PORTS_TIMESTAMP)
                .apply();
    }

    public void saveShippingCompanies(List<ShippingCompany> shippingCompanies) {
        String companiesJson = gson.toJson(shippingCompanies);
        prefs.edit()
                .putString(KEY_SHIPPING_COMPANIES, companiesJson)
                .putLong(KEY_SHIPPING_COMPANIES_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    public List<ShippingCompany> getShippingCompanies() {
        String companiesJson = prefs.getString(KEY_SHIPPING_COMPANIES, null);
        if (companiesJson != null) {
            Type listType = new TypeToken<List<ShippingCompany>>() {
            }.getType();
            return gson.fromJson(companiesJson, listType);
        }
        return new ArrayList<>();
    }

    public boolean isShippingCompaniesCacheValid() {
        long timestamp = prefs.getLong(KEY_SHIPPING_COMPANIES_TIMESTAMP, 0);
        return (System.currentTimeMillis() - timestamp) < CACHE_DURATION_MS;
    }

    public void clearShippingCompaniesCache() {
        prefs.edit()
                .remove(KEY_SHIPPING_COMPANIES)
                .remove(KEY_SHIPPING_COMPANIES_TIMESTAMP)
                .apply();
    }

    public void clearAllCache() {
        prefs.edit().clear().apply();
    }
}
