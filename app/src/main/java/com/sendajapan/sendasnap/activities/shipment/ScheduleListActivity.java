package com.sendajapan.sendasnap.activities.shipment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sendajapan.sendasnap.MyApplication;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.adapters.ScheduleAdapter;
import com.sendajapan.sendasnap.data.repository.ScheduleRepositoryImpl;
import com.sendajapan.sendasnap.databinding.ActivityScheduleListBinding;
import com.sendajapan.sendasnap.domain.repository.ScheduleRepository;
import com.sendajapan.sendasnap.models.shipment.Pagination;
import com.sendajapan.sendasnap.models.shipment.Schedule;
import com.sendajapan.sendasnap.models.shipment.ScheduleListResponse;
import com.sendajapan.sendasnap.networking.NetworkUtils;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

public class ScheduleListActivity extends AppCompatActivity implements ScheduleAdapter.OnScheduleClickListener, ScheduleAdapter.OnScheduleLongClickListener {

    private ActivityScheduleListBinding binding;
    private ScheduleAdapter scheduleAdapter;
    private ScheduleRepository scheduleRepository;
    private HapticFeedbackHelper hapticHelper;
    private NetworkUtils networkUtils;
    private SharedPrefsManager prefsManager;

    private final List<Schedule> schedules = new ArrayList<>();
    private String currentSearch = "";
    private Integer currentCarrierId = null;
    private Integer currentStartPortId = null;
    private Integer currentEndPortId = null;
    private String currentStatus = null;
    private int currentPage = 1;
    private int lastPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityScheduleListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MyApplication.applyWindowInsets(binding.getRoot());

        initHelpers();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFAB();
        loadSchedules();
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

    private void setupRecyclerView() {
        scheduleAdapter = new ScheduleAdapter(schedules, this);
        scheduleAdapter.setOnScheduleLongClickListener(this);
        binding.recyclerViewSchedules.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSchedules.setAdapter(scheduleAdapter);

        binding.recyclerViewSchedules.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreSchedules();
                    }
                }
            }
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentSearch = s.toString().trim();
                resetAndLoadSchedules();
            }
        });
    }

    private void setupFAB() {
        binding.fabAddSchedule.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            Intent intent = new Intent(this, CreateScheduleActivity.class);
            startActivity(intent);
        });
    }

    private void resetAndLoadSchedules() {
        schedules.clear();
        scheduleAdapter.notifyDataSetChanged();
        currentPage = 1;
        isLastPage = false;
        loadSchedules();
    }

    private void loadSchedules() {
        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(this);
            showEmptyState();
            return;
        }

        if (isLoading) {
            return;
        }

        isLoading = true;
        showLoadingState();

        scheduleRepository.getSchedules(
                currentSearch.isEmpty() ? null : currentSearch,
                null,
                null,
                currentCarrierId,
                currentStartPortId,
                currentEndPortId,
                15,
                currentPage,
                new ScheduleRepository.ScheduleRepositoryCallback<ScheduleListResponse>() {
                    @Override
                    public void onSuccess(ScheduleListResponse result) {
                        isLoading = false;
                        hideLoadingState();

                        if (result != null && result.getSchedules() != null) {
                            schedules.addAll(result.getSchedules());
                            scheduleAdapter.notifyDataSetChanged();

                            if (result.getPagination() != null) {
                                Pagination pagination = result.getPagination();
                                lastPage = pagination.getLastPage();
                                isLastPage = currentPage >= lastPage;
                            }

                            updateEmptyState();
                        } else {
                            showEmptyState();
                        }
                    }

                    @Override
                    public void onError(String message, int errorCode) {
                        isLoading = false;
                        hideLoadingState();
                        handleError(errorCode, message);
                        showEmptyState();
                    }
                });
    }

    private void loadMoreSchedules() {
        if (isLastPage || isLoading) {
            return;
        }

        currentPage++;
        loadSchedules();
    }

    private void showLoadingState() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerViewSchedules.setVisibility(View.GONE);
        binding.textEmpty.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        binding.progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        if (schedules.isEmpty()) {
            binding.textEmpty.setVisibility(View.VISIBLE);
            binding.recyclerViewSchedules.setVisibility(View.GONE);
        } else {
            binding.textEmpty.setVisibility(View.GONE);
            binding.recyclerViewSchedules.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        showEmptyState();
    }

    private void handleError(int errorCode, String message) {
        if (errorCode == 401) {
            CookieBarToastHelper.showError(this, "Unauthorized", "Please login again", CookieBarToastHelper.SHORT_DURATION);
        } else if (errorCode == 403) {
            CookieBarToastHelper.showError(this, "Forbidden", "You don't have permission", CookieBarToastHelper.SHORT_DURATION);
        } else if (errorCode == 404) {
            CookieBarToastHelper.showError(this, "Not Found", "Schedules not found", CookieBarToastHelper.SHORT_DURATION);
        } else if (errorCode == 500) {
            CookieBarToastHelper.showError(this, "Server Error", "Server error, please try again", CookieBarToastHelper.SHORT_DURATION);
        } else {
            CookieBarToastHelper.showError(this, "Error", message != null ? message : "An error occurred", CookieBarToastHelper.SHORT_DURATION);
        }
    }

    @Override
    public void onScheduleClick(Schedule schedule) {
        hapticHelper.vibrateClick();
        Intent intent = new Intent(this, ScheduleDetailActivity.class);
        intent.putExtra("schedule_id", schedule.getId());
        startActivity(intent);
    }

    @Override
    public void onScheduleLongClick(Schedule schedule) {
        hapticHelper.vibrateClick();
        String userRole = prefsManager.getUser() != null ? prefsManager.getUser().getRole() : "";
        if ("admin".equals(userRole) || "manager".equals(userRole)) {
            showScheduleOptionsDialog(schedule);
        }
    }

    private void showScheduleOptionsDialog(Schedule schedule) {
        String[] options = {"Edit", "Delete"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(this, EditScheduleActivity.class);
                        intent.putExtra("schedule_id", schedule.getId());
                        startActivity(intent);
                    } else if (which == 1) {
                        showDeleteConfirmationDialog(schedule);
                    }
                })
                .show();
    }

    private void showDeleteConfirmationDialog(Schedule schedule) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Schedule")
                .setMessage("Are you sure you want to delete this schedule?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSchedule(schedule.getId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSchedule(Integer scheduleId) {
        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(this);
            return;
        }

        scheduleRepository.deleteSchedule(scheduleId, new ScheduleRepository.ScheduleRepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                CookieBarToastHelper.showSuccess(ScheduleListActivity.this, "Success", "Schedule deleted successfully", CookieBarToastHelper.SHORT_DURATION);
                resetAndLoadSchedules();
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
        resetAndLoadSchedules();
    }
}
