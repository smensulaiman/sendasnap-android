package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ScheduleListResponse implements Serializable {

    @SerializedName("schedules")
    private List<Schedule> schedules;

    @SerializedName("pagination")
    private Pagination pagination;

    public ScheduleListResponse() {
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
