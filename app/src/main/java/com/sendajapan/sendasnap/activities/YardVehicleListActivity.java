package com.sendajapan.sendasnap.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.sendajapan.sendasnap.MyApplication;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.adapters.YardVehicleAdapter;
import com.sendajapan.sendasnap.data.dto.YardVehicleRequestDto;
import com.sendajapan.sendasnap.data.dto.YardVehicleResponseDto;
import com.sendajapan.sendasnap.databinding.ActivityYardVehicleListBinding;
import com.sendajapan.sendasnap.models.Vehicle;
import com.sendajapan.sendasnap.networking.ApiService;
import com.sendajapan.sendasnap.networking.RetrofitClient;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YardVehicleListActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 20;
    private static final String EXTRA_YARD_ID = "yard_id";
    private static final String EXTRA_COMPANY = "company";
    private static final String EXTRA_YARD_NAME = "yard_name";

    private ActivityYardVehicleListBinding binding;
    private ApiService apiService;
    private HapticFeedbackHelper hapticHelper;
    private YardVehicleAdapter adapter;

    private final List<Vehicle> allVehicles = new ArrayList<>();
    private final List<Vehicle> filteredVehicles = new ArrayList<>();
    private int displayedCount = 0;
    private boolean isLoadingMore = false;

    private String filterMake = null;
    private String filterModel = null;
    private String filterYear = null;
    private String filterChassis = null;

    public static Intent newIntent(Context context, String yardId, String company, String yardName) {
        Intent intent = new Intent(context, YardVehicleListActivity.class);
        intent.putExtra(EXTRA_YARD_ID, yardId);
        intent.putExtra(EXTRA_COMPANY, company);
        intent.putExtra(EXTRA_YARD_NAME, yardName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityYardVehicleListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        MyApplication.applyWindowInsets(binding.getRoot());
        initHelpers();
        setupToolbar();
        setupRecyclerView();
        setupFilterChips();
        fetchVehicles();
    }

    private void initHelpers() {
        apiService = RetrofitClient.getInstance(this).getApiService();
        hapticHelper = HapticFeedbackHelper.getInstance(this);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            String yardName = getIntent().getStringExtra(EXTRA_YARD_NAME);
            getSupportActionBar().setTitle(yardName != null ? yardName : "Yard Vehicles");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new YardVehicleAdapter(vehicle -> {
            hapticHelper.vibrateClick();
            Intent intent = new Intent(this, VehicleDetailsActivity.class);
            intent.putExtra("vehicle", vehicle);
            intent.putExtra("company", getIntent().getStringExtra(EXTRA_COMPANY));
            startActivity(intent);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoadingMore) return;
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                int total = layoutManager.getItemCount();
                if (lastVisible >= total - 4 && displayedCount < filteredVehicles.size()) {
                    loadNextPage();
                }
            }
        });
    }

    private void setupFilterChips() {
        binding.chipMake.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            showListFilterDialog("Make", filterMake, getUniqueMakes(), value -> {
                filterMake = value;
                filterModel = null;
                setChipActive(binding.chipMake, "Make", filterMake);
                setChipActive(binding.chipModel, "Model", null);
                applyFilters();
            });
        });

        binding.chipModel.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            showListFilterDialog("Model", filterModel, getUniqueModels(), value -> {
                filterModel = value;
                setChipActive(binding.chipModel, "Model", filterModel);
                applyFilters();
            });
        });

        binding.chipYear.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            showListFilterDialog("Year", filterYear, getUniqueYears(), value -> {
                filterYear = value;
                setChipActive(binding.chipYear, "Year", filterYear);
                applyFilters();
            });
        });

        binding.chipChassis.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            showChassisFilterDialog();
        });
    }

    private void showListFilterDialog(String title, String currentValue, List<String> options, OnFilterSelected callback) {
        if (options.isEmpty()) {
            CookieBarToastHelper.showError(this, "No options", "No " + title + " values available", CookieBarToastHelper.SHORT_DURATION);
            return;
        }

        List<String> items = new ArrayList<>();
        items.add("All " + title + "s");
        items.addAll(options);

        String[] itemsArray = items.toArray(new String[0]);
        int checkedItem = currentValue == null ? 0 : items.indexOf(currentValue);
        if (checkedItem < 0) checkedItem = 0;

        final int[] selectedIndex = {checkedItem};

        new AlertDialog.Builder(this)
                .setTitle("Filter by " + title)
                .setSingleChoiceItems(itemsArray, checkedItem, (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String selected = selectedIndex[0] == 0 ? null : itemsArray[selectedIndex[0]];
                    callback.onSelected(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChassisFilterDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter chassis number");
        input.setText(filterChassis != null ? filterChassis : "");
        input.setSingleLine(true);

        int padding = (int) getResources().getDimension(R.dimen.dimen_16);
        input.setPadding(padding, padding, padding, padding);

        input.post(() -> {
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });

        new AlertDialog.Builder(this)
                .setTitle("Filter by Chassis")
                .setView(input)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    filterChassis = text.isEmpty() ? null : text;
                    setChipActive(binding.chipChassis, "Chassis", filterChassis);
                    applyFilters();
                })
                .setNegativeButton("Clear", (dialog, which) -> {
                    filterChassis = null;
                    setChipActive(binding.chipChassis, "Chassis", null);
                    applyFilters();
                })
                .show();
    }

    private void applyFilters() {
        filteredVehicles.clear();
        for (Vehicle v : allVehicles) {
            if (filterMake != null && !filterMake.equalsIgnoreCase(v.getMake())) continue;
            if (filterModel != null && !filterModel.equalsIgnoreCase(v.getModel())) continue;
            if (filterYear != null && !filterYear.equals(v.getYear())) continue;
            if (!TextUtils.isEmpty(filterChassis)) {
                String chassis = v.getSerialNumber();
                if (chassis == null || !chassis.toLowerCase().contains(filterChassis.toLowerCase())) continue;
            }
            filteredVehicles.add(v);
        }
        resetAndReload();
    }

    private void resetAndReload() {
        displayedCount = 0;
        adapter.clearVehicles();
        updateToolbarSubtitle();
        if (filteredVehicles.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            loadNextPage();
        }
    }

    private void fetchVehicles() {
        String yardId = getIntent().getStringExtra(EXTRA_YARD_ID);
        String company = getIntent().getStringExtra(EXTRA_COMPANY);

        if (yardId == null || company == null) {
            showEmpty(true);
            return;
        }

        showLoading(true);

        int yardIdInt;
        try {
            yardIdInt = Integer.parseInt(yardId);
        } catch (NumberFormatException e) {
            showLoading(false);
            CookieBarToastHelper.showError(this, "Error", "Invalid yard ID", CookieBarToastHelper.SHORT_DURATION);
            return;
        }

        YardVehicleRequestDto request = new YardVehicleRequestDto(yardIdInt, company);
        apiService.getYardVehicles(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<YardVehicleResponseDto> call, @NonNull Response<YardVehicleResponseDto> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    CookieBarToastHelper.showError(YardVehicleListActivity.this, "Error", "Failed to load yard vehicles", CookieBarToastHelper.SHORT_DURATION);
                    showEmpty(true);
                    return;
                }
                YardVehicleResponseDto body = response.body();
                if (body.getData() == null || body.getData().getVehicles().isEmpty()) {
                    showEmpty(true);
                    return;
                }
                for (YardVehicleResponseDto.YardVehicleItemDto dto : body.getData().getVehicles()) {
                    allVehicles.add(dto.toVehicle());
                }
                filteredVehicles.addAll(allVehicles);
                updateToolbarSubtitle();
                loadNextPage();
            }

            @Override
            public void onFailure(@NonNull Call<YardVehicleResponseDto> call, @NonNull Throwable t) {
                showLoading(false);
                CookieBarToastHelper.showError(YardVehicleListActivity.this, "Error", "Network error. Please try again.", CookieBarToastHelper.SHORT_DURATION);
                showEmpty(true);
            }
        });
    }

    private void loadNextPage() {
        if (displayedCount >= filteredVehicles.size()) return;

        isLoadingMore = true;
        adapter.showFooter(true);

        int from = displayedCount;
        int to = Math.min(displayedCount + PAGE_SIZE, filteredVehicles.size());
        List<Vehicle> batch = new ArrayList<>(filteredVehicles.subList(from, to));
        displayedCount = to;

        binding.recyclerView.post(() -> {
            adapter.showFooter(false);
            adapter.addVehicles(batch);
            isLoadingMore = false;
        });
    }

    private List<String> getUniqueMakes() {
        Set<String> set = new LinkedHashSet<>();
        for (Vehicle v : allVehicles) {
            if (!TextUtils.isEmpty(v.getMake())) set.add(v.getMake());
        }
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    private List<String> getUniqueModels() {
        Set<String> set = new LinkedHashSet<>();
        for (Vehicle v : allVehicles) {
            if (filterMake != null && !filterMake.equalsIgnoreCase(v.getMake())) continue;
            if (!TextUtils.isEmpty(v.getModel())) set.add(v.getModel());
        }
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    private List<String> getUniqueYears() {
        Set<String> set = new LinkedHashSet<>();
        for (Vehicle v : allVehicles) {
            if (!TextUtils.isEmpty(v.getYear())) set.add(v.getYear());
        }
        List<String> list = new ArrayList<>(set);
        Collections.sort(list, Collections.reverseOrder());
        return list;
    }

    private void setChipActive(Chip chip, String defaultLabel, String value) {
        if (value != null) {
            chip.setText(value);
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_light)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        } else {
            chip.setText(defaultLabel);
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_black));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        }
    }

    private void updateToolbarSubtitle() {
        if (getSupportActionBar() != null) {
            int total = allVehicles.size();
            int shown = filteredVehicles.size();
            if (shown == total) {
                getSupportActionBar().setSubtitle(total + " vehicles");
            } else {
                getSupportActionBar().setSubtitle(shown + " of " + total + " vehicles");
            }
        }
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        binding.scrollFilterChips.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showEmpty(boolean empty) {
        binding.txtEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private interface OnFilterSelected {
        void onSelected(String value);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
