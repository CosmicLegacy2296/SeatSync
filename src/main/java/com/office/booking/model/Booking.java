package com.office.booking.model;

import java.time.LocalDate;
import java.util.Objects;

public class Booking {
    private String companyId;
    private String username;
    private LocalDate date;
    private int floor;
    private String seatId;
    private int month;
    private int year;

    public Booking() {
    }

    public Booking(String companyId, String username, LocalDate date, int floor, String seatId) {
        this.companyId = companyId;
        this.username = username;
        this.date = date;
        this.floor = floor;
        this.seatId = seatId;
        this.month = date.getMonthValue();
        this.year = date.getYear();
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
        this.month = date.getMonthValue();
        this.year = date.getYear();
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return floor == booking.floor &&
                Objects.equals(companyId, booking.companyId) &&
                Objects.equals(username, booking.username) &&
                Objects.equals(date, booking.date) &&
                Objects.equals(seatId, booking.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, username, date, floor, seatId);
    }
}
