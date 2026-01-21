package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Schedule implements Serializable {

    @SerializedName("id")
    private Integer id;

    @SerializedName("vessel_name")
    private String vesselName;

    @SerializedName("voyage_no")
    private String voyageNo;

    @SerializedName("carrier_1")
    private Carrier carrier1;

    @SerializedName("carrier_2")
    private Carrier carrier2;

    @SerializedName("carrier_3")
    private Carrier carrier3;

    @SerializedName("start_port")
    private Port startPort;

    @SerializedName("end_port")
    private Port endPort;

    @SerializedName("eta")
    private String eta;

    @SerializedName("status")
    private String status;

    @SerializedName("comment")
    private String comment;

    @SerializedName("stopovers")
    private List<Stopover> stopovers;

    @SerializedName("added_by")
    private AddedBy addedBy;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public Schedule() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVesselName() {
        return vesselName;
    }

    public void setVesselName(String vesselName) {
        this.vesselName = vesselName;
    }

    public String getVoyageNo() {
        return voyageNo;
    }

    public void setVoyageNo(String voyageNo) {
        this.voyageNo = voyageNo;
    }

    public Carrier getCarrier1() {
        return carrier1;
    }

    public void setCarrier1(Carrier carrier1) {
        this.carrier1 = carrier1;
    }

    public Carrier getCarrier2() {
        return carrier2;
    }

    public void setCarrier2(Carrier carrier2) {
        this.carrier2 = carrier2;
    }

    public Carrier getCarrier3() {
        return carrier3;
    }

    public void setCarrier3(Carrier carrier3) {
        this.carrier3 = carrier3;
    }

    public Port getStartPort() {
        return startPort;
    }

    public void setStartPort(Port startPort) {
        this.startPort = startPort;
    }

    public Port getEndPort() {
        return endPort;
    }

    public void setEndPort(Port endPort) {
        this.endPort = endPort;
    }

    public String getEta() {
        return eta;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Stopover> getStopovers() {
        return stopovers;
    }

    public void setStopovers(List<Stopover> stopovers) {
        this.stopovers = stopovers;
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
