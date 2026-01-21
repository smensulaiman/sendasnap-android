package com.sendajapan.sendasnap.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.sendajapan.sendasnap.data.repository.ScheduleRepositoryImpl;
import com.sendajapan.sendasnap.domain.repository.ScheduleRepository;
import com.sendajapan.sendasnap.models.shipment.CreateScheduleRequest;
import com.sendajapan.sendasnap.models.shipment.Port;
import com.sendajapan.sendasnap.models.shipment.Schedule;
import com.sendajapan.sendasnap.models.shipment.ScheduleListResponse;
import com.sendajapan.sendasnap.models.shipment.ShippingCompany;
import com.sendajapan.sendasnap.models.shipment.UpdateScheduleRequest;

import java.util.List;

public class ScheduleViewModel extends AndroidViewModel {

    private final ScheduleRepository scheduleRepository;

    public ScheduleViewModel(@NonNull Application application) {
        super(application);
        this.scheduleRepository = new ScheduleRepositoryImpl(application);
    }

    public void getSchedules(String search, String vesselName, String voyageNo, Integer carrierId,
                            Integer startPortId, Integer endPortId, Integer perPage, Integer page,
                            ScheduleCallback<ScheduleListResponse> callback) {
        scheduleRepository.getSchedules(search, vesselName, voyageNo, carrierId, startPortId, endPortId, perPage, page,
                new ScheduleRepository.ScheduleRepositoryCallback<ScheduleListResponse>() {
                    @Override
                    public void onSuccess(ScheduleListResponse result) {
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(String message, int errorCode) {
                        callback.onError(message, errorCode);
                    }
                });
    }

    public void getSchedule(Integer id, ScheduleCallback<Schedule> callback) {
        scheduleRepository.getSchedule(id, new ScheduleRepository.ScheduleRepositoryCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, int errorCode) {
                callback.onError(message, errorCode);
            }
        });
    }

    public void createSchedule(CreateScheduleRequest request, ScheduleCallback<Schedule> callback) {
        scheduleRepository.createSchedule(request, new ScheduleRepository.ScheduleRepositoryCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, int errorCode) {
                callback.onError(message, errorCode);
            }
        });
    }

    public void updateSchedule(Integer id, UpdateScheduleRequest request, ScheduleCallback<Schedule> callback) {
        scheduleRepository.updateSchedule(id, request, new ScheduleRepository.ScheduleRepositoryCallback<Schedule>() {
            @Override
            public void onSuccess(Schedule result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, int errorCode) {
                callback.onError(message, errorCode);
            }
        });
    }

    public void deleteSchedule(Integer id, ScheduleCallback<Void> callback) {
        scheduleRepository.deleteSchedule(id, new ScheduleRepository.ScheduleRepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message, int errorCode) {
                callback.onError(message, errorCode);
            }
        });
    }

    public void getPorts(String search, String portType, Integer perPage, Integer page,
                        ScheduleCallback<List<Port>> callback) {
        scheduleRepository.getPorts(search, portType, perPage, page,
                new ScheduleRepository.ScheduleRepositoryCallback<List<Port>>() {
                    @Override
                    public void onSuccess(List<Port> result) {
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(String message, int errorCode) {
                        callback.onError(message, errorCode);
                    }
                });
    }

    public void getShippingCompanies(String search, String status, Integer perPage, Integer page,
                                     ScheduleCallback<List<ShippingCompany>> callback) {
        scheduleRepository.getShippingCompanies(search, status, perPage, page,
                new ScheduleRepository.ScheduleRepositoryCallback<List<ShippingCompany>>() {
                    @Override
                    public void onSuccess(List<ShippingCompany> result) {
                        callback.onSuccess(result);
                    }

                    @Override
                    public void onError(String message, int errorCode) {
                        callback.onError(message, errorCode);
                    }
                });
    }

    public interface ScheduleCallback<T> {
        void onSuccess(T result);
        void onError(String message, int errorCode);
    }
}
