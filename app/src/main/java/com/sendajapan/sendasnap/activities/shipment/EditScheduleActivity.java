package com.sendajapan.sendasnap.activities.shipment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sendajapan.sendasnap.MyApplication;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.data.repository.ScheduleRepositoryImpl;
import com.sendajapan.sendasnap.databinding.ActivityCreateScheduleBinding;
import com.sendajapan.sendasnap.domain.repository.ScheduleRepository;
import com.sendajapan.sendasnap.models.shipment.Port;
import com.sendajapan.sendasnap.models.shipment.Schedule;
import com.sendajapan.sendasnap.models.shipment.ShippingCompany;
import com.sendajapan.sendasnap.models.shipment.UpdateScheduleRequest;
import com.sendajapan.sendasnap.networking.NetworkUtils;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.DateFormatter;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.ScheduleCache;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EditScheduleActivity extends AppCompatActivity {

    private ActivityCreateScheduleBinding binding;
    private ScheduleRepository scheduleRepository;
    private HapticFeedbackHelper hapticHelper;
    private NetworkUtils networkUtils;
    private ScheduleCache scheduleCache;

    private Schedule schedule;
    private Integer scheduleId;
    private List<Port> ports = new ArrayList<>();
    private List<ShippingCompany> shippingCompanies = new ArrayList<>();
    private List<Port> localPorts = new ArrayList<>();
    private Calendar etaCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCreateScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MyApplication.applyWindowInsets(binding.getRoot());

        scheduleId = getIntent().getIntExtra("schedule_id", -1);
        if (scheduleId == -1) {
            finish();
            return;
        }

        initHelpers();
        setupToolbar();
        setupClickListeners();
        setupStatusDropdown();
        loadPortsAndCarriers();
        loadSchedule();
    }

    private void initHelpers() {
        scheduleRepository = new ScheduleRepositoryImpl(this);
        hapticHelper = HapticFeedbackHelper.getInstance(this);
        networkUtils = NetworkUtils.getInstance(this);
        scheduleCache = new ScheduleCache(this);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setTitle("Edit Schedule");
        binding.toolbar.setNavigationOnClickListener(v -> {
            hapticHelper.vibrateClick();
            finish();
        });
    }

    private void setupClickListeners() {
        binding.etEta.setOnClickListener(v -> showDatePicker(binding.etEta, etaCalendar));
        binding.btnCancel.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            finish();
        });
        binding.btnSubmit.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            submitSchedule();
        });
        binding.btnSubmit.setText("Update Schedule");
    }

    private void setupStatusDropdown() {
        String[] statuses = {"Waiting", "In Transit", "Arrived", "Completed", "Cancelled"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statuses);
        binding.actvStatus.setAdapter(statusAdapter);
    }

    private void loadPortsAndCarriers() {
        if (scheduleCache.isPortsCacheValid()) {
            ports = scheduleCache.getPorts();
            filterLocalPorts();
            setupPortDropdowns();
        } else {
            loadPorts();
        }

        if (scheduleCache.isShippingCompaniesCacheValid()) {
            shippingCompanies = scheduleCache.getShippingCompanies();
            setupCarrierDropdowns();
        } else {
            loadShippingCompanies();
        }
    }

    private void loadPorts() {
        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(this);
            return;
        }

        scheduleRepository.getPorts(null, "Local Port", 1000, 1, new ScheduleRepository.ScheduleRepositoryCallback<List<Port>>() {
            @Override
            public void onSuccess(List<Port> result) {
                ports = result;
                scheduleCache.savePorts(ports);
                filterLocalPorts();
                setupPortDropdowns();
                populateForm();
            }

            @Override
            public void onError(String message, int errorCode) {
                CookieBarToastHelper.showError(EditScheduleActivity.this, "Error", "Failed to load ports", CookieBarToastHelper.SHORT_DURATION);
            }
        });
    }

    private void filterLocalPorts() {
        localPorts.clear();
        for (Port port : ports) {
            if ("Local Port".equals(port.getPortType())) {
                localPorts.add(port);
            }
        }
    }

    private void loadShippingCompanies() {
        if (!networkUtils.isNetworkAvailable()) {
            return;
        }

        scheduleRepository.getShippingCompanies(null, "Active", 1000, 1, new ScheduleRepository.ScheduleRepositoryCallback<List<ShippingCompany>>() {
            @Override
            public void onSuccess(List<ShippingCompany> result) {
                shippingCompanies = result;
                scheduleCache.saveShippingCompanies(shippingCompanies);
                setupCarrierDropdowns();
                populateForm();
            }

            @Override
            public void onError(String message, int errorCode) {
            }
        });
    }

    private void setupPortDropdowns() {
        List<String> portNames = new ArrayList<>();
        for (Port port : localPorts) {
            portNames.add(port.getPortName());
        }
        ArrayAdapter<String> portAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, portNames);
        binding.actvStartPort.setAdapter(portAdapter);
        binding.actvEndPort.setAdapter(portAdapter);
    }

    private void setupCarrierDropdowns() {
        List<String> carrierNames = new ArrayList<>();
        carrierNames.add("None");
        for (ShippingCompany company : shippingCompanies) {
            carrierNames.add(company.getLineName());
        }
        ArrayAdapter<String> carrierAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, carrierNames);
        binding.actvCarrier1.setAdapter(carrierAdapter);
        binding.actvCarrier2.setAdapter(carrierAdapter);
        binding.actvCarrier3.setAdapter(carrierAdapter);
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
                populateForm();
            }

            @Override
            public void onError(String message, int errorCode) {
                handleError(errorCode, message);
            }
        });
    }

    private void populateForm() {
        if (schedule == null || localPorts.isEmpty() || shippingCompanies.isEmpty()) {
            return;
        }

        binding.etVesselName.setText(schedule.getVesselName());
        binding.etVoyageNo.setText(schedule.getVoyageNo());

        if (schedule.getCarrier1() != null) {
            binding.actvCarrier1.setText(schedule.getCarrier1().getLineName(), false);
        }

        if (schedule.getCarrier2() != null) {
            binding.actvCarrier2.setText(schedule.getCarrier2().getLineName(), false);
        }

        if (schedule.getCarrier3() != null) {
            binding.actvCarrier3.setText(schedule.getCarrier3().getLineName(), false);
        }

        if (schedule.getStartPort() != null) {
            binding.actvStartPort.setText(schedule.getStartPort().getPortName(), false);
        }

        if (schedule.getEndPort() != null) {
            binding.actvEndPort.setText(schedule.getEndPort().getPortName(), false);
        }

        if (schedule.getEta() != null) {
            binding.etEta.setText(schedule.getEta());
            Date etaDate = DateFormatter.parseApiDate(schedule.getEta());
            if (etaDate != null) {
                etaCalendar.setTime(etaDate);
            }
        }

        if (schedule.getStatus() != null) {
            binding.actvStatus.setText(schedule.getStatus(), false);
        }

        if (schedule.getComment() != null) {
            binding.etComment.setText(schedule.getComment());
        }
    }

    private void showDatePicker(TextInputEditText editText, Calendar calendar) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    editText.setText(DateFormatter.formatDateForApi(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void submitSchedule() {
        if (!validateForm()) {
            return;
        }

        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(this);
            return;
        }

        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setVesselName(binding.etVesselName.getText().toString().trim());
        request.setVoyageNo(binding.etVoyageNo.getText().toString().trim());

        String carrier1Text = binding.actvCarrier1.getText().toString();
        if (!carrier1Text.isEmpty() && !"None".equals(carrier1Text)) {
            request.setCarrier1Id(findCarrierId(carrier1Text));
        } else {
            request.setCarrier1Id(null);
        }

        String carrier2Text = binding.actvCarrier2.getText().toString();
        if (!carrier2Text.isEmpty() && !"None".equals(carrier2Text)) {
            request.setCarrier2Id(findCarrierId(carrier2Text));
        } else {
            request.setCarrier2Id(null);
        }

        String carrier3Text = binding.actvCarrier3.getText().toString();
        if (!carrier3Text.isEmpty() && !"None".equals(carrier3Text)) {
            request.setCarrier3Id(findCarrierId(carrier3Text));
        } else {
            request.setCarrier3Id(null);
        }

        request.setStartPortId(findPortId(binding.actvStartPort.getText().toString()));
        request.setEndPortId(findPortId(binding.actvEndPort.getText().toString()));
        request.setEta(binding.etEta.getText().toString().trim());

        String status = binding.actvStatus.getText().toString();
        if (!status.isEmpty()) {
            request.setStatus(status);
        }

        String comment = binding.etComment.getText().toString().trim();
        request.setComment(comment.isEmpty() ? null : comment);

        binding.btnSubmit.setEnabled(false);
        binding.btnSubmit.setText("Updating...");

        scheduleRepository.updateSchedule(scheduleId, request, new ScheduleRepository.ScheduleRepositoryCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                binding.btnSubmit.setEnabled(true);
                binding.btnSubmit.setText("Update Schedule");
                CookieBarToastHelper.showSuccess(EditScheduleActivity.this, "Success", "Schedule updated successfully", CookieBarToastHelper.SHORT_DURATION);
                Intent intent = new Intent(EditScheduleActivity.this, ScheduleDetailActivity.class);
                intent.putExtra("schedule_id", result.getId());
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message, int errorCode) {
                binding.btnSubmit.setEnabled(true);
                binding.btnSubmit.setText("Update Schedule");
                handleError(errorCode, message);
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (TextUtils.isEmpty(binding.etVesselName.getText())) {
            binding.tilVesselName.setError("Vessel name is required");
            isValid = false;
        } else {
            binding.tilVesselName.setError(null);
        }

        if (TextUtils.isEmpty(binding.etVoyageNo.getText())) {
            binding.tilVoyageNo.setError("Voyage number is required");
            isValid = false;
        } else {
            binding.tilVoyageNo.setError(null);
        }

        if (TextUtils.isEmpty(binding.actvStartPort.getText())) {
            binding.tilStartPort.setError("Start port is required");
            isValid = false;
        } else {
            binding.tilStartPort.setError(null);
        }

        if (TextUtils.isEmpty(binding.actvEndPort.getText())) {
            binding.tilEndPort.setError("End port is required");
            isValid = false;
        } else {
            binding.tilEndPort.setError(null);
        }

        if (TextUtils.isEmpty(binding.etEta.getText())) {
            binding.tilEta.setError("ETA is required");
            isValid = false;
        } else {
            binding.tilEta.setError(null);
        }

        return isValid;
    }

    private Integer findPortId(String portName) {
        for (Port port : localPorts) {
            if (port.getPortName().equals(portName)) {
                return port.getId();
            }
        }
        return null;
    }

    private Integer findCarrierId(String carrierName) {
        for (ShippingCompany company : shippingCompanies) {
            if (company.getLineName().equals(carrierName)) {
                return company.getId();
            }
        }
        return null;
    }

    private void handleError(int errorCode, String message) {
        if (errorCode == 422) {
            CookieBarToastHelper.showError(this, "Validation Error", message != null ? message : "Please check your input", CookieBarToastHelper.SHORT_DURATION);
        } else if (errorCode == 500) {
            CookieBarToastHelper.showError(this, "Server Error", "Server error, please try again", CookieBarToastHelper.SHORT_DURATION);
        } else {
            CookieBarToastHelper.showError(this, "Error", message != null ? message : "Failed to update schedule", CookieBarToastHelper.SHORT_DURATION);
        }
    }
}
