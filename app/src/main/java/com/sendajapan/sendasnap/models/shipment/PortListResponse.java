package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class PortListResponse implements Serializable {

    @SerializedName("ports")
    private List<Port> ports;

    @SerializedName("pagination")
    private Pagination pagination;

    public PortListResponse() {
    }

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
