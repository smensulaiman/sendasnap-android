package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CreateScheduleRequest implements Serializable {

    @SerializedName("vessel_name")
    private String vesselName;

    @SerializedName("voyage_no")
    private String voyageNo;

    @SerializedName("carrier_1_id")
    private Integer carrier1Id;

    @SerializedName("carrier_2_id")
    private Integer carrier2Id;

    @SerializedName("carrier_3_id")
    private Integer carrier3Id;

    @SerializedName("start_port_id")
    private Integer startPortId;

    @SerializedName("end_port_id")
    private Integer endPortId;

    @SerializedName("eta")
    private String eta;

    @SerializedName("status")
    private String status;

    @SerializedName("comment")
    private String comment;

    public CreateScheduleRequest() {
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

    public Integer getCarrier1Id() {
        return carrier1Id;
    }

    public void setCarrier1Id(Integer carrier1Id) {
        this.carrier1Id = carrier1Id;
    }

    public Integer getCarrier2Id() {
        return carrier2Id;
    }

    public void setCarrier2Id(Integer carrier2Id) {
        this.carrier2Id = carrier2Id;
    }

    public Integer getCarrier3Id() {
        return carrier3Id;
    }

    public void setCarrier3Id(Integer carrier3Id) {
        this.carrier3Id = carrier3Id;
    }

    public Integer getStartPortId() {
        return startPortId;
    }

    public void setStartPortId(Integer startPortId) {
        this.startPortId = startPortId;
    }

    public Integer getEndPortId() {
        return endPortId;
    }

    public void setEndPortId(Integer endPortId) {
        this.endPortId = endPortId;
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
}
