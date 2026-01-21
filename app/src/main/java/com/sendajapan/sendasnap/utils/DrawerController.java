package com.sendajapan.sendasnap.utils;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.activities.HistoryActivity;
import com.sendajapan.sendasnap.activities.MainActivity;
import com.sendajapan.sendasnap.activities.shipment.ScheduleListActivity;

public class DrawerController {

    private final AppCompatActivity activity;
    private final DrawerLayout drawerLayout;
    private final NavigationView navigationView;
    private MaterialToolbar toolbar;
    private final HapticFeedbackHelper hapticHelper;

    public DrawerController(AppCompatActivity activity, DrawerLayout drawerLayout,
            NavigationView navigationView, MaterialToolbar toolbar) {
        this.activity = activity;
        this.drawerLayout = drawerLayout;
        this.navigationView = navigationView;
        this.toolbar = toolbar;
        this.hapticHelper = HapticFeedbackHelper.getInstance(activity);

        setupDrawer();
    }

    private void setupDrawer() {
        // Only set up if toolbar is not null
        if (toolbar != null) {
            // Set hamburger icon and make it clickable
            toolbar.setNavigationIcon(R.drawable.ic_menu);
            toolbar.setNavigationOnClickListener(v -> {
                hapticHelper.vibrateClick();
                drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        // Setup custom header layout click handlers
        setupCustomDrawerMenu();
    }

    private void setupCustomDrawerMenu() {
        // Inflate custom drawer content and add as body (not header)
        LayoutInflater inflater = LayoutInflater.from(activity);
        View drawerContent = inflater.inflate(R.layout.nav_drawer_header, navigationView, false);
        navigationView.addView(drawerContent);

        // Setup close button
        ImageButton closeButton = drawerContent.findViewById(R.id.buttonCloseDrawer);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }

        setupMenuItemClick(drawerContent, R.id.menu_settings, this::handleSettingsNavigation);
        setupMenuItemClick(drawerContent, R.id.menu_history, this::handleHistoryNavigation);
        setupMenuItemClick(drawerContent, R.id.menu_shipment_schedule, this::handleShipmentScheduleNavigation);
        setupMenuItemClick(drawerContent, R.id.menu_logout, this::handleLogoutNavigation);
        
        // Set dynamic copyright year
        TextView txtCopyright = drawerContent.findViewById(R.id.txtCopyright);
        if (txtCopyright != null) {
            @SuppressLint("SimpleDateFormat") String year = new SimpleDateFormat("yyyy").format(new Date());
            txtCopyright.setText("© " + year + " SendaSnap. All rights reserved.");
        }
    }

    private void setupMenuItemClick(View headerView, int menuItemId, Runnable action) {
        View menuItem = headerView.findViewById(menuItemId);
        if (menuItem != null) {
            menuItem.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                action.run();
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        }
    }

    private void handleSettingsNavigation() {
        // TODO: Implement SettingsActivity
        Toast.makeText(activity, "Settings coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void handleHistoryNavigation() {
        if (activity instanceof HistoryActivity) {
            return;
        }

        Intent intent = new Intent(activity, HistoryActivity.class);
        activity.startActivity(intent);
    }

    private void handleShipmentScheduleNavigation() {
        Intent intent = new Intent(activity, ScheduleListActivity.class);
        activity.startActivity(intent);
    }

    private void handleLogoutNavigation() {
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;
            mainActivity.showLogoutDialog();
        } else {
            // For other activities, show dialog using a helper method
            showLogoutDialog();
        }
    }

    private void showLogoutDialog() {
        android.app.Dialog dialog = new android.app.Dialog(activity, R.style.DialogTheme);
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

        com.google.android.material.button.MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnLogout = dialog.findViewById(R.id.btnLogout);

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
        com.sendajapan.sendasnap.utils.FcmNotificationSender.removeNotificationListener();
        com.sendajapan.sendasnap.utils.ChatMessageListener.removeChatMessageListener();
        com.sendajapan.sendasnap.utils.ChatMessageListener.clearProcessedTimestamps();

        SharedPrefsManager prefsManager = SharedPrefsManager.getInstance(activity);
        prefsManager.logout();

        Intent intent = new Intent(activity, com.sendajapan.sendasnap.activities.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);

        activity.finish();
    }

    public void setToolbarTitle(String title) {
        toolbar.setTitle(title);
    }

    public void setToolbarLogo(int logoResId) {
        if (toolbar != null) {
            toolbar.setLogo(logoResId);
        }
    }

    public void updateToolbar(MaterialToolbar newToolbar) {
        this.toolbar = newToolbar;
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_menu);
            toolbar.setNavigationOnClickListener(v -> {
                hapticHelper.vibrateClick();
                drawerLayout.openDrawer(GravityCompat.START);
            });
        }
    }
}
