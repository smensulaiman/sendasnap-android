package com.sendajapan.sendasnap.models.shipment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Pagination implements Serializable {

    @SerializedName("current_page")
    private Integer currentPage;

    @SerializedName("last_page")
    private Integer lastPage;

    @SerializedName("per_page")
    private Integer perPage;

    @SerializedName("total")
    private Integer total;

    public Pagination() {
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getLastPage() {
        return lastPage;
    }

    public void setLastPage(Integer lastPage) {
        this.lastPage = lastPage;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
