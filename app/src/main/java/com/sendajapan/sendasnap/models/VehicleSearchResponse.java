package com.sendajapan.sendasnap.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class VehicleSearchResponse {

    @SerializedName("success")
    private Boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private VehicleSearchData data;

    @SerializedName("meta")
    private MetaData meta;

    public VehicleSearchResponse() {
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public VehicleSearchData getData() {
        return data;
    }

    public void setData(VehicleSearchData data) {
        this.data = data;
    }

    public MetaData getMeta() {
        return meta;
    }

    public void setMeta(MetaData meta) {
        this.meta = meta;
    }

    public static class VehicleSearchData {

        @SerializedName("vehicles")
        private List<Vehicle> vehicles;

        @SerializedName("company")
        private String company;

        public VehicleSearchData() {
        }

        public List<Vehicle> getVehicles() {
            return vehicles;
        }

        public void setVehicles(List<Vehicle> vehicles) {
            this.vehicles = vehicles;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }
    }
}
