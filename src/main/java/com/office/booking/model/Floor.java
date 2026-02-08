package com.office.booking.model;

import java.util.ArrayList;
import java.util.List;

public class Floor {
    private int floorNumber;
    private List<String> seats;
    private boolean hasSeats;

    public Floor() {
        this.seats = new ArrayList<>();
    }

    public Floor(int floorNumber, boolean hasSeats) {
        this.floorNumber = floorNumber;
        this.hasSeats = hasSeats;
        this.seats = new ArrayList<>();
        if (hasSeats) {
            initializeSeats();
        }
    }

    private void initializeSeats() {
        // Create 20 seats per floor (you can adjust this number)
        for (int i = 1; i <= 20; i++) {
            seats.add("F" + floorNumber + "-S" + i);
        }
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
    }

    public boolean isHasSeats() {
        return hasSeats;
    }

    public void setHasSeats(boolean hasSeats) {
        this.hasSeats = hasSeats;
    }
}
