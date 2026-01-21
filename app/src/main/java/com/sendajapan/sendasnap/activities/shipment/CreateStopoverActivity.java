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
import com.sendajapan.sendasnap.databinding.ActivityCreateStopoverBinding;
import com.sendajapan.sendasnap.domain.repository.ScheduleRepository;
import com.sendajapan.sendasnap.models.shipment.CreateStopoverRequest;
import com.sendajapan.sendasnap.models.shipment.Port;
import com.sendajapan.sendasnap.models.shipment.Stopover;

import java.util.Date;
import com.sendajapan.sendasnap.networking.NetworkUtils;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.DateFormatter;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.ScheduleCache;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CreateStopoverActivity extends AppCompatActivity {

    private ActivityCreateStopoverBinding binding;
    private ScheduleRepository scheduleRepository;
    private HapticFeedbackHelper hapticHelper;
    private NetworkUtils networkUtils;
    private ScheduleCache scheduleCache;

    private Integer scheduleId;
    private List<Port> ports = new ArrayList<>();
    private Calendar arrivalCalendar = Calendar.getInstance();
    private Calendar departureCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCreateStopoverBinding.inflate(getLayoutInflater());
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
        loadPorts();
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
        binding.toolbar.setNavigationOnClickListener(v -> {
            hapticHelper.vibrateClick();
            finish();
        });
    }

    private void setupClickListeners() {
        binding.etArrival.setOnClickListener(v -> showDatePicker(binding.etArrival, arrivalCalendar));
        binding.etDeparture.setOnClickListener(v -> showDatePicker(binding.etDeparture, departureCalendar));
        binding.btnSaveStopover.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            submitStopover(false);
        });
        binding.btnSaveAndAddNext.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            submitStopover(true);
        });
    }

    private void loadPorts() {
        if (scheduleCache.isPortsCacheValid()) {
            ports = scheduleCache.getPorts();
            setupPortDropdown();
        } else {
            if (!networkUtils.isNetworkAvailable()) {
                CookieBarToastHelper.showNoInternet(this);
                return;
            }

            scheduleRepository.getPorts(null, null, 1000, 1, new ScheduleRepository.ScheduleRepositoryCallback<List<Port>>() {
                @Override
                public void onSuccess(List<Port> result) {
                    ports = result;
                    scheduleCache.savePorts(ports);
                    setupPortDropdown();
                }

                @Override
                public void onError(String message, int errorCode) {
                    CookieBarToastHelper.showError(CreateStopoverActivity.this, "Error", "Failed to load ports", CookieBarToastHelper.SHORT_DURATION);
                }
            });
        }
    }

    private void setupPortDropdown() {
        List<String> portNames = new ArrayList<>();
        for (Port port : ports) {
            String portName = port.getPortName();
            if (port.getPortType() != null) {
                portName += " (" + port.getPortType() + ")";
            }
            portNames.add(portName);
        }
        ArrayAdapter<String> portAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, portNames);
        binding.actvPort.setAdapter(portAdapter);
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

    private void submitStopover(boolean addAnother) {
        if (!validateForm()) {
            return;
        }

        if (!networkUtils.isNetworkAvailable()) {
            CookieBarToastHelper.showNoInternet(this);
            return;
        }

        CreateStopoverRequest request = new CreateStopoverRequest();
        request.setPortId(findPortId(binding.actvPort.getText().toString()));

        String arrivalText = binding.etArrival.getText().toString().trim();
        if (!arrivalText.isEmpty()) {
            request.setStopoverEta(arrivalText);
        }

        String departureText = binding.etDeparture.getText().toString().trim();
        if (!departureText.isEmpty()) {
            request.setStopoverEtd(departureText);
        }

        setButtonsEnabled(false);
        if (addAnother) {
            binding.btnSaveAndAddNext.setText("Saving...");
        } else {
            binding.btnSaveStopover.setText("Saving...");
        }

        scheduleRepository.createStopover(scheduleId, request, new ScheduleRepository.ScheduleRepositoryCallback<Stopover>() {
            @Override
            public void onSuccess(Stopover result) {
                setButtonsEnabled(true);
                binding.btnSaveStopover.setText("Save Stopover");
                binding.btnSaveAndAddNext.setText("Save & Add Next");

                CookieBarToastHelper.showSuccess(CreateStopoverActivity.this, "Success", "Stopover saved successfully", CookieBarToastHelper.SHORT_DURATION);

                if (addAnother) {
                    clearForm();
                } else {
                    setResult(RESULT_OK);
                    finish();
                }
            }

            @Override
            public void onError(String message, int errorCode) {
                setButtonsEnabled(true);
                binding.btnSaveStopover.setText("Save Stopover");
                binding.btnSaveAndAddNext.setText("Save & Add Next");
                handleError(errorCode, message);
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.btnSaveStopover.setEnabled(enabled);
        binding.btnSaveAndAddNext.setEnabled(enabled);
    }

    private void clearForm() {
        binding.actvPort.setText("", false);
        binding.etArrival.setText("");
        binding.etDeparture.setText("");

        arrivalCalendar = Calendar.getInstance();
        departureCalendar = Calendar.getInstance();
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (TextUtils.isEmpty(binding.actvPort.getText())) {
            binding.tilPort.setError("Port is required");
            isValid = false;
        } else {
            binding.tilPort.setError(null);
        }

        String arrivalText = binding.etArrival.getText().toString().trim();
        String departureText = binding.etDeparture.getText().toString().trim();

        if (!arrivalText.isEmpty() && !departureText.isEmpty()) {
            Date arrivalDate = DateFormatter.parseApiDate(arrivalText);
            Date departureDate = DateFormatter.parseApiDate(departureText);
            
            if (arrivalDate != null && departureDate != null) {
                Calendar arrival = Calendar.getInstance();
                Calendar departure = Calendar.getInstance();
                arrival.setTime(arrivalDate);
                departure.setTime(departureDate);

                if (departure.before(arrival)) {
                    binding.tilDeparture.setError("Departure must be after arrival");
                    isValid = false;
                } else {
                    binding.tilDeparture.setError(null);
                }
            }
        }

        return isValid;
    }

    private Integer findPortId(String portNameWithType) {
        String portName = portNameWithType.split(" \\(")[0];
        for (Port port : ports) {
            if (port.getPortName().equals(portName)) {
                return port.getId();
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
            CookieBarToastHelper.showError(this, "Error", message != null ? message : "Failed to create stopover", CookieBarToastHelper.SHORT_DURATION);
        }
    }
}
