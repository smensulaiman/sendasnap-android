package com.sendajapan.sendasnap.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.sendajapan.sendasnap.data.dto.PagedResult;
import com.sendajapan.sendasnap.data.dto.StatusUpdateRequest;
import com.sendajapan.sendasnap.data.dto.TaskResponseDto;
import com.sendajapan.sendasnap.data.dto.TasksListResponseDto;
import com.sendajapan.sendasnap.data.mapper.TaskMapper;
import com.sendajapan.sendasnap.domain.repository.TaskRepository;
import com.sendajapan.sendasnap.models.Task;
import com.sendajapan.sendasnap.models.ApiResponse;
import com.sendajapan.sendasnap.models.ErrorResponse;
import com.sendajapan.sendasnap.networking.ApiService;
import com.sendajapan.sendasnap.networking.RetrofitClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskRepositoryImpl implements TaskRepository {

    private static final String TAG = "TaskRepositoryImpl";
    
    private final ApiService apiService;
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    public TaskRepositoryImpl(Context context) {
        this.apiService = RetrofitClient.getInstance(context).getApiService();
    }

    @Override
    public void list(String fromDate, String toDate, TaskRepositoryCallback<PagedResult<Task>> callback) {

        Call<ApiResponse<TasksListResponseDto>> call = apiService.getTasksList(fromDate, toDate);
        call.enqueue(new Callback<ApiResponse<TasksListResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TasksListResponseDto>> call,
                                   @NonNull Response<ApiResponse<TasksListResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<TasksListResponseDto> apiResponse = response.body();

                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null) {
                            List<Task> tasks = TaskMapper.toDomainList(apiResponse.getData().getTasks());
                            PagedResult<Task> result = new PagedResult<>(
                                    tasks,
                                    apiResponse.getData().getPagination()
                            );
                            callback.onSuccess(result);
                        } else {
                            callback.onError("No tasks data received", response.code());
                        }
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to retrieve tasks";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TasksListResponseDto>> call, Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void get(Integer id, TaskRepositoryCallback<Task> callback) {
        Call<ApiResponse<TaskResponseDto>> call = apiService.getTask(id);
        call.enqueue(new Callback<ApiResponse<TaskResponseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<TaskResponseDto>> call,
                                   Response<ApiResponse<TaskResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<TaskResponseDto> apiResponse = response.body();

                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getTask() != null) {
                            Task task = TaskMapper.toDomain(apiResponse.getData().getTask());
                            callback.onSuccess(task);
                        } else {
                            callback.onError("Task not found", 404);
                        }
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to retrieve task";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    if (response.code() == 404) {
                        errorMessage = "Task not found";
                    }
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TaskResponseDto>> call, Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void create(CreateTaskParams params, List<File> files, TaskRepositoryCallback<Task> callback) {
        // Validate file sizes
        if (files != null) {
            for (File file : files) {
                if (file.length() > MAX_FILE_SIZE) {
                    callback.onError("File size exceeds 10MB limit: " + file.getName(), 422);
                    return;
                }
            }
        }

        List<MultipartBody.Part> parts = buildMultipartParts(params, files, null);

        Call<ApiResponse<TaskResponseDto>> call = apiService.createTask(parts);
        call.enqueue(new Callback<ApiResponse<TaskResponseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<TaskResponseDto>> call,
                                   Response<ApiResponse<TaskResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<TaskResponseDto> apiResponse = response.body();

                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getTask() != null) {
                            Task task = TaskMapper.toDomain(apiResponse.getData().getTask());
                            callback.onSuccess(task);
                        } else {
                            callback.onError("Failed to create task", response.code());
                        }
                    } else {
                        String errorMessage = parseErrorMessage(apiResponse, response);
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TaskResponseDto>> call, Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void update(Integer id, UpdateTaskParams params, List<File> files, Boolean attachmentsUpdate,
                       TaskRepositoryCallback<Task> callback) {
        if (files != null) {
            for (File file : files) {
                if (file.length() > MAX_FILE_SIZE) {
                    callback.onError("File size exceeds 10MB limit: " + file.getName(), 422);
                    return;
                }
            }
        }

        List<MultipartBody.Part> parts = buildMultipartParts(params, files, attachmentsUpdate);
        
        // Count non-file parts (fields)
        int fieldPartsCount = 0;
        int filePartsCount = 0;
        for (int i = 0; i < parts.size(); i++) {
            MultipartBody.Part part = parts.get(i);
            if (part != null && part.headers() != null) {
                String contentDisposition = part.headers().get("Content-Disposition");
                if (contentDisposition != null) {
                    if (contentDisposition.contains("name=\"")) {
                        int start = contentDisposition.indexOf("name=\"") + 6;
                        int end = contentDisposition.indexOf("\"", start);
                        if (end > start) {
                            String fieldName = contentDisposition.substring(start, end);
                            if (fieldName.startsWith("attachments[")) {
                                filePartsCount++;
                            } else {
                                fieldPartsCount++;
                            }
                        }
                    }
                }
            }
        }
        
        // Warn if no fields are being sent (only files or attachments_update)
        if (fieldPartsCount == 0 && files == null) {
        }

        Call<ApiResponse<TaskResponseDto>> call = apiService.updateTask(id, parts);
        call.enqueue(new Callback<ApiResponse<TaskResponseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<TaskResponseDto>> call,
                                   Response<ApiResponse<TaskResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<TaskResponseDto> apiResponse = response.body();

                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getTask() != null) {
                            Task task = TaskMapper.toDomain(apiResponse.getData().getTask());
                            callback.onSuccess(task);
                        } else {
                            callback.onError("Failed to update task", response.code());
                        }
                    } else {
                        String errorMessage = parseErrorMessage(apiResponse, response);
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    if (response.errorBody() != null) {
                        try {
                            response.errorBody().string();
                        } catch (Exception e) {
                        }
                    }
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TaskResponseDto>> call, Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void delete(Integer id, TaskRepositoryCallback<Void> callback) {
        Call<ApiResponse<Object>> call = apiService.deleteTask(id);
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();

                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        callback.onSuccess(null);
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to delete task";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    if (response.code() == 404) {
                        errorMessage = "Task not found";
                    }
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void updateStatus(Integer id, Task.TaskStatus status, TaskRepositoryCallback<Task> callback) {
        // Convert enum to string for API
        String statusStr = status != null ? status.name().toLowerCase() : "pending";
        StatusUpdateRequest request = new StatusUpdateRequest(statusStr);

        Call<ApiResponse<TaskResponseDto>> call = apiService.updateTaskStatus(id, request);
        call.enqueue(new Callback<ApiResponse<TaskResponseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<TaskResponseDto>> call,
                                   Response<ApiResponse<TaskResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<TaskResponseDto> apiResponse = response.body();

                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getTask() != null) {
                            Task task = TaskMapper.toDomain(apiResponse.getData().getTask());
                            callback.onSuccess(task);
                        } else {
                            callback.onError("Failed to update task status", response.code());
                        }
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to update task status";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TaskResponseDto>> call, Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    private List<MultipartBody.Part> buildMultipartParts(Object params, List<File> files, Boolean attachmentsUpdate) {
        List<MultipartBody.Part> parts = new ArrayList<>();

        if (params instanceof CreateTaskParams) {
            CreateTaskParams createParams = (CreateTaskParams) params;
            addPart(parts, "title", createParams.title);
            addPart(parts, "description", createParams.description);
            addPart(parts, "work_date", createParams.workDate);
            addPart(parts, "work_time", createParams.workTime);
            addPart(parts, "priority", createParams.priority);
            addPart(parts, "due_date", createParams.dueDate);

            if (createParams.assignedTo != null) {
                for (Integer userId : createParams.assignedTo) {
                    addPart(parts, "assigned_to[]", userId != null ? userId.toString() : null);
                }
            }
        } else if (params instanceof UpdateTaskParams) {
            UpdateTaskParams updateParams = (UpdateTaskParams) params;
            
            addPart(parts, "title", updateParams.title);
            addPart(parts, "description", updateParams.description);
            addPart(parts, "work_date", updateParams.workDate);
            addPart(parts, "work_time", updateParams.workTime);
            addPart(parts, "priority", updateParams.priority);
            addPart(parts, "due_date", updateParams.dueDate);

            if (updateParams.assignedTo != null) {
                if (updateParams.assignedTo.isEmpty()) {
                    addPart(parts, "assigned_to[]", "");
                } else {
                    for (Integer userId : updateParams.assignedTo) {
                        if (userId != null) {
                            addPart(parts, "assigned_to[]", userId.toString());
                        }
                    }
                }
            }

            if (attachmentsUpdate != null) {
                addPart(parts, "attachments_update", attachmentsUpdate ? "1" : "0");
            }
            
            addPart(parts, "_method", "PUT");
        }

        // Add file attachments
        if (files != null) {
            for (File file : files) {
                if (file.exists() && file.isFile()) {
                    RequestBody requestFile = RequestBody.create(
                            MediaType.parse("application/octet-stream"), file);
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                            "attachments[]", file.getName(), requestFile);
                    parts.add(filePart);
                }
            }
        }

        return parts;
    }

    private void addPart(List<MultipartBody.Part> parts, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            RequestBody body = RequestBody.create(MediaType.parse("text/plain"), value);
            parts.add(MultipartBody.Part.createFormData(key, null, body));
        }
    }

    private String parseErrorMessage(Response<?> response) {
        String defaultMessage = "An error occurred. Please try again.";

        if (response.errorBody() != null) {
            try {
                String errorBodyStr = response.errorBody().string();
                Gson gson = new Gson();
                ErrorResponse errorResponse = gson.fromJson(errorBodyStr, ErrorResponse.class);

                if (errorResponse != null && errorResponse.getMessage() != null) {
                    return errorResponse.getMessage();
                }
            } catch (Exception e) {
                // If parsing fails, return default message
            }
        }

        return defaultMessage;
    }

    private String parseErrorMessage(ApiResponse<?> apiResponse, Response<?> response) {
        if (apiResponse.getMessage() != null) {
            return apiResponse.getMessage();
        }
        return parseErrorMessage(response);
    }
}

