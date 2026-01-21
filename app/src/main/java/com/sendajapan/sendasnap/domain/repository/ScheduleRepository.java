package com.sendajapan.sendasnap.domain.repository;

import com.sendajapan.sendasnap.models.shipment.CreateScheduleRequest;
import com.sendajapan.sendasnap.models.shipment.CreateStopoverRequest;
import com.sendajapan.sendasnap.models.shipment.Port;
import com.sendajapan.sendasnap.models.shipment.Schedule;
import com.sendajapan.sendasnap.models.shipment.ScheduleListResponse;
import com.sendajapan.sendasnap.models.shipment.ShippingCompany;
import com.sendajapan.sendasnap.models.shipment.Stopover;
import com.sendajapan.sendasnap.models.shipment.UpdateScheduleRequest;
import com.sendajapan.sendasnap.models.shipment.UpdateStopoverRequest;

import java.util.List;

public interface ScheduleRepository {

    void getSchedules(String search, String vesselName, String voyageNo, Integer carrierId,
                     Integer startPortId, Integer endPortId, Integer perPage, Integer page,
                     ScheduleRepositoryCallback<ScheduleListResponse> callback);

    void getSchedule(Integer id, ScheduleRepositoryCallback<Schedule> callback);

    void createSchedule(CreateScheduleRequest request, ScheduleRepositoryCallback<Schedule> callback);

    void updateSchedule(Integer id, UpdateScheduleRequest request, ScheduleRepositoryCallback<Schedule> callback);

    void deleteSchedule(Integer id, ScheduleRepositoryCallback<Void> callback);

    void createStopover(Integer scheduleId, CreateStopoverRequest request, ScheduleRepositoryCallback<Stopover> callback);

    void getStopover(Integer id, ScheduleRepositoryCallback<Stopover> callback);

    void updateStopover(Integer id, UpdateStopoverRequest request, ScheduleRepositoryCallback<Stopover> callback);

    void deleteStopover(Integer id, ScheduleRepositoryCallback<Void> callback);

    void getPorts(String search, String portType, Integer perPage, Integer page,
                  ScheduleRepositoryCallback<List<Port>> callback);

    void getShippingCompanies(String search, String status, Integer perPage, Integer page,
                              ScheduleRepositoryCallback<List<ShippingCompany>> callback);

    interface ScheduleRepositoryCallback<T> {
        void onSuccess(T result);

        void onError(String message, int errorCode);
    }
}
