package com.sendajapan.sendasnap.networking;

import com.sendajapan.sendasnap.data.dto.StatusUpdateRequest;
import com.sendajapan.sendasnap.data.dto.TaskResponseDto;
import com.sendajapan.sendasnap.data.dto.TasksListResponseDto;
import com.sendajapan.sendasnap.data.dto.UsersListResponseDto;
import com.sendajapan.sendasnap.models.ApiResponse;
import com.sendajapan.sendasnap.models.ChangePasswordRequest;
import com.sendajapan.sendasnap.models.LoginRequest;
import com.sendajapan.sendasnap.models.LoginResponse;
import com.sendajapan.sendasnap.models.TasksResponse;
import com.sendajapan.sendasnap.models.UsersResponse;
import com.sendajapan.sendasnap.models.Vehicle;
import com.sendajapan.sendasnap.models.VehicleImageUploadResponse;
import com.sendajapan.sendasnap.models.VehicleSearchResponse;
import com.sendajapan.sendasnap.models.shipment.CreateScheduleRequest;
import com.sendajapan.sendasnap.models.shipment.CreateStopoverRequest;
import com.sendajapan.sendasnap.models.shipment.PortListResponse;
import com.sendajapan.sendasnap.models.shipment.ScheduleListResponse;
import com.sendajapan.sendasnap.models.shipment.ScheduleResponse;
import com.sendajapan.sendasnap.models.shipment.ShippingCompanyListResponse;
import com.sendajapan.sendasnap.models.shipment.StopoverResponse;
import com.sendajapan.sendasnap.models.shipment.UpdateScheduleRequest;
import com.sendajapan.sendasnap.models.shipment.UpdateStopoverRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

import okhttp3.MultipartBody;

public interface ApiService {

    // Login
    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // Refresh token
    @POST("api/v1/auth/refresh")
    Call<LoginResponse> refreshToken();

    // Get current authenticated user
    @GET("api/v1/auth/me")
    Call<LoginResponse> getMe();

    // Change password
    @POST("api/v1/auth/change-password")
    Call<ResponseBody> changePassword(@Body ChangePasswordRequest changePasswordRequest);

    // Search vehicle by chassis number
    @GET("vehicle/{chassisNumber}")
    Call<Vehicle> getVehicleByChassisNumber(@Path("chassisNumber") String chassisNumber);

    // Search vehicles
    @GET("api/v1/vehicles/search")
    Call<VehicleSearchResponse> searchVehicles(
            @Query("search_type") String searchType,
            @Query("search_query") String searchQuery);

    // Upload vehicle images
    @Multipart
    @POST("api/v1/vehicle/{id}/images")
    Call<ResponseBody> uploadVehicleImages(
            @Path("id") String vehicleId,
            @Part List<MultipartBody.Part> images);

    // Upload vehicle images (new endpoint)
    @Multipart
    @POST("api/v1/vehicles/upload-images")
    Call<VehicleImageUploadResponse> uploadVehicleImagesNew(
            @Part("vehicle_id") okhttp3.RequestBody vehicleId,
            @Part List<MultipartBody.Part> images);

    // Get all users
    @GET("api/v1/users")
    Call<ApiResponse<UsersResponse.UsersData>> getUsers();

    // Get tasks with optional filters (legacy - keeping for backward compatibility)
    @GET("api/v1/tasks")
    Call<ApiResponse<TasksResponse.TasksData>> getTasks(
            @Query("search") String search,
            @Query("status") String status,
            @Query("priority") String priority,
            @Query("assigned_to") Integer assignedTo,
            @Query("date_from") String dateFrom,
            @Query("date_to") String dateTo,
            @Query("per_page") Integer perPage);

    // New task endpoints with clean architecture

    // Get tasks list with date range and pagination
    @GET("api/v1/tasks")
    Call<ApiResponse<TasksListResponseDto>> getTasksList(
            @Query("from_date") String fromDate,
            @Query("to_date") String toDate);

    // Get single task by ID
    @GET("api/v1/tasks/{id}")
    Call<ApiResponse<TaskResponseDto>> getTask(@Path("id") Integer id);

    // Create task with multipart (fields + attachments)
    // Note: assigned_to[] and attachments[] will be built as individual parts in
    // repository
    @Multipart
    @POST("api/v1/tasks")
    Call<ApiResponse<TaskResponseDto>> createTask(@Part List<MultipartBody.Part> parts);

    // Update task with multipart (optional fields + attachments)
    // Note: assigned_to[] and attachments[] will be built as individual parts in
    // repository
    // Using POST with _method=PUT for Laravel method spoofing (more reliable than PUT with multipart)
    @Multipart
    @POST("api/v1/tasks/{id}")
    Call<ApiResponse<TaskResponseDto>> updateTask(@Path("id") int id, @Part List<MultipartBody.Part> parts);

    // Delete task
    @DELETE("api/v1/tasks/{id}")
    Call<ApiResponse<Object>> deleteTask(@Path("id") Integer id);

    // Update task status
    @POST("api/v1/tasks/{id}/status")
    Call<ApiResponse<TaskResponseDto>> updateTaskStatus(
            @Path("id") Integer id,
            @Body StatusUpdateRequest request);

    // Get users list (new endpoint matching backend structure)
    @GET("api/v1/users")
    Call<ApiResponse<UsersListResponseDto>> getUsersList();

    // Schedule endpoints
    @GET("api/v1/schedules")
    Call<ApiResponse<ScheduleListResponse>> getSchedules(
            @Query("search") String search,
            @Query("vessel_name") String vesselName,
            @Query("voyage_no") String voyageNo,
            @Query("carrier_id") Integer carrierId,
            @Query("start_port_id") Integer startPortId,
            @Query("end_port_id") Integer endPortId,
            @Query("per_page") Integer perPage,
            @Query("page") Integer page);

    @POST("api/v1/schedules")
    Call<ApiResponse<ScheduleResponse>> createSchedule(@Body CreateScheduleRequest request);

    @GET("api/v1/schedules/{id}")
    Call<ApiResponse<ScheduleResponse>> getSchedule(@Path("id") Integer id);

    @PUT("api/v1/schedules/{id}")
    Call<ApiResponse<ScheduleResponse>> updateSchedule(@Path("id") Integer id, @Body UpdateScheduleRequest request);

    @DELETE("api/v1/schedules/{id}")
    Call<ApiResponse<Object>> deleteSchedule(@Path("id") Integer id);

    // Stopover endpoints
    @POST("api/v1/schedules/{schedule_id}/stopovers")
    Call<ApiResponse<StopoverResponse>> createStopover(@Path("schedule_id") Integer scheduleId, @Body CreateStopoverRequest request);

    @GET("api/v1/stopovers/{id}")
    Call<ApiResponse<StopoverResponse>> getStopover(@Path("id") Integer id);

    @PUT("api/v1/stopovers/{id}")
    Call<ApiResponse<StopoverResponse>> updateStopover(@Path("id") Integer id, @Body UpdateStopoverRequest request);

    @DELETE("api/v1/stopovers/{id}")
    Call<ApiResponse<Object>> deleteStopover(@Path("id") Integer id);

    // Port endpoints
    @GET("api/v1/ports")
    Call<ApiResponse<PortListResponse>> getPorts(
            @Query("search") String search,
            @Query("port_type") String portType,
            @Query("per_page") Integer perPage,
            @Query("page") Integer page);

    // Shipping company endpoints
    @GET("api/v1/shipping-companies")
    Call<ApiResponse<ShippingCompanyListResponse>> getShippingCompanies(
            @Query("search") String search,
            @Query("status") String status,
            @Query("per_page") Integer perPage,
            @Query("page") Integer page);

}
