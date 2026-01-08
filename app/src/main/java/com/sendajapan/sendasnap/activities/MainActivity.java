package com.sendajapan.sendasnap.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sendajapan.sendasnap.MyApplication;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.databinding.ActivityMainBinding;
import com.sendajapan.sendasnap.fragments.HomeFragment;
import com.sendajapan.sendasnap.fragments.ProfileFragment;
import com.sendajapan.sendasnap.fragments.ScheduleFragment;
import com.sendajapan.sendasnap.networking.NetworkUtils;
import com.google.firebase.database.ValueEventListener;
import com.sendajapan.sendasnap.activities.auth.LoginActivity;
import com.sendajapan.sendasnap.utils.ChatMessageListener;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.DrawerController;
import com.sendajapan.sendasnap.utils.FcmNotificationSender;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.NotificationHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;

import me.ibrahimsn.lib.OnItemSelectedListener;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private HapticFeedbackHelper hapticHelper;
    private NetworkUtils networkUtils;

    private boolean isNetworkToastShowing = false;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    
    private MenuItem notificationsMenuItem;
    private TextView badgeTextView;
    private ValueEventListener unreadCountListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, 0);
            return insets;
        });

        MyApplication.applyWindowInsets(binding.getRoot());

        initHelpers();
        setupDrawer();
        setupBottomNavigation();
        setupNetworkMonitoring();
        setupBackPressHandler();
        checkAndRequestNotificationPermission();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        } else {
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (!(currentFragment instanceof HomeFragment)) {
            getMenuInflater().inflate(R.menu.toolbar_menu, menu);
            setupNotificationIcon(menu);
            
            if (currentFragment instanceof ScheduleFragment || currentFragment instanceof ProfileFragment) {
                MenuItem chatMenuItem = menu.findItem(R.id.action_chat);
                if (chatMenuItem != null) {
                    chatMenuItem.setVisible(false);
                }
            }
        }
        return true;
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
            
            new Handler(Looper.getMainLooper()).postDelayed(this::setupNotificationBadge, 300);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (currentFragment != null && currentFragment.onOptionsItemSelected(item)) {
            return true;
        }

        hapticHelper.vibrateClick();
        int itemId = item.getItemId();
        if (itemId == R.id.action_notifications) {
            openNotifications();
            return true;
        } else if (itemId == R.id.action_about) {
            openAbout();
            return true;
        } else if (itemId == R.id.action_logout) {
            handleLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openNotifications() {
        Intent intent = new Intent(this, NotificationsActivity.class);
        startActivity(intent);
    }
    
    private void setupNotificationBadge() {
        if (!SharedPrefsManager.getInstance(this).isLoggedIn()) {
            return;
        }
        
        // Get initial count
        NotificationHelper.getUnreadNotificationCount(this, new NotificationHelper.UnreadCountCallback() {
            @Override
            public void onSuccess(int unreadCount) {
                updateNotificationBadge(unreadCount);
            }

            @Override
            public void onFailure(Exception e) {
                // Silently fail
            }
        });
        
        // Setup real-time listener
        unreadCountListener = NotificationHelper.addUnreadCountListener(this, new NotificationHelper.UnreadCountCallback() {
            @Override
            public void onSuccess(int unreadCount) {
                updateNotificationBadge(unreadCount);
            }

            @Override
            public void onFailure(Exception e) {
                // Silently fail
            }
        });
    }
    
    private void updateNotificationBadge(int unreadCount) {
        runOnUiThread(() -> {
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
                    badgeTextView.invalidate();
                    badgeTextView.requestLayout();
                } else {
                    badgeTextView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void handleLogout() {
        AlertDialog dialog = getDialog();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            if (positiveButton instanceof MaterialButton) {
                positiveButton.setBackgroundTintList(
                        ColorStateList.valueOf(
                                getResources().getColor(R.color.error, null)
                        )
                );
            } else {
                positiveButton.setBackgroundColor(getResources().getColor(R.color.error, null));
            }
            positiveButton.setTextColor(getResources().getColor(R.color.on_primary, null));
            positiveButton.setAllCaps(false);
        }

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_secondary, null));
            negativeButton.setAllCaps(false);
        }
    }

    @NonNull
    private AlertDialog getDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        builder.setPositiveButton("Logout", (dialog, which) -> {
            hapticHelper.vibrateClick();
            performLogout();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            hapticHelper.vibrateClick();
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    private void performLogout() {
        FcmNotificationSender.removeNotificationListener();
        ChatMessageListener.removeChatMessageListener();
        ChatMessageListener.clearProcessedTimestamps();
        
        SharedPrefsManager prefsManager = SharedPrefsManager.getInstance(this);
        prefsManager.logout();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();
    }

    private void openAbout() {
        Toast.makeText(this, "coming soon", Toast.LENGTH_SHORT).show();
    }

    private void initHelpers() {
        networkUtils = NetworkUtils.getInstance(this);
        hapticHelper = HapticFeedbackHelper.getInstance(this);
    }

    /**
     * Check and request notification permission
     * For Android 13+ (API 33+): Requires POST_NOTIFICATIONS permission
     * For Android 11 (API 30): Checks if notifications are enabled in system settings
     */
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires POST_NOTIFICATIONS permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                
                // Check if we should show rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    // Show explanation dialog
                    showNotificationPermissionRationale();
                } else {
                    // Request permission directly
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                    );
                }
            } else {
                // Permission granted, check if notifications are enabled in system settings
                checkNotificationSettings();
            }
        } else {
            // Android 11 and below: Check if notifications are enabled in system settings
            checkNotificationSettings();
        }
    }

    /**
     * Show rationale dialog for notification permission
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void showNotificationPermissionRationale() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Notification Permission Required")
                .setMessage("SendaSnap needs notification permission to notify you about new task assignments. " +
                        "Please allow notifications to receive important updates.")
                .setPositiveButton("Allow", (dialog, which) -> {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                    );
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    // Show info that notifications won't work
                    CookieBarToastHelper.showWarning(
                            this,
                            "Notifications Disabled",
                            "You can enable notifications later in app settings",
                            CookieBarToastHelper.SHORT_DURATION
                    );
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Check if notifications are enabled in system settings
     * For Android 11 and below, this is the only way to check
     */
    private void checkNotificationSettings() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (!notificationManager.areNotificationsEnabled()) {
            // Notifications are disabled, show dialog to open settings
            showNotificationSettingsDialog();
        }
    }

    /**
     * Show dialog to open notification settings
     */
    private void showNotificationSettingsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable Notifications")
                .setMessage("Notifications are currently disabled. Please enable them in system settings to receive task assignment notifications.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    openNotificationSettings();
                })
                .setNegativeButton("Later", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Open app notification settings
     */
    private void openNotificationSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, check system notification settings
                checkNotificationSettings();
                CookieBarToastHelper.showSuccess(
                        this,
                        "Permission Granted",
                        "You will now receive task assignment notifications",
                        CookieBarToastHelper.SHORT_DURATION
                );
            } else {
                // Permission denied
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    // User denied but can still be asked again
                    CookieBarToastHelper.showWarning(
                            this,
                            "Permission Denied",
                            "Notifications are disabled. You can enable them in app settings",
                            CookieBarToastHelper.LONG_DURATION
                    );
                } else {
                    // User denied and selected "Don't ask again"
                    showNotificationSettingsDialog();
                }
            }
        }
    }

    private void setupDrawer() {
        setSupportActionBar(binding.toolbar);
        supportInvalidateOptionsMenu();
        new DrawerController(this,
                binding.drawerLayout,
                binding.navigationView,
                binding.toolbar);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener((OnItemSelectedListener) pos -> {
            hapticHelper.vibrateClick();

            Fragment selectedFragment = null;

            switch (pos) {
                case 0:
                    selectedFragment = new HomeFragment();
                    break;
                case 1:
                    selectedFragment = new ScheduleFragment();
                    break;
                case 2:
                    selectedFragment = new ProfileFragment();
                    break;
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();

        updateToolbarTitle(fragment);
    }

    private void updateToolbarTitle(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            binding.toolbar.setTitle("Home");
            invalidateOptionsMenu();
        } else if (fragment instanceof ScheduleFragment) {
            binding.toolbar.setTitle("Work Schedule");
            invalidateOptionsMenu();
        } else if (fragment instanceof ProfileFragment) {
            binding.toolbar.setTitle("Profile");
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);

        boolean showMenu = currentFragment instanceof HomeFragment;

        MenuItem notificationsItem = menu.findItem(R.id.action_notifications);
        MenuItem moreItem = menu.findItem(R.id.action_more);

        if (notificationsItem != null) {
            notificationsItem.setVisible(showMenu);
        }
        if (moreItem != null) {
            moreItem.setVisible(showMenu);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    public void switchToProfileTab() {
        binding.bottomNavigation.setItemActiveIndex(2);
        loadFragment(new ProfileFragment());
    }

    private void setupNetworkMonitoring() {
        networkUtils.startNetworkCallback();

        networkUtils.getIsConnected().observe(this, isConnected -> {
            if (isConnected) {
                if (isNetworkToastShowing) {
                    isNetworkToastShowing = false;
                }
            } else {
                if (!isNetworkToastShowing) {
                    isNetworkToastShowing = true;
                    showNoInternetToast();
                }
            }
        });
    }

    private void showNoInternetToast() {
        CookieBarToastHelper.showNoInternet(this);
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void showExitConfirmationDialog() {
        hapticHelper.vibrateClick();

        AlertDialog dialog = getAlertDialog();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            if (positiveButton instanceof MaterialButton) {
                positiveButton.setBackgroundTintList(
                        ColorStateList.valueOf(
                                getResources().getColor(R.color.red_700, null)
                        )
                );
            } else {
                positiveButton.setBackgroundColor(getResources().getColor(R.color.red_700, null));
            }
            positiveButton.setTextColor(getResources().getColor(R.color.on_primary, null));
            positiveButton.setAllCaps(false);
        }

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_secondary, null));
            negativeButton.setAllCaps(false);
        }
    }

    @NonNull
    private AlertDialog getAlertDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Exit App?");
        builder.setMessage("Are you sure you want to exit Senda Snap?");
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        builder.setPositiveButton("Exit", (dialog, which) -> {
            hapticHelper.vibrateClick();
            finishAffinity();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            hapticHelper.vibrateClick();
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Remove notification listener
        if (unreadCountListener != null) {
            NotificationHelper.removeUnreadCountListener(this, unreadCountListener);
            unreadCountListener = null;
        }
        
        if (networkUtils != null) {
            networkUtils.stopNetworkCallback();
        }
        binding = null;
    }
}
