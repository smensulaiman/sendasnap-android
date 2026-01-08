package com.sendajapan.sendasnap.activities.schedule;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.sendajapan.sendasnap.MyApplication;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.adapters.TaskAttachmentAdapter;
import com.sendajapan.sendasnap.adapters.UserDropdownAdapter;
import com.sendajapan.sendasnap.databinding.ActivityAddTaskBinding;
import com.sendajapan.sendasnap.models.Task;
import com.sendajapan.sendasnap.models.TaskAttachment;
import com.sendajapan.sendasnap.models.UserData;
import com.sendajapan.sendasnap.utils.AlarmHelper;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.FcmNotificationSender;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;
import com.sendajapan.sendasnap.utils.SoundHelper;
import com.sendajapan.sendasnap.utils.TaskNotificationHelper;
import com.sendajapan.sendasnap.viewmodel.TaskViewModel;
import com.sendajapan.sendasnap.viewmodel.UserViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AddScheduleActivity extends AppCompatActivity {

    private static final int FILE_PICKER_REQUEST_CODE = 1001;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB for files
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB for images

    private ActivityAddTaskBinding binding;

    private HapticFeedbackHelper hapticHelper;
    private SharedPrefsManager prefsManager;
    private TaskViewModel taskViewModel;
    private UserViewModel userViewModel;

    private PopupWindow userDropdownPopup;
    private TaskAttachmentAdapter attachmentAdapter;
    private UserDropdownAdapter userDropdownAdapter;

    private Task editingTask;
    private UserData currentUser;

    private boolean isEditMode = false;
    private boolean isPreventingPriorityDeselection = false;
    private boolean isPreventingStatusDeselection = false;
    private boolean isRestrictedEditMode = false;
    private boolean isShowingDropdown = false;
    private boolean replaceAttachments = false;

    private int lastCheckedPriorityChipId = -1;
    private int lastCheckedStatusChipId = -1;

    private final Calendar selectedDate = Calendar.getInstance();
    private final Calendar selectedTime = Calendar.getInstance();
    private final List<TaskAttachment> attachments = new ArrayList<>();
    private final List<UserData> selectedAssignees = new ArrayList<>();
    private final List<UserData> users = new ArrayList<>();
    private final List<Uri> attachmentUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MyApplication.applyWindowInsets(binding.getRoot());

        initHelpers();
        setupToolbar();
        checkEditMode();
        checkUserRole();
        setupRecyclerView();
        setupListeners();
        setupInitialValues();
        setupChipSelectionPrevention();
    }

    private void initHelpers() {
        hapticHelper = HapticFeedbackHelper.getInstance(this);
        prefsManager = SharedPrefsManager.getInstance(this);
        currentUser = prefsManager.getUser();
        taskViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(TaskViewModel.class);
        userViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(UserViewModel.class);
    }

    private void checkUserRole() {
        if (currentUser != null && isEditMode) {
            String role = currentUser.getRole();
            if (role == null || (!role.equalsIgnoreCase("admin") && !role.equalsIgnoreCase("manager"))) {
                isRestrictedEditMode = true;
                disableFieldsExceptStatus();
            }
        }
    }

    private void checkEditMode() {
        editingTask = (Task) getIntent().getSerializableExtra("task");
        if (editingTask != null) {
            isEditMode = true;
            binding.toolbar.setTitle("Edit Schedule");
            binding.buttonSave.setText("Update");
        }
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
        if (isEditMode) {
            getMenuInflater().inflate(R.menu.menu_add_schedule, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save) {
            hapticHelper.vibrateClick();
            saveTask();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        attachmentAdapter = new TaskAttachmentAdapter(attachments, true);
        attachmentAdapter.setContext(this);
        attachmentAdapter.setOnAttachmentRemoveListener((position, attachment) -> {
            hapticHelper.vibrateClick();
            if (position >= 0 && position < attachments.size()) {
                TaskAttachment removedAttachment = attachments.remove(position);
                // Also remove from attachmentUris if it exists
                if (removedAttachment != null && removedAttachment.getFileUrl() != null) {
                    String fileUrl = removedAttachment.getFileUrl();
                    attachmentUris.removeIf(uri -> uri.toString().equals(fileUrl));
                }
                attachmentAdapter.notifyItemRemoved(position);
                attachmentAdapter.notifyItemRangeChanged(position, attachments.size());
                updateFileDisplay();
            }
        });
        setAttachmentActionListener();
        binding.recyclerViewAttachments.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewAttachments.setAdapter(attachmentAdapter);
        setAttachmentActionListener();
    }

    private void setAttachmentActionListener() {
        if (attachmentAdapter != null) {
            attachmentAdapter.setOnAttachmentActionListener((position, attachment) -> {
                hapticHelper.vibrateClick();
                openAttachment(attachment);
            });
        }
    }

    private void setupListeners() {
        binding.editTextDate.setOnClickListener(v -> showDatePicker());
        binding.editTextTime.setOnClickListener(v -> showTimePicker());

        binding.editTextAssignee.setOnClickListener(v -> {
            if (!isShowingDropdown) {
                hapticHelper.vibrateClick();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                showUserDropdown();
            }
        });

        binding.editTextAssignee.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !isShowingDropdown) {
                hapticHelper.vibrateClick();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                v.post(() -> showUserDropdown());
            }
        });

        binding.buttonCancel.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            finish();
        });

        binding.buttonSave.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            saveTask();
        });

        binding.buttonAddFile.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            showFilePicker();
        });
    }

    private void setupInitialValues() {
        if (isEditMode && editingTask != null) {
            populateFieldsForEdit();
        } else {
            setDefaultValues();
        }

        fetchUsers();

        binding.editTextTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                hapticHelper.vibrateClick();
        });

        binding.editTextDescription.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                hapticHelper.vibrateClick();
        });
    }

    private void fetchUsers() {
        userViewModel.listUsers(new UserViewModel.UserCallback<List<UserData>>() {
            @Override
            public void onSuccess(List<UserData> userList) {
                users.clear();
                users.addAll(userList);

                if (isEditMode && editingTask != null && userDropdownAdapter != null) {
                    List<String> selectedNames = new ArrayList<>();
                    for (UserData user : selectedAssignees) {
                        if (user != null && user.getName() != null) {
                            selectedNames.add(user.getName());
                        }
                    }
                    userDropdownAdapter.setSelectedUsers(selectedNames);
                }
            }

            @Override
            public void onError(String message, int errorCode) {
                Toast.makeText(AddScheduleActivity.this,
                        getErrorMessage("Failed to load users: " + message, errorCode),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserDropdown() {
        if (users.isEmpty()) {
            Toast.makeText(this, "No users available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userDropdownPopup != null && userDropdownPopup.isShowing()) {
            userDropdownPopup.dismiss();
            isShowingDropdown = false;
            return;
        }

        isShowingDropdown = true;

        LayoutInflater inflater = LayoutInflater.from(AddScheduleActivity.this);
        View popupView = inflater.inflate(R.layout.dialog_user_dropdown, null);

        RecyclerView recyclerView = popupView.findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(AddScheduleActivity.this));

        userDropdownAdapter = new UserDropdownAdapter(users);
        List<String> selectedNames = new ArrayList<>();
        for (UserData user : selectedAssignees) {
            if (user != null && user.getName() != null) {
                selectedNames.add(user.getName());
            }
        }
        userDropdownAdapter.setSelectedUsers(selectedNames);
        userDropdownAdapter.setOnUserClickListener((user, isSelected) -> {
            hapticHelper.vibrateClick();
            if (isSelected) {
                boolean exists = false;
                for (UserData existing : selectedAssignees) {
                    if (existing != null && user != null &&
                            existing.getId() == user.getId()) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    selectedAssignees.add(user);
                }
            } else {
                selectedAssignees.removeIf(u -> u != null && user != null &&
                        u.getId() == user.getId());
            }

            updateAssigneeChips();
        });

        recyclerView.setAdapter(userDropdownAdapter);

        binding.editTextAssignee.post(() -> {
            int width = binding.editTextAssignee.getWidth();

            userDropdownPopup = new PopupWindow(
                    popupView,
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);

            userDropdownPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            userDropdownPopup.setElevation(8f);
            userDropdownPopup.setOutsideTouchable(true);
            userDropdownPopup.setFocusable(false);

            userDropdownPopup.showAsDropDown(binding.editTextAssignee, 0, 0);

            userDropdownPopup.setOnDismissListener(() -> {
                isShowingDropdown = false;
                binding.editTextAssignee.clearFocus();
            });
        });
    }

    private void updateAssigneeChips() {
        binding.chipGroupAssignees.removeAllViews();

        if (selectedAssignees.isEmpty()) {
            binding.editTextAssignee.setText("");
            binding.chipGroupAssignees.setVisibility(View.GONE);
        } else {
            binding.editTextAssignee.setText(selectedAssignees.size() + " assignee(s) selected");
            binding.chipGroupAssignees.setVisibility(View.VISIBLE);

            for (UserData assignee : selectedAssignees) {
                if (assignee == null)
                    continue;

                Chip chip = new Chip(this);
                chip.setText(assignee.getName() != null ? assignee.getName() : "");
                chip.setCloseIconVisible(true);
                chip.setCloseIconTint(getColorStateList(R.color.text_primary));
                chip.setOnCloseIconClickListener(v -> {
                    hapticHelper.vibrateClick();
                    selectedAssignees.remove(assignee);
                    updateAssigneeChips();
                    if (userDropdownAdapter != null) {
                        List<String> selectedNames = new ArrayList<>();
                        for (UserData user : selectedAssignees) {
                            if (user != null && user.getName() != null) {
                                selectedNames.add(user.getName());
                            }
                        }
                        userDropdownAdapter.setSelectedUsers(selectedNames);
                    }
                });
                binding.chipGroupAssignees.addView(chip);
            }
        }
    }

    private void populateFieldsForEdit() {
        binding.editTextTitle.setText(editingTask.getTitle());
        binding.editTextDescription.setText(editingTask.getDescription());

        selectedAssignees.clear();
        if (editingTask.getAssignees() != null && !editingTask.getAssignees().isEmpty()) {
            selectedAssignees.addAll(editingTask.getAssignees());
        } else if (editingTask.getAssignee() != null) {
            selectedAssignees.add(editingTask.getAssignee());
        }

        updateAssigneeChips();

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            selectedDate.setTime(inputFormat.parse(editingTask.getWorkDate()));
            binding.editTextDate.setText(displayFormat.format(selectedDate.getTime()));
        } catch (Exception e) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            binding.editTextDate.setText(dateFormat.format(selectedDate.getTime()));
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            selectedTime.setTime(inputFormat.parse(editingTask.getWorkTime()));
            binding.editTextTime.setText(displayFormat.format(selectedTime.getTime()));
        } catch (Exception e) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            binding.editTextTime.setText(timeFormat.format(selectedTime.getTime()));
        }

        setStatusChip(editingTask.getStatus());

        if (editingTask.getPriority() != null) {
            setPriorityChip(editingTask.getPriority());
        } else {
            setPriorityChip(Task.TaskPriority.NORMAL);
        }

        if (editingTask.getAttachments() != null) {
            attachments.clear();
            attachments.addAll(editingTask.getAttachments());
            setAttachmentActionListener();
            updateFileDisplay();
        }
    }

    private void setDefaultValues() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        binding.editTextDate.setText(dateFormat.format(selectedDate.getTime()));

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        binding.editTextTime.setText(timeFormat.format(selectedTime.getTime()));

        setStatusChip(Task.TaskStatus.RUNNING);
        setPriorityChip(Task.TaskPriority.NORMAL);
    }

    private void setStatusChip(Task.TaskStatus status) {
        isPreventingStatusDeselection = true;
        binding.chipGroupStatus.clearCheck();

        int chipId = View.NO_ID;
        switch (status) {
            case RUNNING:
                Chip runningChip = binding.chipGroupStatus.findViewById(R.id.chipRunning);
                if (runningChip != null) {
                    runningChip.setChecked(true);
                    chipId = R.id.chipRunning;
                }
                break;
            case PENDING:
                Chip pendingChip = binding.chipGroupStatus.findViewById(R.id.chipPending);
                if (pendingChip != null) {
                    pendingChip.setChecked(true);
                    chipId = R.id.chipPending;
                }
                break;
            case COMPLETED:
                Chip completedChip = binding.chipGroupStatus.findViewById(R.id.chipCompleted);
                if (completedChip != null) {
                    completedChip.setChecked(true);
                    chipId = R.id.chipCompleted;
                }
                break;
            case CANCELLED:
                Chip cancelledChip = binding.chipGroupStatus.findViewById(R.id.chipCancelled);
                if (cancelledChip != null) {
                    cancelledChip.setChecked(true);
                    chipId = R.id.chipCancelled;
                }
                break;
        }

        if (chipId != View.NO_ID) {
            lastCheckedStatusChipId = chipId;
        }
        isPreventingStatusDeselection = false;
    }

    private void setPriorityChip(Task.TaskPriority priority) {
        isPreventingPriorityDeselection = true;
        binding.chipGroupPriority.clearCheck();

        int chipId = View.NO_ID;
        switch (priority) {
            case LOW:
                Chip lowChip = binding.chipGroupPriority.findViewById(R.id.chipPriorityLow);
                if (lowChip != null) {
                    lowChip.setChecked(true);
                    chipId = R.id.chipPriorityLow;
                }
                break;
            case NORMAL:
                Chip mediumChip = binding.chipGroupPriority.findViewById(R.id.chipPriorityNormal);
                if (mediumChip != null) {
                    mediumChip.setChecked(true);
                    chipId = R.id.chipPriorityNormal;
                }
                break;
            case HIGH:
                Chip highChip = binding.chipGroupPriority.findViewById(R.id.chipPriorityHigh);
                if (highChip != null) {
                    highChip.setChecked(true);
                    chipId = R.id.chipPriorityHigh;
                }
                break;
        }

        if (chipId != View.NO_ID) {
            lastCheckedPriorityChipId = chipId;
        }
        isPreventingPriorityDeselection = false;
    }

    private void setupChipSelectionPrevention() {
        int initialStatusChipId = binding.chipGroupStatus.getCheckedChipId();
        if (initialStatusChipId != View.NO_ID) {
            lastCheckedStatusChipId = initialStatusChipId;
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isPreventingStatusDeselection) {
                return;
            }

            if (checkedIds.isEmpty()) {
                if (lastCheckedStatusChipId != -1) {
                    isPreventingStatusDeselection = true;
                    Chip lastChip = group.findViewById(lastCheckedStatusChipId);
                    if (lastChip != null) {
                        lastChip.post(() -> {
                            lastChip.setChecked(true);
                            isPreventingStatusDeselection = false;
                        });
                    } else {
                        isPreventingStatusDeselection = false;
                    }
                    return;
                }
            } else {
                int newCheckedId = checkedIds.get(0);
                if (newCheckedId == lastCheckedStatusChipId) {
                    return;
                }
                lastCheckedStatusChipId = newCheckedId;
            }
        });

        int initialPriorityChipId = binding.chipGroupPriority.getCheckedChipId();
        if (initialPriorityChipId != View.NO_ID) {
            lastCheckedPriorityChipId = initialPriorityChipId;
        }

        binding.chipGroupPriority.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isPreventingPriorityDeselection) {
                return;
            }

            if (checkedIds.isEmpty()) {
                if (lastCheckedPriorityChipId != -1) {
                    isPreventingPriorityDeselection = true;
                    Chip lastChip = group.findViewById(lastCheckedPriorityChipId);
                    if (lastChip != null) {
                        lastChip.post(() -> {
                            lastChip.setChecked(true);
                            isPreventingPriorityDeselection = false;
                        });
                    } else {
                        isPreventingPriorityDeselection = false;
                    }
                    return;
                }
            } else {
                int newCheckedId = checkedIds.get(0);
                if (newCheckedId == lastCheckedPriorityChipId) {
                    return;
                }
                lastCheckedPriorityChipId = newCheckedId;
            }
        });
    }

    private void disableFieldsExceptStatus() {
        binding.editTextTitle.setEnabled(false);
        binding.editTextDescription.setEnabled(false);
        binding.editTextAssignee.setEnabled(false);
        binding.editTextDate.setEnabled(false);
        binding.editTextTime.setEnabled(false);
        binding.buttonAddFile.setVisibility(View.GONE);

        for (int i = 0; i < binding.chipGroupPriority.getChildCount(); i++) {
            View child = binding.chipGroupPriority.getChildAt(i);
            if (child instanceof Chip) {
                ((Chip) child).setClickable(false);
                ((Chip) child).setEnabled(false);
            }
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.CustomDatePickerDialog,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    binding.editTextDate.setText(dateFormat.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.setOnShowListener(dialog -> {
            datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.primary));

            datePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.error_dark));
        });

        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                R.style.CustomTimePickerDialog,
                (view, hourOfDay, minute) -> {
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute);
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                    binding.editTextTime.setText(timeFormat.format(selectedTime.getTime()));
                },
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE),
                true);

        timePickerDialog.setOnShowListener(dialog -> {
            timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.primary));

            timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.error_dark));
        });

        timePickerDialog.show();
    }

    private void saveTask() {
        String title = Objects.requireNonNull(binding.editTextTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(binding.editTextDescription.getText()).toString().trim();

        if (title.isEmpty()) {
            binding.editTextTitle.setError("Title is required");
            return;
        }

        if (!validateFileSizes()) {
            return;
        }

        Task.TaskPriority priority = getSelectedPriority();
        String priorityStr = priorityToString(priority);

        String workDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
        String workTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedTime.getTime());

        List<Integer> assignedTo = new ArrayList<>();
        for (UserData user : selectedAssignees) {
            if (user != null && user.getId() > 0) {
                assignedTo.add(user.getId());
            }
        }

        List<File> files = convertUrisToFiles(attachmentUris);

        if (isEditMode && editingTask != null) {
            TaskViewModel.UpdateTaskParams params = new TaskViewModel.UpdateTaskParams();
            if (!isRestrictedEditMode) {
                params.title = title;
                params.description = description;
                params.workDate = workDate;
                params.workTime = workTime;
                params.priority = priorityStr;
                params.assignedTo = assignedTo;
            }

            Boolean attachmentsUpdate = replaceAttachments ? true : null;

            taskViewModel.updateTask(editingTask.getId(), params, files, attachmentsUpdate,
                    new TaskViewModel.TaskCallback<Task>() {
                        @Override
                        public void onSuccess(Task updatedTask) {
                            AlarmHelper.setTaskAlarm(AddScheduleActivity.this, updatedTask);

                            CookieBarToastHelper.showSuccess(
                                    AddScheduleActivity.this,
                                    "Task Updated",
                                    "Task has been updated successfully",
                                    CookieBarToastHelper.SHORT_DURATION);

                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("task", updatedTask);
                            setResult(RESULT_OK, resultIntent);

                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                finish();
                            }, 500);
                        }

                        @Override
                        public void onError(String message, int errorCode) {
                            showValidationErrors(message, errorCode);
                        }
                    });
        } else {
            TaskViewModel.CreateTaskParams params = new TaskViewModel.CreateTaskParams();
            params.title = title;
            params.description = description;
            params.workDate = workDate;
            params.workTime = workTime;
            params.priority = priorityStr;
            params.assignedTo = assignedTo;

            taskViewModel.createTask(params, files, new TaskViewModel.TaskCallback<Task>() {
                @Override
                public void onSuccess(Task createdTask) {
                    AlarmHelper.setTaskAlarm(AddScheduleActivity.this, createdTask);

                    // Use assignees from the created task (API response) if they have emails
                    // Otherwise use selectedAssignees which should have full user data
                    List<UserData> taskAssignees = createdTask.getAssignees();
                    if (taskAssignees == null || taskAssignees.isEmpty()) {
                        // Fallback to selectedAssignees if API didn't return assignees
                        taskAssignees = selectedAssignees;
                    } else {
                        // Verify assignees have emails, if not, use selectedAssignees
                        boolean allHaveEmails = true;
                        for (UserData assignee : taskAssignees) {
                            if (assignee == null || assignee.getEmail() == null || assignee.getEmail().isEmpty()) {
                                allHaveEmails = false;
                                break;
                            }
                        }
                        if (!allHaveEmails) {
                            // Use selectedAssignees which should have full user data with emails
                            taskAssignees = selectedAssignees;
                        }
                    }

                    FcmNotificationSender.sendTaskAssignmentNotifications(
                            AddScheduleActivity.this,
                            createdTask,
                            taskAssignees);

                    // Show cookiebar notification with sound
                    String notificationTitle = "Task Created";
                    String notificationMessage = createdTask.getTitle() != null && !createdTask.getTitle().isEmpty()
                            ? "Task \"" + createdTask.getTitle() + "\" has been created and assigned"
                            : "Task has been created and assigned";

                    CookieBarToastHelper.showSuccess(
                            AddScheduleActivity.this,
                            notificationTitle,
                            notificationMessage,
                            CookieBarToastHelper.SHORT_DURATION);

                    // Play notification sound
                    SoundHelper.playNotificationSound(AddScheduleActivity.this);

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("task", createdTask);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }

                @Override
                public void onError(String message, int errorCode) {
                    showValidationErrors(message, errorCode);
                }
            });
        }
    }

    private boolean validateFileSizes() {
        // Only validate new local files (from attachmentUris), not already uploaded
        // files
        for (Uri uri : attachmentUris) {
            if (uri == null)
                continue;

            // Skip if it's already a server URL (http/https)
            String uriString = uri.toString();
            if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                continue; // Already uploaded, skip validation
            }

            long fileSize = getFileSize(uri);
            if (fileSize <= 0) {
                // If we can't determine size, show warning but allow (might be a server file)
                continue;
            }

            // Determine if it's an image from MIME type
            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null || mimeType.isEmpty()) {
                // Try to determine from URI or file name
                String fileName = getFileName(uri);
                String extension = getFileExtension(fileName);
                mimeType = getMimeTypeFromExtension(extension);
            }

            boolean isImage = mimeType != null && mimeType.startsWith("image/");
            long maxSize = isImage ? MAX_IMAGE_SIZE : MAX_FILE_SIZE;

            if (fileSize > maxSize) {
                String fileName = getFileName(uri);
                String sizeLimit = isImage ? "5MB" : "10MB";
                Toast.makeText(this,
                        "File size exceeds " + sizeLimit + " limit: " + fileName + " (" + formatFileSize(fileSize)
                                + ")",
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private List<File> convertUrisToFiles(List<Uri> uris) {
        List<File> files = new ArrayList<>();
        for (Uri uri : uris) {
            try {
                File tempFile = createTempFileFromUri(uri);
                if (tempFile != null && tempFile.exists()) {
                    files.add(tempFile);
                }
            } catch (Exception e) {
            }
        }
        return files;
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null)
                return null;

            String fileName = getFileName(uri);
            File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + "_" + fileName);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    private String priorityToString(Task.TaskPriority priority) {
        switch (priority) {
            case LOW:
                return "low";
            case NORMAL:
                return "medium";
            case HIGH:
                return "high";
            default:
                return "medium";
        }
    }

    private void showValidationErrors(String message, int errorCode) {
        String errorMessage = getErrorMessage(message, errorCode);

        if (errorCode == 422) {
            if (message.contains("title")) {
                binding.editTextTitle.setError("Title: " + message);
            } else if (message.contains("priority")) {
                Toast.makeText(this, "Priority: " + message, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private String getErrorMessage(String message, int errorCode) {
        switch (errorCode) {
            case 401:
                return "Authentication required. Please login again.";
            case 403:
                return "You don't have permission to perform this action.";
            case 404:
                return "Resource not found.";
            case 422:
                return "Validation error: " + message;
            default:
                return message != null ? message : "An error occurred. Please try again.";
        }
    }

    private Task.TaskPriority getSelectedPriority() {
        int checkedId = binding.chipGroupPriority.getCheckedChipId();

        if (checkedId == R.id.chipPriorityLow) {
            return Task.TaskPriority.LOW;
        } else if (checkedId == R.id.chipPriorityNormal) {
            return Task.TaskPriority.NORMAL;
        } else if (checkedId == R.id.chipPriorityHigh) {
            return Task.TaskPriority.HIGH;
        }

        return Task.TaskPriority.NORMAL;
    }

    private void showFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"), FILE_PICKER_REQUEST_CODE);
    }

    private String getFileExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    private void updateFileDisplay() {
        if (attachments.isEmpty()) {
            binding.recyclerViewAttachments.setVisibility(View.GONE);
            binding.textNoFiles.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerViewAttachments.setVisibility(View.VISIBLE);
            binding.textNoFiles.setVisibility(View.GONE);
            if (attachmentAdapter != null) {
                setAttachmentActionListener();
                attachmentAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                addFileFromUri(fileUri);
            }
        }
    }

    private void addFileFromUri(Uri fileUri) {
        try {
            String fileName = getFileName(fileUri);
            String mimeType = getContentResolver().getType(fileUri);
            if (mimeType == null || mimeType.isEmpty()) {
                // Try to determine from file extension
                String extension = getFileExtension(fileName);
                mimeType = getMimeTypeFromExtension(extension);
            }

            long fileSize = getFileSize(fileUri);
            if (fileSize <= 0) {
                // Try alternative method to get file size
                fileSize = getFileSizeAlternative(fileUri);
            }

            if (fileSize <= 0) {
                Toast.makeText(this,
                        "Could not determine file size: " + fileName,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if it's an image
            boolean isImage = mimeType != null && mimeType.startsWith("image/");
            long maxSize = isImage ? MAX_IMAGE_SIZE : MAX_FILE_SIZE;

            if (fileSize > maxSize) {
                String sizeLimit = isImage ? "5MB" : "10MB";
                Toast.makeText(this,
                        "File size exceeds " + sizeLimit + " limit: " + fileName + " (" + formatFileSize(fileSize)
                                + ")",
                        Toast.LENGTH_LONG).show();
                return;
            }

            attachmentUris.add(fileUri);

            TaskAttachment attachment = new TaskAttachment(
                    String.valueOf(System.currentTimeMillis()),
                    fileName,
                    fileUri.toString(),
                    getFileExtension(fileName),
                    fileSize,
                    mimeType);

            attachments.add(attachment);
            updateFileDisplay();
            attachmentAdapter.notifyItemInserted(attachments.size() - 1);

            Toast.makeText(this, "File added: " + fileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error adding file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeTypeFromExtension(String extension) {
        if (extension == null || extension.isEmpty())
            return "*/*";

        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "pdf":
                return "application/pdf";
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            default:
                return "*/*";
        }
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        try {
            String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME };
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fileName == null) {
            fileName = "Unknown File";
        }

        return fileName;
    }

    private long getFileSize(Uri uri) {
        if (uri == null)
            return 0;

        try {
            // Method 1: Try OpenableColumns.SIZE (works for all content URIs)
            Cursor cursor = getContentResolver().query(
                    uri,
                    null,
                    null,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    long size = cursor.getLong(sizeIndex);
                    cursor.close();
                    if (size > 0) {
                        return size;
                    }
                } else {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            // Fall through to alternative method
        }

        try {
            // Method 2: Try MediaStore.MediaColumns.SIZE (works for media files)
            Cursor cursor = getContentResolver().query(
                    uri,
                    new String[] { MediaStore.MediaColumns.SIZE },
                    null,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                if (sizeIndex != -1) {
                    long size = cursor.getLong(sizeIndex);
                    cursor.close();
                    if (size > 0) {
                        return size;
                    }
                } else {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            // Fall through to alternative method
        }

        // Method 3: Try alternative method
        return getFileSizeAlternative(uri);
    }

    private long getFileSizeAlternative(Uri uri) {
        if (uri == null)
            return 0;

        try {
            // For file:// URIs, only access if it's in app-specific storage
            if ("file".equals(uri.getScheme())) {
                try {
                    String path = uri.getPath();
                    if (path != null) {
                        File file = new File(path);
                        // Only access files in app-specific directories
                        File cacheDir = getCacheDir();
                        File filesDir = getFilesDir();
                        File externalFilesDir = getExternalFilesDir(null);

                        boolean isInAppStorage = (cacheDir != null && path.startsWith(cacheDir.getAbsolutePath())) ||
                                (filesDir != null && path.startsWith(filesDir.getAbsolutePath())) ||
                                (externalFilesDir != null && path.startsWith(externalFilesDir.getAbsolutePath()));

                        if (isInAppStorage && file.exists() && file.isFile()) {
                            return file.length();
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }

            // For content:// URIs, use AssetFileDescriptor (MediaStore API compatible)
            try (android.content.res.AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(uri, "r")) {
                if (afd != null) {
                    long size = afd.getLength();
                    if (size > 0) {
                        return size;
                    }
                }
            } catch (Exception e) {
                // Fall through
            }
        } catch (Exception e) {
            // Return 0 if we can't determine size
        }

        return 0;
    }

    private void openAttachment(TaskAttachment attachment) {
        String filePathOrUrl = attachment.getFileUrl();
        if (filePathOrUrl == null || filePathOrUrl.isEmpty()) {
            filePathOrUrl = attachment.getFilePath();
        }

        if (filePathOrUrl == null || filePathOrUrl.isEmpty()) {
            Toast.makeText(this, "Invalid file path", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            boolean isUrl = filePathOrUrl.startsWith("http://") || filePathOrUrl.startsWith("https://");
            boolean isLocalUri = filePathOrUrl.startsWith("content://") || filePathOrUrl.startsWith("file://");
            boolean isServerPath = !isUrl && !isLocalUri;

            if (isUrl || isServerPath) {
                String fileUrl = filePathOrUrl;
                if (isServerPath) {
                    if (filePathOrUrl.startsWith("/")) {
                        fileUrl = "https://snap.senda.fit" + filePathOrUrl;
                    } else {
                        fileUrl = "https://snap.senda.fit/storage/" + filePathOrUrl;
                    }
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(fileUrl));
                startActivity(Intent.createChooser(intent, "Open file"));
            } else if (isLocalUri) {
                Uri fileUri = Uri.parse(filePathOrUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String mimeType = attachment.getMimeType();
                if (mimeType == null || mimeType.isEmpty()) {
                    mimeType = getContentResolver().getType(fileUri);
                }
                if (mimeType == null) {
                    mimeType = "*/*";
                }
                intent.setDataAndType(fileUri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Open file"));
            } else {
                // For raw file paths, only access if in app-specific storage
                try {
                    File file = new File(filePathOrUrl);
                    File cacheDir = getCacheDir();
                    File filesDir = getFilesDir();
                    File externalFilesDir = getExternalFilesDir(null);

                    boolean isInAppStorage = (cacheDir != null && filePathOrUrl.startsWith(cacheDir.getAbsolutePath()))
                            ||
                            (filesDir != null && filePathOrUrl.startsWith(filesDir.getAbsolutePath())) ||
                            (externalFilesDir != null && filePathOrUrl.startsWith(externalFilesDir.getAbsolutePath()));

                    if (isInAppStorage && file.exists() && file.isFile()) {
                        Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                                this,
                                getPackageName() + ".fileprovider",
                                file);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        String mimeType = attachment.getMimeType();
                        if (mimeType == null || mimeType.isEmpty()) {
                            mimeType = getContentResolver().getType(fileUri);
                        }
                        if (mimeType == null) {
                            mimeType = "*/*";
                        }
                        intent.setDataAndType(fileUri, mimeType);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, "Open file"));
                    } else {
                        Toast.makeText(this, "File not found or not accessible", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
