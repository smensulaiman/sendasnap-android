package com.sendajapan.sendasnap.networking;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NetworkUtils extends LiveData<Boolean> {

    private static NetworkUtils instance;
    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();

    private NetworkUtils(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        initNetworkCallback();
    }

    public static synchronized NetworkUtils getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkUtils(context.getApplicationContext());
        }
        return instance;
    }

    private void initNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                isConnected.postValue(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                isConnected.postValue(false);
            }
        };
    }

    public void startNetworkCallback() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
    }

    public void stopNetworkCallback() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return true;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
    }
}
