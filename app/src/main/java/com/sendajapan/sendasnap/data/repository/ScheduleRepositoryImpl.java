package com.sendajapan.sendasnap.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.sendajapan.sendasnap.domain.repository.ScheduleRepository;
import com.sendajapan.sendasnap.models.ApiResponse;
import com.sendajapan.sendasnap.models.ErrorResponse;
import com.sendajapan.sendasnap.models.shipment.CreateScheduleRequest;
import com.sendajapan.sendasnap.models.shipment.CreateStopoverRequest;
import com.sendajapan.sendasnap.models.shipment.Port;
import com.sendajapan.sendasnap.models.shipment.PortListResponse;
import com.sendajapan.sendasnap.models.shipment.Schedule;
import com.sendajapan.sendasnap.models.shipment.ScheduleListResponse;
import com.sendajapan.sendasnap.models.shipment.ScheduleResponse;
import com.sendajapan.sendasnap.models.shipment.ShippingCompany;
import com.sendajapan.sendasnap.models.shipment.ShippingCompanyListResponse;
import com.sendajapan.sendasnap.models.shipment.Stopover;
import com.sendajapan.sendasnap.models.shipment.StopoverResponse;
import com.sendajapan.sendasnap.models.shipment.UpdateScheduleRequest;
import com.sendajapan.sendasnap.models.shipment.UpdateStopoverRequest;
import com.sendajapan.sendasnap.networking.ApiService;
import com.sendajapan.sendasnap.networking.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleRepositoryImpl implements ScheduleRepository {

    private final ApiService apiService;

    public ScheduleRepositoryImpl(Context context) {
        this.apiService = RetrofitClient.getInstance(context).getApiService();
    }

    @Override
    public void getSchedules(String search, String vesselName, String voyageNo, Integer carrierId,
                            Integer startPortId, Integer endPortId, Integer perPage, Integer page,
                            ScheduleRepositoryCallback<ScheduleListResponse> callback) {
        Call<ApiResponse<ScheduleListResponse>> call = apiService.getSchedules(
                search, vesselName, voyageNo, carrierId, startPortId, endPortId, perPage, page);
        call.enqueue(new Callback<ApiResponse<ScheduleListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ScheduleListResponse>> call,
                                   @NonNull Response<ApiResponse<ScheduleListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ScheduleListResponse> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null) {
                            callback.onSuccess(apiResponse.getData());
                        } else {
                            callback.onError("No schedules data received", response.code());
                        }
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to retrieve schedules";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ScheduleListResponse>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void getSchedule(Integer id, ScheduleRepositoryCallback<Schedule> callback) {
        Call<ApiResponse<ScheduleResponse>> call = apiService.getSchedule(id);
        call.enqueue(new Callback<ApiResponse<ScheduleResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ScheduleResponse>> call,
                                   @NonNull Response<ApiResponse<ScheduleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ScheduleResponse> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getSchedule() != null) {
                            callback.onSuccess(apiResponse.getData().getSchedule());
                        } else {
                            callback.onError("Schedule not found", 404);
                        }
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to retrieve schedule";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    if (response.code() == 404) {
                        errorMessage = "Schedule not found";
                    }
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ScheduleResponse>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void createSchedule(CreateScheduleRequest request, ScheduleRepositoryCallback<Schedule> callback) {
        Call<ApiResponse<ScheduleResponse>> call = apiService.createSchedule(request);
        call.enqueue(new Callback<ApiResponse<ScheduleResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ScheduleResponse>> call,
                                   @NonNull Response<ApiResponse<ScheduleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ScheduleResponse> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getSchedule() != null) {
                            callback.onSuccess(apiResponse.getData().getSchedule());
                        } else {
                            callback.onError("Failed to create schedule", response.code());
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
            public void onFailure(@NonNull Call<ApiResponse<ScheduleResponse>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void updateSchedule(Integer id, UpdateScheduleRequest request, ScheduleRepositoryCallback<Schedule> callback) {
        Call<ApiResponse<ScheduleResponse>> call = apiService.updateSchedule(id, request);
        call.enqueue(new Callback<ApiResponse<ScheduleResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ScheduleResponse>> call,
                                   @NonNull Response<ApiResponse<ScheduleResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ScheduleResponse> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getSchedule() != null) {
                            callback.onSuccess(apiResponse.getData().getSchedule());
                        } else {
                            callback.onError("Failed to update schedule", response.code());
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
            public void onFailure(@NonNull Call<ApiResponse<ScheduleResponse>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void deleteSchedule(Integer id, ScheduleRepositoryCallback<Void> callback) {
        Call<ApiResponse<Object>> call = apiService.deleteSchedule(id);
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        callback.onSuccess(null);
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to delete schedule";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    if (response.code() == 404) {
                        errorMessage = "Schedule not found";
                    }
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void createStopover(Integer scheduleId, CreateStopoverRequest request, ScheduleRepositoryCallback<Stopover> callback) {
        Call<ApiResponse<StopoverResponse>> call = apiService.createStopover(scheduleId, request);
        call.enqueue(new Callback<ApiResponse<StopoverResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<StopoverResponse>> call,
                                   @NonNull Response<ApiResponse<StopoverResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<StopoverResponse> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getStopover() != null) {
                            callback.onSuccess(apiResponse.getData().getStopover());
                        } else {
                            callback.onError("Failed to create stopover", response.code());
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
            public void onFailure(@NonNull Call<ApiResponse<StopoverResponse>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void getStopover(Integer id, ScheduleRepositoryCallback<Stopover> callback) {
        Call<ApiResponse<StopoverResponse>> call = apiService.getStopover(id);
        call.enqueue(new Callback<ApiResponse<StopoverResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<StopoverResponse>> call,
                                   @NonNull Response<ApiResponse<StopoverResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<StopoverResponse> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getStopover() != null) {
                            callback.onSuccess(apiResponse.getData().getStopover());
                        } else {
                            callback.onError("Stopover not found", 404);
                        }
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to retrieve stopover";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    if (response.code() == 404) {
                        errorMessage = "Stopover not found";
                    }
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<StopoverResponse>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void updateStopover(Integer id, UpdateStopoverRequest request, ScheduleRepositoryCallback<Stopover> callback) {
        Call<ApiResponse<StopoverResponse>> call = apiService.updateStopover(id, request);
        call.enqueue(new Callback<ApiResponse<StopoverResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<StopoverResponse>> call,
                                   @NonNull Response<ApiResponse<StopoverResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<StopoverResponse> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getStopover() != null) {
                            callback.onSuccess(apiResponse.getData().getStopover());
                        } else {
                            callback.onError("Failed to update stopover", response.code());
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
            public void onFailure(@NonNull Call<ApiResponse<StopoverResponse>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void deleteStopover(Integer id, ScheduleRepositoryCallback<Void> callback) {
        Call<ApiResponse<Object>> call = apiService.deleteStopover(id);
        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        callback.onSuccess(null);
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to delete stopover";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    if (response.code() == 404) {
                        errorMessage = "Stopover not found";
                    }
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void getPorts(String search, String portType, Integer perPage, Integer page,
                         ScheduleRepositoryCallback<List<Port>> callback) {
        Call<ApiResponse<PortListResponse>> call = apiService.getPorts(search, portType, perPage, page);
        call.enqueue(new Callback<ApiResponse<PortListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PortListResponse>> call,
                                   @NonNull Response<ApiResponse<PortListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PortListResponse> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getPorts() != null) {
                            callback.onSuccess(apiResponse.getData().getPorts());
                        } else {
                            callback.onSuccess(new ArrayList<>());
                        }
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to retrieve ports";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PortListResponse>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
    }

    @Override
    public void getShippingCompanies(String search, String status, Integer perPage, Integer page,
                                     ScheduleRepositoryCallback<List<ShippingCompany>> callback) {
        Call<ApiResponse<ShippingCompanyListResponse>> call = apiService.getShippingCompanies(search, status, perPage, page);
        call.enqueue(new Callback<ApiResponse<ShippingCompanyListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<ShippingCompanyListResponse>> call,
                                   @NonNull Response<ApiResponse<ShippingCompanyListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ShippingCompanyListResponse> apiResponse = response.body();
                    if (apiResponse.getSuccess() != null && apiResponse.getSuccess()) {
                        if (apiResponse.getData() != null && apiResponse.getData().getShippingCompanies() != null) {
                            callback.onSuccess(apiResponse.getData().getShippingCompanies());
                        } else {
                            callback.onSuccess(new ArrayList<>());
                        }
                    } else {
                        String errorMessage = apiResponse.getMessage() != null
                                ? apiResponse.getMessage()
                                : "Failed to retrieve shipping companies";
                        callback.onError(errorMessage, response.code());
                    }
                } else {
                    String errorMessage = parseErrorMessage(response);
                    callback.onError(errorMessage, response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<ShippingCompanyListResponse>> call,
                                  @NonNull Throwable t) {
                String errorMessage = "Network error. Please check your connection and try again.";
                if (t.getMessage() != null) {
                    errorMessage = t.getMessage();
                }
                callback.onError(errorMessage, 0);
            }
        });
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
