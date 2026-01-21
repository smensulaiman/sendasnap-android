package com.sendajapan.sendasnap.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sendajapan.sendasnap.activities.schedule.AddScheduleActivity;
import com.sendajapan.sendasnap.activities.schedule.ScheduleDetailActivity;
import com.sendajapan.sendasnap.adapters.TaskAdapter;
import com.sendajapan.sendasnap.data.dto.PagedResult;
import com.sendajapan.sendasnap.data.dto.StatusUpdateRequest;
import com.sendajapan.sendasnap.data.dto.TaskResponseDto;
import com.sendajapan.sendasnap.data.mapper.TaskMapper;
import com.sendajapan.sendasnap.databinding.DialogTaskStatusBinding;
import com.sendajapan.sendasnap.databinding.FragmentScheduleBinding;
import com.sendajapan.sendasnap.dialogs.LoadingDialog;
import com.sendajapan.sendasnap.models.ApiResponse;
import com.sendajapan.sendasnap.models.Task;
import com.sendajapan.sendasnap.models.UserData;
import com.sendajapan.sendasnap.networking.ApiService;
import com.sendajapan.sendasnap.networking.NetworkUtils;
import com.sendajapan.sendasnap.networking.RetrofitClient;
import com.sendajapan.sendasnap.utils.CookieBarToastHelper;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;
import com.sendajapan.sendasnap.viewmodel.TaskViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment for displaying and managing work schedules/tasks.
 * Handles task listing, filtering, status updates, and deletion.
 */
public class ScheduleFragment extends Fragment
        implements TaskAdapter.OnTaskClickListener, TaskAdapter.OnTaskLongClickListener {

    // Constants
    private static final int ADD_TASK_REQUEST_CODE = 1001;
    private static final String DATE_FORMAT_INPUT = "yyyy-MM-dd";
    private static final String DATE_FORMAT_OUTPUT = "EEEE, MMMM dd, yyyy";
    private static final String DATE_SEPARATOR = "T";
    private static final int MIN_DATE_LENGTH = 10;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_UNPROCESSABLE_ENTITY = 422;
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_MANAGER = "manager";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String CHIP_TEXT_ALL = "All";
    private static final String CHIP_TEXT_IN_PROGRESS = "In Progress";
    private static final String CHIP_TEXT_PENDING = "Pending";
    private static final String CHIP_TEXT_COMPLETED = "Completed";
    private static final String CHIP_TEXT_CANCELLED = "Cancelled";
    private static final String DEFAULT_DATE_TEXT = "Today's Tasks";

    // View Binding
    private FragmentScheduleBinding binding;

    // Dependencies
    private SharedPrefsManager prefsManager;
    private TaskAdapter taskAdapter;
    private TaskViewModel taskViewModel;
    private ApiService apiService;
    private NetworkUtils networkUtils;
    private HapticFeedbackHelper hapticHelper;
    private LoadingDialog loadingDialog;

    // State
    private Task.TaskStatus currentFilter = null;
    private String selectedDate;
    private boolean isFirstLoad = true;
    private boolean isPreventingDeselection = false;
    private int lastCheckedChipId = -1;

    // Data
    private final List<Task> allTasks = new ArrayList<>();
    private final List<Task> filteredTasks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeDependencies();
        setupViews();
        initializeSelectedDate();
        loadTasksIfConnected();
    }

    /**
     * Initializes all dependencies and helper classes.
     */
    private void initializeDependencies() {
        prefsManager = SharedPrefsManager.getInstance(requireContext());
        taskViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(TaskViewModel.class);
        apiService = RetrofitClient.getInstance(requireContext()).getApiService();
        networkUtils = NetworkUtils.getInstance(requireContext());
        hapticHelper = HapticFeedbackHelper.getInstance(requireContext());
    }

    /**
     * Sets up all UI components and listeners.
     */
    private void setupViews() {
        setupRecyclerView();
        setupCalendar();
        setupFilterChips();
        setupFAB();
    }

    /**
     * Initializes the selected date to today's date.
     */
    private void initializeSelectedDate() {
        Calendar today = Calendar.getInstance();
        selectedDate = formatDate(today.getTime(), DATE_FORMAT_INPUT);
        updateSelectedDateText();
    }

    /**
     * Sets up the RecyclerView for displaying tasks.
     */
    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(filteredTasks, this);
        taskAdapter.setOnTaskLongClickListener(this);
        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewTasks.setAdapter(taskAdapter);
    }

    /**
     * Sets up the calendar view and date selection listener.
     */
    private void setupCalendar() {
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = formatDate(calendar.getTime(), DATE_FORMAT_INPUT);
            updateSelectedDateText();
            loadTasksIfConnected();
        });
    }

    /**
     * Sets up filter chips for task status filtering.
     */
    private void setupFilterChips() {
        int initialCheckedId = binding.chipGroupStatus.getCheckedChipId();
        if (initialCheckedId != View.NO_ID) {
            lastCheckedChipId = initialCheckedId;
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isPreventingDeselection) {
                return;
            }

            handleChipSelectionChange(group, checkedIds);
        });
    }

    /**
     * Handles chip selection change events.
     *
     * @param group      The chip group
     * @param checkedIds List of checked chip IDs
     */
    private void handleChipSelectionChange(ChipGroup group,
            List<Integer> checkedIds) {
        if (checkedIds.isEmpty()) {
            preventChipDeselection(group);
            currentFilter = null;
        } else {
            int newCheckedId = checkedIds.get(0);
            if (newCheckedId == lastCheckedChipId) {
                return;
            }
            lastCheckedChipId = newCheckedId;
            updateFilterFromChip(group, newCheckedId);
        }

        filterTasks();
        loadTasksIfConnected();
    }

    /**
     * Prevents chip deselection by re-checking the last selected chip.
     *
     * @param group The chip group
     */
    private void preventChipDeselection(ChipGroup group) {
        if (lastCheckedChipId != -1) {
            isPreventingDeselection = true;
            Chip lastChip = group.findViewById(lastCheckedChipId);
            if (lastChip != null) {
                lastChip.post(() -> {
                    lastChip.setChecked(true);
                    isPreventingDeselection = false;
                });
            } else {
                isPreventingDeselection = false;
            }
        }
    }

    /**
     * Updates the current filter based on the selected chip.
     *
     * @param group     The chip group
     * @param checkedId The ID of the checked chip
     */
    private void updateFilterFromChip(ChipGroup group, int checkedId) {
        Chip selectedChip = group.findViewById(checkedId);
        if (selectedChip != null) {
            String chipText = selectedChip.getText().toString();
            currentFilter = getTaskStatusFromChipText(chipText);
        }
    }

    /**
     * Maps chip text to TaskStatus enum.
     *
     * @param chipText The text of the selected chip
     * @return The corresponding TaskStatus, or null for "All"
     */
    private Task.TaskStatus getTaskStatusFromChipText(String chipText) {
        switch (chipText) {
            case CHIP_TEXT_IN_PROGRESS:
                return Task.TaskStatus.RUNNING;
            case CHIP_TEXT_PENDING:
                return Task.TaskStatus.PENDING;
            case CHIP_TEXT_COMPLETED:
                return Task.TaskStatus.COMPLETED;
            case CHIP_TEXT_CANCELLED:
                return Task.TaskStatus.CANCELLED;
            case CHIP_TEXT_ALL:
            default:
                return null;
        }
    }

    /**
     * Sets up the Floating Action Button for adding new tasks.
     */
    private void setupFAB() {
        UserData currentUser = prefsManager.getUser();
        boolean canCreateTask = canUserCreateTask(currentUser);

        if (canCreateTask) {
            binding.fabAddTask.setVisibility(View.VISIBLE);
            binding.fabAddTask.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                openAddTaskActivity();
            });
        } else {
            binding.fabAddTask.setVisibility(View.GONE);
        }
    }

    /**
     * Checks if the current user has permission to create tasks.
     *
     * @param user The current user
     * @return true if user can create tasks, false otherwise
     */
    private boolean canUserCreateTask(@Nullable UserData user) {
        if (user == null) {
            return false;
        }
        String role = user.getRole();
        return role != null && (role.equalsIgnoreCase(ROLE_ADMIN) || role.equalsIgnoreCase(ROLE_MANAGER));
    }

    /**
     * Opens the AddScheduleActivity to create a new task.
     */
    private void openAddTaskActivity() {
        Intent intent = new Intent(requireContext(), AddScheduleActivity.class);
        startActivityForResult(intent, ADD_TASK_REQUEST_CODE);
    }

    /**
     * Updates the selected date text display.
     */
    private void updateSelectedDateText() {
        if (selectedDate == null) {
            binding.textSelectedDate.setText(DEFAULT_DATE_TEXT);
            return;
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(DATE_FORMAT_INPUT, Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat(DATE_FORMAT_OUTPUT, Locale.getDefault());
            Date date = inputFormat.parse(selectedDate);
            if (date != null) {
                String formattedDate = outputFormat.format(date);
                binding.textSelectedDate.setText(formattedDate);
            } else {
                binding.textSelectedDate.setText(DEFAULT_DATE_TEXT);
            }
        } catch (Exception e) {
            binding.textSelectedDate.setText(DEFAULT_DATE_TEXT);
        }
    }

    /**
     * Checks internet connectivity before loading tasks from API.
     */
    private void loadTasksIfConnected() {
        if (!networkUtils.isNetworkAvailable()) {
            showNoInternetError();
            hideShimmer();
            return;
        }

        loadTasksFromApi();
    }

    /**
     * Shows no internet connection error message.
     */
    private void showNoInternetError() {
        CookieBarToastHelper.showNoInternet(requireContext());
        hapticHelper.vibrateError();
    }

    /**
     * Loads tasks from the API for the selected date.
     */
    private void loadTasksFromApi() {
        if (selectedDate == null) {
            initializeSelectedDate();
        }

        showShimmer();

        taskViewModel.listTasks(selectedDate, selectedDate, new TaskViewModel.TaskCallback<PagedResult<Task>>() {
            @Override
            public void onSuccess(PagedResult<Task> result) {
                if (!isAdded() || binding == null) {
                    return;
                }

                handleTasksLoadSuccess(result);
            }

            @Override
            public void onError(String message, int errorCode) {
                if (!isAdded() || binding == null) {
                    return;
                }

                handleTasksLoadError(message, errorCode);
            }
        });
    }

    /**
     * Handles successful task loading.
     *
     * @param result The paged result containing tasks
     */
    private void handleTasksLoadSuccess(@Nullable PagedResult<Task> result) {
        allTasks.clear();
        if (result != null && result.getItems() != null) {
            allTasks.addAll(result.getItems());
        }

        hideShimmer();
        filterTasks();
    }

    /**
     * Handles task loading errors.
     *
     * @param message   Error message
     * @param errorCode HTTP error code
     */
    private void handleTasksLoadError(@Nullable String message, int errorCode) {
        hideShimmer();
        allTasks.clear();
        filterTasks();

        String errorMessage = getErrorMessage(message, errorCode);
        Toast.makeText(requireContext(), "Failed to load tasks: " + errorMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     * Gets a user-friendly error message based on error code.
     *
     * @param message   The error message from server
     * @param errorCode The HTTP error code
     * @return User-friendly error message
     */
    private String getErrorMessage(@Nullable String message, int errorCode) {
        switch (errorCode) {
            case HTTP_UNAUTHORIZED:
                return "Authentication required. Please login again.";
            case HTTP_FORBIDDEN:
                return "You don't have permission to perform this action.";
            case HTTP_NOT_FOUND:
                return "Resource not found.";
            case HTTP_UNPROCESSABLE_ENTITY:
                return "Validation error: " + (message != null ? message : "");
            default:
                return message != null ? message : "An error occurred. Please try again.";
        }
    }

    /**
     * Shows the shimmer loading effect.
     */
    private void showShimmer() {
        if (binding == null) {
            return;
        }
        binding.shimmerTasks.setVisibility(View.VISIBLE);
        binding.shimmerTasks.startShimmer();
        binding.recyclerViewTasks.setVisibility(View.GONE);
        binding.layoutEmptyState.setVisibility(View.INVISIBLE);
    }

    /**
     * Hides the shimmer loading effect.
     */
    private void hideShimmer() {
        if (binding == null) {
            return;
        }
        binding.shimmerTasks.stopShimmer();
        binding.shimmerTasks.setVisibility(View.GONE);
        binding.recyclerViewTasks.setVisibility(View.VISIBLE);
    }

    /**
     * Filters tasks based on selected date and status filter.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void filterTasks() {
        filteredTasks.clear();

        for (Task task : allTasks) {
            String taskDate = extractDateFromWorkDate(task.getWorkDate());

            if (taskDate != null && taskDate.equals(selectedDate)) {
                if (currentFilter == null || task.getStatus() == currentFilter) {
                    filteredTasks.add(task);
                }
            }
        }

        taskAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    /**
     * Extracts date string from work date (handles ISO format with 'T' separator).
     *
     * @param workDate The work date string (may contain time)
     * @return Extracted date string in yyyy-MM-dd format, or null if invalid
     */
    @Nullable
    private String extractDateFromWorkDate(@Nullable String workDate) {
        if (workDate == null || workDate.isEmpty()) {
            return null;
        }

        if (workDate.contains(DATE_SEPARATOR)) {
            return workDate.substring(0, MIN_DATE_LENGTH);
        }

        if (workDate.length() >= MIN_DATE_LENGTH) {
            return workDate.substring(0, MIN_DATE_LENGTH);
        }

        return workDate;
    }

    /**
     * Updates the empty state visibility based on filtered tasks.
     */
    private void updateEmptyState() {
        if (binding == null) {
            return;
        }

        if (filteredTasks.isEmpty()) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewTasks.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewTasks.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Formats a date to the specified format string.
     *
     * @param date   The date to format
     * @param format The format pattern
     * @return Formatted date string
     */
    private String formatDate(@NonNull Date date, @NonNull String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        return dateFormat.format(date);
    }

    /**
     * Handles task saved event after adding a new task.
     *
     * @param task The saved task
     */
    private void onTaskSaved(@NonNull Task task) {
        loadTasksIfConnected();
        Toast.makeText(requireContext(), "Task added successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskClick(@NonNull Task task) {
        hapticHelper.vibrateClick();
        openTaskDetails(task);
    }

    /**
     * Opens the task details activity.
     *
     * @param task The task to display
     */
    private void openTaskDetails(@NonNull Task task) {
        Intent intent = new Intent(requireContext(), ScheduleDetailActivity.class);
        intent.putExtra("task", task);
        startActivity(intent);
    }

    @Override
    public void onTaskLongClick(@NonNull Task task) {
        hapticHelper.vibrateClick();
        showTaskStatusDialog(task);
    }

    /**
     * Shows dialog for updating task status or deleting task.
     *
     * @param task The task to modify
     */
    private void showTaskStatusDialog(@NonNull Task task) {
        DialogTaskStatusBinding dialogBinding = DialogTaskStatusBinding.inflate(getLayoutInflater());

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        dialogBuilder.setView(dialogBinding.getRoot());
        dialogBuilder.setCancelable(true);

        AlertDialog dialog = dialogBuilder.create();

        setupStatusDialogButtons(dialogBinding, task, dialog);
        dialog.show();
    }

    /**
     * Sets up button listeners for the task status dialog.
     *
     * @param dialogBinding The dialog binding
     * @param task          The task being modified
     * @param dialog        The dialog instance
     */
    private void setupStatusDialogButtons(@NonNull DialogTaskStatusBinding dialogBinding,
            @NonNull Task task,
            @NonNull AlertDialog dialog) {
        dialogBinding.btnStatusPending.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            updateTaskStatusIfConnected(task, STATUS_PENDING, dialog);
        });

        dialogBinding.btnStatusRunning.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            updateTaskStatusIfConnected(task, STATUS_RUNNING, dialog);
        });

        dialogBinding.btnStatusCompleted.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            updateTaskStatusIfConnected(task, STATUS_COMPLETED, dialog);
        });

        dialogBinding.btnStatusCancelled.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            updateTaskStatusIfConnected(task, STATUS_CANCELLED, dialog);
        });

        UserData currentUser = prefsManager.getUser();
        boolean canCreateTask = canUserCreateTask(currentUser);
        if (canCreateTask) {
            dialogBinding.btnDelete.setVisibility(View.VISIBLE);
            dialogBinding.btnDelete.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                dialog.dismiss();
                showDeleteConfirmationDialog(task);
            });
        } else {
            dialogBinding.btnDelete.setVisibility(View.INVISIBLE);
        }

        dialogBinding.btnClose.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            dialog.dismiss();
        });
    }

    /**
     * Checks internet connectivity before updating task status.
     *
     * @param task   The task to update
     * @param status The new status
     * @param dialog The dialog to dismiss on success
     */
    private void updateTaskStatusIfConnected(@NonNull Task task, @NonNull String status, @NonNull AlertDialog dialog) {
        if (!networkUtils.isNetworkAvailable()) {
            showNoInternetError();
            return;
        }

        updateTaskStatus(task, status, dialog);
    }

    /**
     * Updates the task status via API.
     *
     * @param task   The task to update
     * @param status The new status string
     * @param dialog The dialog to dismiss on success
     */
    private void updateTaskStatus(@NonNull Task task, @NonNull String status, @NonNull AlertDialog dialog) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            return;
        }

        showLoadingDialog("Updating task status...");

        StatusUpdateRequest request = new StatusUpdateRequest(status);
        Call<ApiResponse<TaskResponseDto>> call = apiService.updateTaskStatus(task.getId(), request);

        call.enqueue(new Callback<ApiResponse<TaskResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TaskResponseDto>> call,
                    @NonNull Response<ApiResponse<TaskResponseDto>> response) {
                dismissLoadingDialog();

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    handleStatusUpdateSuccess(response.body().getData(), dialog);
                } else {
                    handleStatusUpdateError(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TaskResponseDto>> call, @NonNull Throwable t) {
                dismissLoadingDialog();
                showNetworkError();
            }
        });
    }

    /**
     * Handles successful task status update.
     *
     * @param taskResponseDto The updated task data
     * @param dialog          The dialog to dismiss
     */
    private void handleStatusUpdateSuccess(@NonNull TaskResponseDto taskResponseDto, @NonNull AlertDialog dialog) {
        if (taskResponseDto.getTask() != null) {
            Task updatedTask = TaskMapper.toDomain(taskResponseDto.getTask());
            updateTaskInList(updatedTask);
            dialog.dismiss();

            CookieBarToastHelper.showSuccess(requireContext(), "Success",
                    "Task status updated successfully", CookieBarToastHelper.SHORT_DURATION);
        } else {
            CookieBarToastHelper.showError(requireContext(), "Error",
                    "Invalid response from server", CookieBarToastHelper.LONG_DURATION);
        }
    }

    /**
     * Handles task status update error.
     *
     * @param response The API response
     */
    private void handleStatusUpdateError(@NonNull Response<ApiResponse<TaskResponseDto>> response) {
        String errorMessage = "Failed to update task status";
        if (response.errorBody() != null) {
            try {
                errorMessage = response.errorBody().string();
            } catch (Exception e) {
                // Keep default error message
            }
        }
        CookieBarToastHelper.showError(requireContext(), "Error", errorMessage,
                CookieBarToastHelper.LONG_DURATION);
    }

    /**
     * Shows network error message.
     */
    private void showNetworkError() {
        CookieBarToastHelper.showError(requireContext(), "Error",
                "Network error. Please check your connection and try again.",
                CookieBarToastHelper.LONG_DURATION);
    }

    /**
     * Updates a task in the local list.
     *
     * @param updatedTask The updated task
     */
    private void updateTaskInList(@NonNull Task updatedTask) {
        for (int i = 0; i < allTasks.size(); i++) {
            if (allTasks.get(i).getId() == updatedTask.getId()) {
                allTasks.set(i, updatedTask);
                break;
            }
        }
        filterTasks();
    }

    /**
     * Shows confirmation dialog before deleting a task.
     *
     * @param task The task to delete
     */
    private void showDeleteConfirmationDialog(@NonNull Task task) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    hapticHelper.vibrateClick();
                    deleteTaskIfConnected(task);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    hapticHelper.vibrateClick();
                })
                .show();
    }

    /**
     * Checks internet connectivity before deleting task.
     *
     * @param task The task to delete
     */
    private void deleteTaskIfConnected(@NonNull Task task) {
        if (!networkUtils.isNetworkAvailable()) {
            showNoInternetError();
            return;
        }

        deleteTask(task);
    }

    /**
     * Deletes a task via API.
     *
     * @param task The task to delete
     */
    private void deleteTask(@NonNull Task task) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            return;
        }

        showLoadingDialog("Deleting task...");

        Call<ApiResponse<Object>> call = apiService.deleteTask(task.getId());

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                dismissLoadingDialog();

                if (response.isSuccessful() && response.body() != null) {
                    handleDeleteSuccess(response.body(), task);
                } else {
                    handleDeleteError(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                dismissLoadingDialog();
                showNetworkError();
            }
        });
    }

    /**
     * Handles successful task deletion.
     *
     * @param apiResponse The API response
     * @param task        The deleted task
     */
    private void handleDeleteSuccess(@NonNull ApiResponse<Object> apiResponse, @NonNull Task task) {
        if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
            allTasks.removeIf(t -> t.getId() == task.getId());
            filterTasks();
            String message = apiResponse.getMessage() != null
                    ? apiResponse.getMessage()
                    : "Task deleted successfully";
            CookieBarToastHelper.showSuccess(requireContext(), "Success", message,
                    CookieBarToastHelper.SHORT_DURATION);
        } else {
            String errorMessage = apiResponse.getMessage() != null
                    ? apiResponse.getMessage()
                    : "Failed to delete task";
            CookieBarToastHelper.showError(requireContext(), "Error", errorMessage,
                    CookieBarToastHelper.LONG_DURATION);
        }
    }

    /**
     * Handles task deletion error.
     *
     * @param response The API response
     */
    private void handleDeleteError(@NonNull Response<ApiResponse<Object>> response) {
        String errorMessage = "Failed to delete task";
        if (response.errorBody() != null) {
            try {
                errorMessage = response.errorBody().string();
            } catch (Exception e) {
                // Keep default error message
            }
        }
        if (response.code() == HTTP_NOT_FOUND) {
            errorMessage = "Task not found";
        }
        CookieBarToastHelper.showError(requireContext(), "Error", errorMessage,
                CookieBarToastHelper.LONG_DURATION);
    }

    /**
     * Shows loading dialog with message.
     *
     * @param message The message to display
     */
    private void showLoadingDialog(@NonNull String message) {
        loadingDialog = new LoadingDialog.Builder(requireContext())
                .setMessage(message)
                .setCancelable(false)
                .setShowProgressIndicator(true)
                .build();
        loadingDialog.show();
    }

    /**
     * Dismisses the loading dialog if showing.
     */
    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_TASK_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            Task task = (Task) data.getSerializableExtra("task");
            if (task != null) {
                onTaskSaved(task);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && binding != null && !isFirstLoad) {
            loadTasksIfConnected();
        }
        isFirstLoad = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissLoadingDialog();
        binding = null;
    }
}
