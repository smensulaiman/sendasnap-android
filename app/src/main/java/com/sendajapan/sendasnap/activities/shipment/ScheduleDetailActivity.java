package com.sendajapan.sendasnap.activities.shipment;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sendajapan.sendasnap.MyApplication;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.adapters.StopoverAdapter;
import com.google.android.material.chip.Chip;
import com.sendajapan.sendasnap.data.repository.ScheduleRepositoryImpl;
import com.sendajapan.sendasnap.databinding.ActivityShipmentScheduleDetailBinding;
import com.sendajapan.sendasnap.domain.repository.ScheduleRepository;
import com.sendajapan.sendasnap.models.shipment.Schedule;
import com.sendajapan.sendasnap.models.shipment.Stopover;
import com.sendajapan.sendasnap.networking.NetworkUtils;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.DateFormatter;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class ScheduleDetailActivity extends AppCompatActivity implements StopoverAdapter.OnStopoverClickListener, StopoverAdapter.OnStopoverLongClickListener {

    private ActivityShipmentScheduleDetailBinding binding;
    private ScheduleRepository scheduleRepository;
    private HapticFeedbackHelper hapticHelper;
    private NetworkUtils networkUtils;
    private SharedPrefsManager prefsManager;
    private StopoverAdapter stopoverAdapter;

    private Schedule schedule;
    private Integer scheduleId;
    private boolean stopoversExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityShipmentScheduleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MyApplication.applyWindowInsets(binding.getRoot());

        scheduleId = getIntent().getIntExtra("schedule_id", -1);
        if (scheduleId == -1) {
            finish();
            return;
        }

        initHelpers();
        setupToolbar();
        setupStopoversRecyclerView();
        setupClickListeners();
        // Expand stopovers by default
        toggleStopovers();
        loadSchedule();
    }

    private void initHelpers() {
        scheduleRepository = new ScheduleRepositoryImpl(this);
        hapticHelper = HapticFeedbackHelper.getInstance(this);
        networkUtils = NetworkUtils.getInstance(this);
        prefsManager = SharedPrefsManager.getInstance(this);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> {
            hapticHelper.vibrateClick();
            finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.schedule_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_edit) {
            hapticHelper.vibrateClick();
            Intent intent = new Intent(this, EditScheduleActivity.class);
            intent.putExtra("schedule_id", scheduleId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupStopoversRecyclerView() {
        stopoverAdapter = new StopoverAdapter(new ArrayList<>(), this);
        stopoverAdapter.setOnStopoverLongClickListener(this);
        binding.recyclerViewStopovers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewStopovers.setAdapter(stopoverAdapter);
    }

    private void setupClickListeners() {
        binding.txtStopoversTitle.setOnClickListener(v -> toggleStopovers());
        binding.imgExpandStopovers.setOnClickListener(v -> toggleStopovers());
        binding.btnAddStopover.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            Intent intent = new Intent(this, CreateStopoverActivity.class);
            intent.putExtra("schedule_id", scheduleId);
            startActivity(intent);
        });
    }

    private void toggleStopovers() {
        stopoversExpanded = !stopoversExpanded;
        binding.recyclerViewStopovers.setVisibility(stopoversExpanded ? View.VISIBLE : View.GONE);
        binding.imgExpandStopovers.setRotation(stopoversExpanded ? 180f : 0f);
    }

    private void loadSchedule() {
        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(this);
            return;
        }

        scheduleRepository.getSchedule(scheduleId, new ScheduleRepository.ScheduleRepositoryCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                schedule = result;
                displaySchedule();
            }

            @Override
            public void onError(String message, int errorCode) {
                handleError(errorCode, message);
            }
        });
    }

    private void displaySchedule() {
        if (schedule == null) {
            return;
        }

        binding.txtVesselName.setText(schedule.getVesselName());
        binding.txtVoyageNo.setText(schedule.getVoyageNo());

        if (schedule.getStartPort() != null) {
            String startPortText = schedule.getStartPort().getPortName();
            if (schedule.getStartPort().getPortType() != null) {
                startPortText += " (" + schedule.getStartPort().getPortType() + ")";
            }
            binding.txtStartPort.setText(startPortText);
        }

        if (schedule.getEndPort() != null) {
            String endPortText = schedule.getEndPort().getPortName();
            if (schedule.getEndPort().getPortType() != null) {
                endPortText += " (" + schedule.getEndPort().getPortType() + ")";
            }
            binding.txtEndPort.setText(endPortText);
        }

        if (schedule.getEta() != null) {
            binding.txtEta.setText(DateFormatter.formatDateForDisplay(schedule.getEta()));
        }

        if (schedule.getComment() != null && !schedule.getComment().isEmpty()) {
            binding.layoutComment.setVisibility(View.VISIBLE);
            binding.txtComment.setText(schedule.getComment());
        } else {
            binding.layoutComment.setVisibility(View.GONE);
        }

        displayCarriers();
        displayStopovers();
        displayMetadata();
    }

    private void displayCarriers() {
        binding.chipGroupCarriers.removeAllViews();
        if (schedule.getCarrier1() != null) {
            addCarrierChip(schedule.getCarrier1().getLineName());
        }
        if (schedule.getCarrier2() != null) {
            addCarrierChip(schedule.getCarrier2().getLineName());
        }
        if (schedule.getCarrier3() != null) {
            addCarrierChip(schedule.getCarrier3().getLineName());
        }
    }

    private void addCarrierChip(String carrierName) {
        Chip chip = new Chip(this);
        chip.setText(carrierName);
        chip.setTextSize(12);
        chip.setChipBackgroundColorResource(R.color.white);
        chip.setChipStrokeColorResource(R.color.primary);
        chip.setChipStrokeWidth(1);
        binding.chipGroupCarriers.addView(chip);
    }

    private void displayStopovers() {
        if (schedule.getStopovers() != null && !schedule.getStopovers().isEmpty()) {
            binding.txtStopoversTitle.setText("Stopovers (" + schedule.getStopovers().size() + ")");
            stopoverAdapter = new StopoverAdapter(schedule.getStopovers(), this);
            stopoverAdapter.setOnStopoverLongClickListener(this);
            binding.recyclerViewStopovers.setAdapter(stopoverAdapter);
        } else {
            binding.txtStopoversTitle.setText("Stopovers (0)");
        }
    }

    private void displayMetadata() {
        if (schedule.getAddedBy() != null) {
            binding.txtAddedBy.setText(schedule.getAddedBy().getName());
        }
        if (schedule.getCreatedAt() != null) {
            binding.txtCreatedAt.setText(DateFormatter.formatDateTimeForDisplay(schedule.getCreatedAt()));
        }
        if (schedule.getUpdatedAt() != null) {
            binding.txtUpdatedAt.setText(DateFormatter.formatDateTimeForDisplay(schedule.getUpdatedAt()));
        }
    }

    private void showDeleteConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Schedule")
                .setMessage("Are you sure you want to delete this schedule?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSchedule())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSchedule() {
        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(this);
            return;
        }

        scheduleRepository.deleteSchedule(scheduleId, new ScheduleRepository.ScheduleRepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                CookieBarToastHelper.showSuccess(ScheduleDetailActivity.this, "Success", "Schedule deleted successfully", CookieBarToastHelper.SHORT_DURATION);
                finish();
            }

            @Override
            public void onError(String message, int errorCode) {
                handleError(errorCode, message);
            }
        });
    }

    private void handleError(int errorCode, String message) {
        if (errorCode == 401) {
            CookieBarToastHelper.showError(this, "Unauthorized", "Please login again", CookieBarToastHelper.SHORT_DURATION);
        } else if (errorCode == 403) {
            CookieBarToastHelper.showError(this, "Forbidden", "You don't have permission", CookieBarToastHelper.SHORT_DURATION);
        } else if (errorCode == 404) {
            CookieBarToastHelper.showError(this, "Not Found", "Schedule not found", CookieBarToastHelper.SHORT_DURATION);
        } else if (errorCode == 500) {
            CookieBarToastHelper.showError(this, "Server Error", "Server error, please try again", CookieBarToastHelper.SHORT_DURATION);
        } else {
            CookieBarToastHelper.showError(this, "Error", message != null ? message : "An error occurred", CookieBarToastHelper.SHORT_DURATION);
        }
    }

    @Override
    public void onStopoverClick(Stopover stopover) {
        hapticHelper.vibrateClick();
        Intent intent = new Intent(this, EditStopoverActivity.class);
        intent.putExtra("stopover_id", stopover.getId());
        startActivity(intent);
    }

    @Override
    public void onStopoverLongClick(Stopover stopover) {
        hapticHelper.vibrateClick();
        String userRole = prefsManager.getUser() != null ? prefsManager.getUser().getRole() : "";
        if ("admin".equals(userRole) || "manager".equals(userRole)) {
            showDeleteStopoverConfirmationDialog(stopover.getId());
        }
    }

    private void showDeleteStopoverConfirmationDialog(Integer stopoverId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Stopover")
                .setMessage("Are you sure you want to delete this stopover?")
                .setPositiveButton("Delete", (dialog, which) -> deleteStopover(stopoverId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteStopover(Integer stopoverId) {
        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(this);
            return;
        }

        scheduleRepository.deleteStopover(stopoverId, new ScheduleRepository.ScheduleRepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                CookieBarToastHelper.showSuccess(ScheduleDetailActivity.this, "Success", "Stopover deleted successfully", CookieBarToastHelper.SHORT_DURATION);
                loadSchedule();
            }

            @Override
            public void onError(String message, int errorCode) {
                handleError(errorCode, message);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSchedule();
    }
}
