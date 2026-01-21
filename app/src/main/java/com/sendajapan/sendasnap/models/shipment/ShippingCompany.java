package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ShippingCompany implements Serializable {

    @SerializedName("id")
    private Integer id;

    @SerializedName("line_name")
    private String lineName;

    @SerializedName("status")
    private String status;

    public ShippingCompany() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
