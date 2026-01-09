package com.sendajapan.sendasnap.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.activities.NotificationsActivity;
import com.sendajapan.sendasnap.activities.VehicleDetailsActivity;
import com.sendajapan.sendasnap.activities.auth.LoginActivity;
import com.sendajapan.sendasnap.adapters.VehicleAdapter;
import com.sendajapan.sendasnap.databinding.FragmentHomeBinding;
import com.sendajapan.sendasnap.dialogs.LoadingDialog;
import com.sendajapan.sendasnap.dialogs.VehicleSearchDialog;
import com.sendajapan.sendasnap.models.ErrorResponse;
import com.sendajapan.sendasnap.models.UserData;
import com.sendajapan.sendasnap.models.Vehicle;
import com.sendajapan.sendasnap.models.VehicleSearchResponse;
import com.sendajapan.sendasnap.networking.ApiService;
import com.sendajapan.sendasnap.networking.NetworkUtils;
import com.sendajapan.sendasnap.networking.RetrofitClient;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.NotificationHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;
import com.sendajapan.sendasnap.utils.VehicleCache;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements VehicleAdapter.OnVehicleClickListener {

    private FragmentHomeBinding binding;
    private ApiService apiService;
    private HapticFeedbackHelper hapticHelper;
    private LoadingDialog loadingDialog;
    private NetworkUtils networkUtils;
    private VehicleAdapter vehicleAdapter;
    private VehicleCache vehicleCache;
    
    private MenuItem notificationsMenuItem;
    private TextView badgeTextView;
    private ValueEventListener unreadCountListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true);

        initHelpers();
        setupRecyclerView();
        setupClickListeners();
        setupFAB();
        loadUserProfile();
        loadRecentVehicles();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && getContext() != null) {
                setupNotificationBadge();
            }
        }, 500);
    }

    private void initHelpers() {
        vehicleCache = VehicleCache.getInstance(requireContext());
        hapticHelper = HapticFeedbackHelper.getInstance(requireContext());
        apiService = RetrofitClient.getInstance(requireContext()).getApiService();
        networkUtils = NetworkUtils.getInstance(requireContext());
    }

    private void setupRecyclerView() {
        vehicleAdapter = new VehicleAdapter(this);
        binding.recyclerViewVehicles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewVehicles.setAdapter(vehicleAdapter);
    }

    private void setupClickListeners() {
        binding.btnViewAll.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
        });

        binding.btnQuickSearch.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            showSearchDialog();
        });

        binding.btnViewStats.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            Toast.makeText(getContext(), "Stats feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupFAB() {
        binding.fabSearch.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            showSearchDialog();
        });
    }

    private void loadUserProfile() {
        SharedPrefsManager prefsManager = SharedPrefsManager.getInstance(requireContext());
        UserData currentUser = prefsManager.getUser();

        if (currentUser != null) {
            if (binding.txtUserName != null) {
                String userName = currentUser.getName();
                if (userName != null && !userName.isEmpty()) {
                    binding.txtUserName.setText(userName + " san");
                    binding.txtUserEmail.setText(currentUser.getEmail());
                } else {
                    binding.txtUserName.setText("User");
                }
            }

            if (binding.txtUserRole != null) {
                String userRole = currentUser.getRole();
                if (userRole != null && !userRole.isEmpty()) {
                    String capitalizedRole = userRole.substring(0, 1).toUpperCase() +
                            userRole.substring(1).toLowerCase();
                    binding.txtUserRole.setText(capitalizedRole);
                } else {
                    binding.txtUserRole.setText("User");
                }
            }

            if (binding.imgProfile != null) {
                String avatarUrl = currentUser.getAvatarUrl() != null ? currentUser.getAvatarUrl() : currentUser.getAvatar();

                if (avatarUrl != null && !avatarUrl.isEmpty() && isValidUrl(avatarUrl)) {
                    Glide.with(requireContext())
                            .load(avatarUrl)
                            .placeholder(R.drawable.avater_placeholder)
                            .error(R.drawable.avater_placeholder)
                            .circleCrop()
                            .into(binding.imgProfile);
                } else {
                    binding.imgProfile.setImageResource(R.drawable.avater_placeholder);
                }
            }
        }
    }

    private boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private void showSearchDialog() {
        new VehicleSearchDialog.Builder(requireContext())
                .setOnSearchListener(this::performSearchFromDialog)
                .setCancelable(true)
                .show();
    }

    private void showLoadingDialog(String message) {
        hideLoadingDialog();

        loadingDialog = new LoadingDialog.Builder(requireContext())
                .setMessage(message)
                .setSubtitle("Please wait while we fetch the data")
                .setCancelable(false)
                .setShowProgressIndicator(false)
                .show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    private void performSearchFromDialog(String searchType, String searchQuery) {
        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(requireContext());
            hapticHelper.vibrateError();
            return;
        }

        showLoadingDialog("Searching vehicles, please wait...");

        Call<VehicleSearchResponse> call = apiService.searchVehicles(searchType, searchQuery);
        call.enqueue(new Callback<VehicleSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<VehicleSearchResponse> call,
                    @NonNull Response<VehicleSearchResponse> response) {
                hideLoadingDialog();

                if (response.isSuccessful() && response.body() != null) {
                    VehicleSearchResponse searchResponse = response.body();

                    if (searchResponse.getSuccess() != null && searchResponse.getSuccess()) {
                        VehicleSearchResponse.VehicleSearchData data = searchResponse.getData();
                        if (data != null && data.getVehicles() != null) {
                            List<Vehicle> vehicles = data.getVehicles();

                            if (vehicles.isEmpty()) {
                                CookieBarToastHelper.showError(requireContext(), "No Vehicle",
                                        "No vehicles found matching your search criteria",
                                        CookieBarToastHelper.EXTRA_LONG_DURATION);

                            } else if (vehicles.size() == 1) {
                                Vehicle vehicle = vehicles.get(0);
                                vehicleCache.addVehicle(vehicle);

                                Intent intent = new Intent(requireContext(), VehicleDetailsActivity.class);
                                intent.putExtra("vehicle", vehicle);
                                startActivity(intent);

                                CookieBarToastHelper.showSuccess(requireContext(), "Vehicle Found",
                                        "Vehicle details loaded successfully!",
                                        CookieBarToastHelper.LONG_DURATION);
                            } else {
                                showSearchResultsDialog(vehicles);
                            }
                        } else {
                            CookieBarToastHelper.showError(requireContext(), "Error",
                                    "No vehicle data received",
                                    CookieBarToastHelper.LONG_DURATION);
                        }
                    } else {
                        String message = searchResponse.getMessage() != null ? searchResponse.getMessage()
                                : "Search failed";
                        CookieBarToastHelper.showError(requireContext(), "Error", message,
                                CookieBarToastHelper.LONG_DURATION);
                    }
                } else {
                    String errorMessage = "Failed to search vehicles. Please try again.";
                    if (response.errorBody() != null) {
                        try {
                            String errorBodyStr = response.errorBody().string();
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(errorBodyStr,
                                    ErrorResponse.class);
                            if (errorResponse != null && errorResponse.getMessage() != null) {
                                errorMessage = errorResponse.getMessage();
                            }
                        } catch (Exception e) {
                        }
                    }
                    CookieBarToastHelper.showError(requireContext(), "Error",
                            errorMessage,
                            CookieBarToastHelper.LONG_DURATION);
                }
            }

            @Override
            public void onFailure(@NonNull Call<VehicleSearchResponse> call, @NonNull Throwable t) {
                hideLoadingDialog();
                CookieBarToastHelper.showError(requireContext(), "Error",
                        "Failed to connect to server. Please check your internet connection.",
                        CookieBarToastHelper.LONG_DURATION);
                hapticHelper.vibrateError();
            }
        });
    }

    private void showSearchResultsDialog(List<Vehicle> vehicles) {
        Dialog resultsDialog = new Dialog(requireContext());
        resultsDialog.setContentView(R.layout.dialog_vehicle_search_results);
        if (resultsDialog.getWindow() != null) {
            resultsDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        RecyclerView recyclerViewResults = resultsDialog.findViewById(R.id.recyclerViewResults);
        LinearLayout emptyState = resultsDialog.findViewById(R.id.layoutEmptyState);
        MaterialButton btnClose = resultsDialog.findViewById(R.id.btnClose);

        VehicleAdapter resultsAdapter = new VehicleAdapter(vehicle -> {
            hapticHelper.vibrateClick();
            resultsDialog.dismiss();

            vehicleCache.addVehicle(vehicle);

            if (vehicle != null && vehicle.getId() != null) {
                Vehicle latestVehicle = vehicleCache.getVehicleById(vehicle.getId());
                if (latestVehicle != null) {
                    vehicle = latestVehicle;
                }
            }

            Intent intent = new Intent(requireContext(), VehicleDetailsActivity.class);
            intent.putExtra("vehicle", vehicle);
            startActivity(intent);
        });

        recyclerViewResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewResults.setAdapter(resultsAdapter);
        resultsAdapter.updateVehicles(vehicles);

        if (vehicles.isEmpty()) {
            recyclerViewResults.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerViewResults.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            resultsDialog.dismiss();
        });

        resultsDialog.show();
    }

    private void loadRecentVehicles() {
        showShimmer();

        new android.os.Handler().postDelayed(() -> {
            if (!isAdded() || binding == null) {
                return;
            }

            List<Vehicle> dummyVehicles = SharedPrefsManager.getInstance(requireContext()).getRecentVehicles();

            hideShimmer();

            if (dummyVehicles.isEmpty()) {
                binding.recyclerViewVehicles.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
            } else {
                binding.recyclerViewVehicles.setVisibility(View.VISIBLE);
                binding.layoutEmptyState.setVisibility(View.GONE);
                vehicleAdapter.updateVehicles(dummyVehicles);
            }
        }, 700);
    }

    private void showShimmer() {
        if (binding == null)
            return;
        binding.shimmerVehicles.setVisibility(View.VISIBLE);
        binding.shimmerVehicles.startShimmer();
        binding.recyclerViewVehicles.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.GONE);
    }

    private void hideShimmer() {
        if (binding == null)
            return;
        binding.shimmerVehicles.stopShimmer();
        binding.shimmerVehicles.setVisibility(View.GONE);
    }

    @Override
    public void onVehicleClick(Vehicle vehicle) {
        hapticHelper.vibrateClick();

        if (vehicle != null && vehicle.getId() != null) {
            Vehicle latestVehicle = vehicleCache.getVehicleById(vehicle.getId());
            if (latestVehicle != null) {
                vehicle = latestVehicle;
            }
        }

        Intent intent = new Intent(requireContext(), VehicleDetailsActivity.class);
        intent.putExtra("vehicle", vehicle);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && binding != null && getContext() != null) {
            loadRecentVehicles();
            if (SharedPrefsManager.getInstance(getContext()).isLoggedIn()) {
                NotificationHelper.getUnreadNotificationCount(getContext(), new NotificationHelper.UnreadCountCallback() {
                    @Override
                    public void onSuccess(int unreadCount) {
                        updateNotificationBadge(unreadCount);
                    }

                    @Override
                    public void onFailure(Exception e) {
                    }
                });
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (unreadCountListener != null && getContext() != null) {
            NotificationHelper.removeUnreadCountListener(getContext(), unreadCountListener);
            unreadCountListener = null;
        }

        hideLoadingDialog();
        binding = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu, menu);
        setupNotificationIcon(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    private void setupNotificationIcon(Menu menu) {
        notificationsMenuItem = menu.findItem(R.id.action_notifications);
        if (notificationsMenuItem != null) {
            View actionView = getLayoutInflater().inflate(R.layout.menu_notification_badge, null);
            notificationsMenuItem.setActionView(actionView);
            badgeTextView = actionView.findViewById(R.id.badge_text);

            actionView.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                openNotifications();
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        hapticHelper.vibrateClick();

        int itemId = item.getItemId();
        if (itemId == R.id.action_notifications) {
            openNotifications();
            return true;
        } else if (itemId == R.id.action_chat) {
            openChat();
            return true;
        } else if (itemId == R.id.action_logout) {
            handleLogout();
            return true;
        }
        // Let MainActivity handle action_about
        return super.onOptionsItemSelected(item);
    }

    private void openNotifications() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        Intent intent = new Intent(getContext(), NotificationsActivity.class);
        startActivity(intent);
    }

    private void openChat() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        Toast.makeText(getContext(), "Under development", Toast.LENGTH_SHORT).show();
    }
    
    private void setupNotificationBadge() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (!SharedPrefsManager.getInstance(getContext()).isLoggedIn()) {
            return;
        }

        NotificationHelper.getUnreadNotificationCount(getContext(), new NotificationHelper.UnreadCountCallback() {
            @Override
            public void onSuccess(int unreadCount) {
                updateNotificationBadge(unreadCount);
            }

            @Override
            public void onFailure(Exception e) {
            }
        });

        unreadCountListener = NotificationHelper.addUnreadCountListener(getContext(), new NotificationHelper.UnreadCountCallback() {
            @Override
            public void onSuccess(int unreadCount) {
                updateNotificationBadge(unreadCount);
            }

            @Override
            public void onFailure(Exception e) {
            }
        });
    }
    
    private void updateNotificationBadge(int unreadCount) {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        getActivity().runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }

            if (badgeTextView == null && notificationsMenuItem != null) {
                View actionView = notificationsMenuItem.getActionView();
                if (actionView != null) {
                    badgeTextView = actionView.findViewById(R.id.badge_text);
                }
            }

            if (badgeTextView != null) {
                if (unreadCount > 0) {
                    String badgeText = String.valueOf(unreadCount > 99 ? "99+" : unreadCount);
                    badgeTextView.setText(badgeText);
                    badgeTextView.setVisibility(View.VISIBLE);
                } else {
                    badgeTextView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void handleLogout() {
        Dialog dialog = new Dialog(requireContext(), R.style.DialogTheme);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setDimAmount(0.5f);
        }

        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnLogout = dialog.findViewById(R.id.btnLogout);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                dialog.dismiss();
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                dialog.dismiss();
                performLogout();
            });
        }

        dialog.show();
    }

    private void performLogout() {
        SharedPrefsManager prefsManager = SharedPrefsManager.getInstance(requireContext());
        prefsManager.logout();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
