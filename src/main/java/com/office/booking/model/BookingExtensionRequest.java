package com.office.booking.model;

import java.util.Objects;

public class BookingExtensionRequest {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private Long id;
    private String username;
    private int requestedDays;
    private int month;
    private int year;
    private String status;

    private static long nextId = 1;

    public BookingExtensionRequest() {
    }

    public BookingExtensionRequest(String username, int requestedDays, int month, int year) {
        this.id = nextId++;
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
