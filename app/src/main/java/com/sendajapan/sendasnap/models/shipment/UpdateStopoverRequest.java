package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class UpdateStopoverRequest implements Serializable {

    @SerializedName("port_id")
    private Integer portId;

    @SerializedName("stopover_eta")
    private String stopoverEta;

    @SerializedName("stopover_etd")
    private String stopoverEtd;

    @SerializedName("status")
    private String status;

    public UpdateStopoverRequest() {
    }

    public Integer getPortId() {
        return portId;
    }

    public void setPortId(Integer portId) {
        this.portId = portId;
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
}
