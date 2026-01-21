package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class StopoverResponse implements Serializable {

    @SerializedName("stopover")
    private Stopover stopover;

    public StopoverResponse() {
    }

    public Stopover getStopover() {
        return stopover;
    }

    public void setStopover(Stopover stopover) {
        this.stopover = stopover;
    }
}
