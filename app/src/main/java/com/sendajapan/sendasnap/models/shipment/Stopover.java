package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Stopover implements Serializable {

    @SerializedName("id")
    private Integer id;

    @SerializedName("schedule_id")
    private Integer scheduleId;

    @SerializedName("port")
    private Port port;

    @SerializedName("stopover_eta")
    private String stopoverEta;

    @SerializedName("stopover_etd")
    private String stopoverEtd;

    @SerializedName("status")
    private String status;

    @SerializedName("added_by")
    private AddedBy addedBy;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public Stopover() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Port getPort() {
        return port;
    }

    public void setPort(Port port) {
        this.port = port;
    }

    public String getStopoverEta() {
        return stopoverEta;
    }

    public void setStopoverEta(String stopoverEta) {
        this.stopoverEta = stopoverEta;
    }

    public String getStopoverEtd() {
        return stopoverEtd;
    }

    public void setStopoverEtd(String stopoverEtd) {
        this.stopoverEtd = stopoverEtd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AddedBy getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(AddedBy addedBy) {
        this.addedBy = addedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class AddedBy implements Serializable {
        @SerializedName("id")
        private Integer id;

        @SerializedName("name")
        private String name;

        @SerializedName("email")
        private String email;

        public AddedBy() {
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
