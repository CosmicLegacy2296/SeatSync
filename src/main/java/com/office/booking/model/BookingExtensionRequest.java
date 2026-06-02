package com.office.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "booking_extension_requests")
public class BookingExtensionRequest {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String companyId;
    private String username;
    private int requestedDays;
    @Column(name = "extension_month")
    private int month;
    @Column(name = "extension_year")
    private int year;
    private String status;

    public BookingExtensionRequest() {
    }

    public BookingExtensionRequest(String companyId, String username, int requestedDays, int month, int year) {
        this.companyId = companyId;
        this.username = username;
        this.requestedDays = requestedDays;
        this.month = month;
        this.year = year;
        this.status = PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getRequestedDays() {
        return requestedDays;
    }

    public void setRequestedDays(int requestedDays) {
        this.requestedDays = requestedDays;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingExtensionRequest that = (BookingExtensionRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
