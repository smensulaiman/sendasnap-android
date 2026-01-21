package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ScheduleResponse implements Serializable {

    @SerializedName("schedule")
    private Schedule schedule;

    public ScheduleResponse() {
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }
}
