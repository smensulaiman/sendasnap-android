package com.sendajapan.sendasnap.utils;

import android.app.Activity;
import android.content.IntentSender;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallException;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallErrorCode;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;

/**
 * Utility class to handle Google Play in-app updates.
 * Supports both immediate and flexible update flows.
 */
public class AppUpdateManager {

    private static final String TAG = "AppUpdateManager";
    private static final int REQUEST_CODE_UPDATE = 100;
    private static final int REQUEST_CODE_FLEXIBLE_UPDATE = 101;

    private final Activity activity;
    private final com.google.android.play.core.appupdate.AppUpdateManager playAppUpdateManager;

    public AppUpdateManager(@NonNull Activity activity) {
        this.activity = activity;
        this.playAppUpdateManager = AppUpdateManagerFactory.create(activity);
    }

    /**
     * Check for app updates and handle them appropriately.
     * Uses immediate update for critical updates (staleAllowedDays = 0)
     * and flexible update for non-critical updates.
     *
     * @param staleAllowedDays Number of days before forcing an immediate update (0 = always immediate)
     */
    public void checkForAppUpdate(int staleAllowedDays) {
        Task<AppUpdateInfo> appUpdateInfoTask = playAppUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            int updateAvailability = appUpdateInfo.updateAvailability();
            int availableVersionCode = appUpdateInfo.availableVersionCode();
            Integer stalenessDays = appUpdateInfo.clientVersionStalenessDays();
            int currentStalenessDays = stalenessDays != null ? stalenessDays : 0;

            Log.d(TAG, "Update availability: " + updateAvailability);
            Log.d(TAG, "Available version code: " + availableVersionCode);
            Log.d(TAG, "Staleness days: " + currentStalenessDays);

            if (updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
                // Check if update is required (stale) or optional
                boolean isUpdateStale = currentStalenessDays >= staleAllowedDays;

                if (isUpdateStale && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    // Critical update - use immediate flow
                    startImmediateUpdate(appUpdateInfo);
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    // Non-critical update - use flexible flow
                    startFlexibleUpdate(appUpdateInfo);
                } else {
                    Log.w(TAG, "Update available but not allowed for current update type");
                }
            } else if (updateAvailability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Resume an update that was already started
                resumeUpdate(appUpdateInfo);
            } else {
                Log.d(TAG, "No update available. Availability: " + updateAvailability);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to check for app update", e);
        });
    }

    /**
     * Start an immediate update flow.
     * User must update before continuing to use the app.
     */
    private void startImmediateUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            playAppUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    REQUEST_CODE_UPDATE
            );
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Failed to start immediate update", e);
        }
    }

    /**
     * Start a flexible update flow.
     * User can continue using the app while update downloads in background.
     */
    private void startFlexibleUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            playAppUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    activity,
                    REQUEST_CODE_FLEXIBLE_UPDATE
            );

            // Monitor flexible update state
            com.google.android.play.core.install.InstallStateUpdatedListener[] listenerRef = new com.google.android.play.core.install.InstallStateUpdatedListener[1];
            listenerRef[0] = state -> {
                if (state.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
                    // Update downloaded, show completion dialog
                    showUpdateCompleteSnackbar();
                } else if (state.installStatus() == com.google.android.play.core.install.model.InstallStatus.INSTALLED) {
                    // Update installed, unregister listener
                    if (listenerRef[0] != null) {
                        playAppUpdateManager.unregisterListener(listenerRef[0]);
                    }
                }
            };
            playAppUpdateManager.registerListener(listenerRef[0]);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Failed to start flexible update", e);
        }
    }

    /**
     * Resume an update that was already in progress.
     */
    private void resumeUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                playAppUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        activity,
                        REQUEST_CODE_UPDATE
                );
            } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                playAppUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        activity,
                        REQUEST_CODE_FLEXIBLE_UPDATE
                );
            }
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Failed to resume update", e);
        }
    }

    /**
     * Show a snackbar when flexible update is downloaded and ready to install.
     */
    private void showUpdateCompleteSnackbar() {
        // This will be called when the update is downloaded
        // You can show a snackbar or dialog to prompt user to restart
        Log.d(TAG, "Update downloaded, ready to install");
        
        // Note: In a real implementation, you might want to show a snackbar here
        // For now, we'll just log it. The user can be notified through other means.
    }

    /**
     * Complete the flexible update by restarting the app.
     * Call this when user taps "Restart" after update is downloaded.
     */
    public void completeFlexibleUpdate() {
        playAppUpdateManager.completeUpdate();
    }

    /**
     * Check if an update is in progress and resume if needed.
     * Call this in onResume() to handle cases where user returns to app during update.
     */
    public void checkForUpdateInProgress() {
        Task<AppUpdateInfo> appUpdateInfoTask = playAppUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                resumeUpdate(appUpdateInfo);
            } else if (appUpdateInfo.installStatus() == com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
                showUpdateCompleteSnackbar();
            }
        });
    }

    /**
     * Get the request code for immediate updates.
     */
    public static int getImmediateUpdateRequestCode() {
        return REQUEST_CODE_UPDATE;
    }

    /**
     * Get the request code for flexible updates.
     */
    public static int getFlexibleUpdateRequestCode() {
        return REQUEST_CODE_FLEXIBLE_UPDATE;
    }

    /**
     * Interface for update check callbacks.
     */
    public interface UpdateCheckCallback {
        void onUpdateAvailable();
        void onNoUpdateAvailable();
        void onError(Exception e);
        void onAppNotOwned(); // Called when app is not installed from Play Store
    }

    /**
     * Check for app update with callback.
     * This method only checks for update availability without automatically starting the update flow.
     */
    public void checkForUpdateWithCallback(UpdateCheckCallback callback) {
        Task<AppUpdateInfo> appUpdateInfoTask = playAppUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            int updateAvailability = appUpdateInfo.updateAvailability();
            
            if (updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
                callback.onUpdateAvailable();
            } else {
                callback.onNoUpdateAvailable();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to check for app update", e);
            
            // Check if the error is because app is not installed from Play Store
            if (e instanceof InstallException) {
                InstallException installException = (InstallException) e;
                int errorCode = installException.getErrorCode();
                
                if (errorCode == InstallErrorCode.ERROR_APP_NOT_OWNED) {
                    // App is not installed from Play Store
                    callback.onAppNotOwned();
                    return;
                }
            }
            
            callback.onError(e);
        });
    }

    /**
     * Start the update flow.
     * This will check for available updates and start the appropriate update flow.
     */
    public void startUpdateFlow() {
        Task<AppUpdateInfo> appUpdateInfoTask = playAppUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            int updateAvailability = appUpdateInfo.updateAvailability();
            
            if (updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
                // Prefer flexible update for user-initiated updates
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    startFlexibleUpdate(appUpdateInfo);
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    startImmediateUpdate(appUpdateInfo);
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to start update flow", e);
        });
    }
}
