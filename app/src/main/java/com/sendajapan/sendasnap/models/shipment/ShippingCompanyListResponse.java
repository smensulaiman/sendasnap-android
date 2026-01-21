package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ShippingCompanyListResponse implements Serializable {

    @SerializedName("shipping_companies")
    private List<ShippingCompany> shippingCompanies;

    @SerializedName("pagination")
    private Pagination pagination;

    public ShippingCompanyListResponse() {
    }

    public List<ShippingCompany> getShippingCompanies() {
        return shippingCompanies;
    }

    public void setShippingCompanies(List<ShippingCompany> shippingCompanies) {
        this.shippingCompanies = shippingCompanies;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
