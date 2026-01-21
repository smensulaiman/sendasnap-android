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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sendajapan.sendasnap.MyApplication;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.databinding.ActivityCreateScheduleBinding;
import com.sendajapan.sendasnap.models.shipment.CreateScheduleRequest;
import com.sendajapan.sendasnap.models.shipment.Port;
import com.sendajapan.sendasnap.models.shipment.Schedule;
import com.sendajapan.sendasnap.models.shipment.ShippingCompany;
import com.sendajapan.sendasnap.networking.NetworkUtils;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.DateFormatter;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.ScheduleCache;
import com.sendajapan.sendasnap.viewmodel.ScheduleViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CreateScheduleActivity extends AppCompatActivity {

    private ActivityCreateScheduleBinding binding;
    private ScheduleViewModel scheduleViewModel;
    private HapticFeedbackHelper hapticHelper;
    private NetworkUtils networkUtils;
    private ScheduleCache scheduleCache;

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

        initHelpers();
        setupToolbar();
        setupClickListeners();
        setupStatusDropdown();
        loadPortsAndCarriers();
    }

    private void initHelpers() {
        scheduleViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ScheduleViewModel.class);
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
    }

    private void setupStatusDropdown() {
        String[] statuses = { "Waiting", "In Transit", "Arrived", "Completed", "Cancelled" };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                statuses);
        binding.actvStatus.setAdapter(statusAdapter);
        binding.actvStatus.setText("Waiting", false);
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

        scheduleViewModel.getPorts(null, "Local Port", 1000, 1,
                new ScheduleViewModel.ScheduleCallback<List<Port>>() {
                    @Override
                    public void onSuccess(List<Port> result) {
                        ports = result;
                        scheduleCache.savePorts(ports);
                        filterLocalPorts();
                        setupPortDropdowns();
                    }

                    @Override
                    public void onError(String message, int errorCode) {
                        CookieBarToastHelper.showError(CreateScheduleActivity.this, "Error", "Failed to load ports",
                                CookieBarToastHelper.SHORT_DURATION);
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

        scheduleViewModel.getShippingCompanies(null, "Active", 1000, 1,
                new ScheduleViewModel.ScheduleCallback<List<ShippingCompany>>() {
                    @Override
                    public void onSuccess(List<ShippingCompany> result) {
                        shippingCompanies = result;
                        scheduleCache.saveShippingCompanies(shippingCompanies);
                        setupCarrierDropdowns();
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
        ArrayAdapter<String> portAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                portNames);
        binding.actvStartPort.setAdapter(portAdapter);
        binding.actvEndPort.setAdapter(portAdapter);
    }

    private void setupCarrierDropdowns() {
        List<String> carrierNames = new ArrayList<>();
        carrierNames.add("None");
        for (ShippingCompany company : shippingCompanies) {
            carrierNames.add(company.getLineName());
        }
        ArrayAdapter<String> carrierAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                carrierNames);
        binding.actvCarrier1.setAdapter(carrierAdapter);
        binding.actvCarrier2.setAdapter(carrierAdapter);
        binding.actvCarrier3.setAdapter(carrierAdapter);
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
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void submitSchedule() {
        if (!validateForm()) {
            return;
        }

        // Build the request first, but do not call the API
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setVesselName(binding.etVesselName.getText().toString().trim());
        request.setVoyageNo(binding.etVoyageNo.getText().toString().trim());

        String carrier1Text = binding.actvCarrier1.getText().toString();
        if (!carrier1Text.isEmpty() && !"None".equals(carrier1Text)) {
            request.setCarrier1Id(findCarrierId(carrier1Text));
        }

        String carrier2Text = binding.actvCarrier2.getText().toString();
        if (!carrier2Text.isEmpty() && !"None".equals(carrier2Text)) {
            request.setCarrier2Id(findCarrierId(carrier2Text));
        }

        String carrier3Text = binding.actvCarrier3.getText().toString();
        if (!carrier3Text.isEmpty() && !"None".equals(carrier3Text)) {
            request.setCarrier3Id(findCarrierId(carrier3Text));
        }

        request.setStartPortId(findPortId(binding.actvStartPort.getText().toString()));
        request.setEndPortId(findPortId(binding.actvEndPort.getText().toString()));
        request.setEta(binding.etEta.getText().toString().trim());

        String status = binding.actvStatus.getText().toString();
        if (!status.isEmpty()) {
            request.setStatus(status);
        }

        String comment = binding.etComment.getText().toString().trim();
        if (!comment.isEmpty()) {
            request.setComment(comment);
        }
        // Show confirmation dialog; actual creation happens after user chooses an
        // option
        showCreateScheduleConfirmDialog(request);
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

    private void showCreateScheduleConfirmDialog(CreateScheduleRequest request) {
        android.app.Dialog dialog = new android.app.Dialog(this, R.style.DialogTheme);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_stopover);
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

        android.widget.TextView txtDialogTitle = dialog.findViewById(R.id.txtDialogTitle);
        android.widget.TextView txtDialogMessage = dialog.findViewById(R.id.txtDialogMessage);

        if (txtDialogTitle != null) {
            txtDialogTitle.setText("Create Shipment Schedule");
        }
        if (txtDialogMessage != null) {
            txtDialogMessage.setText("How would you like to create this shipment schedule?");
        }

        com.google.android.material.button.MaterialButton btnCreateScheduleOnly = dialog
                .findViewById(R.id.btnCreateScheduleOnly);
        com.google.android.material.button.MaterialButton btnCreateStopover = dialog
                .findViewById(R.id.btnCreateStopover);

        if (btnCreateScheduleOnly != null) {
            btnCreateScheduleOnly.setText("Create Schedule Only");
            btnCreateScheduleOnly.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                dialog.dismiss();
                performCreateSchedule(request, false);
            });
        }

        if (btnCreateStopover != null) {
            btnCreateStopover.setText("Create & Add Stopover");
            btnCreateStopover.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                dialog.dismiss();
                performCreateSchedule(request, true);
            });
        }

        dialog.show();
    }

    private void performCreateSchedule(CreateScheduleRequest request, boolean createStopoverAfter) {
        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(this);
            return;
        }

        binding.btnSubmit.setEnabled(false);
        binding.btnSubmit.setText("Creating...");

        scheduleViewModel.createSchedule(request, new ScheduleViewModel.ScheduleCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                binding.btnSubmit.setEnabled(true);
                binding.btnSubmit.setText("Create Schedule");

                CookieBarToastHelper.showSuccess(CreateScheduleActivity.this, "Success",
                        "Schedule created successfully", CookieBarToastHelper.SHORT_DURATION);

                if (createStopoverAfter) {
                    Intent intent = new Intent(CreateScheduleActivity.this, CreateStopoverActivity.class);
                    intent.putExtra("schedule_id", result.getId());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(CreateScheduleActivity.this, ScheduleDetailActivity.class);
                    intent.putExtra("schedule_id", result.getId());
                    startActivity(intent);
                }
                finish();
            }

            @Override
            public void onError(String message, int errorCode) {
                binding.btnSubmit.setEnabled(true);
                binding.btnSubmit.setText("Create Schedule");
                handleError(errorCode, message);
            }
        });
    }

    private void handleError(int errorCode, String message) {
        if (errorCode == 422) {
            CookieBarToastHelper.showError(this, "Validation Error",
                    message != null ? message : "Please check your input", CookieBarToastHelper.SHORT_DURATION);
        } else if (errorCode == 500) {
            CookieBarToastHelper.showError(this, "Server Error", "Server error, please try again",
                    CookieBarToastHelper.SHORT_DURATION);
        } else {
            CookieBarToastHelper.showError(this, "Error", message != null ? message : "Failed to create schedule",
                    CookieBarToastHelper.SHORT_DURATION);
        }
    }
}
