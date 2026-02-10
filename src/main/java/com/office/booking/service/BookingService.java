package com.office.booking.service;

import com.office.booking.model.Booking;
import com.office.booking.model.Floor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private static final int MIN_REQUIRED_DAYS = 6;
    private static final int MAX_ALLOWED_DAYS = 10;
    private static final int YEAR = 2026;

    private final Map<String, List<Booking>> userBookings = new ConcurrentHashMap<>();
    private final Map<String, Booking> seatBookings = new ConcurrentHashMap<>(); // Key: date-seatId
    private final List<Floor> floors;

    public BookingService() {
        // Initialize floors: Ground (0) has no seats, First (1) and Second (2) have seats
        floors = new ArrayList<>();
        floors.add(new Floor(0, false)); // Ground floor - no seats
        floors.add(new Floor(1, true));  // First floor - has seats
        floors.add(new Floor(2, true));  // Second floor - has seats
    }

    public List<Floor> getFloors() {
        return floors;
    }

    public List<Booking> getUserBookings(String username, int month) {
        return userBookings.getOrDefault(username, new ArrayList<>())
                .stream()
                .filter(b -> b.getMonth() == month && b.getYear() == YEAR)
                .collect(Collectors.toList());
    }

    public List<Booking> getAllUserBookings(String username) {
        return userBookings.getOrDefault(username, new ArrayList<>());
    }

    public BookingResult addBooking(String username, LocalDate date, int floor, String seatId) {
        // Check if seat is already booked for this date
        String seatKey = date.toString() + "-" + seatId;
        if (seatBookings.containsKey(seatKey)) {
            return new BookingResult(false, "This seat is already booked for the selected date.");
        }

        // Check if user already has a booking for this date
        List<Booking> userBookingsForMonth = getUserBookings(username, date.getMonthValue());
        boolean hasBookingForDate = userBookingsForMonth.stream()
                .anyMatch(b -> b.getDate().equals(date));

        if (hasBookingForDate) {
            return new BookingResult(false, "You already have a booking for this date.");
        }

        // Check maximum booking limit (10 days)
        if (userBookingsForMonth.size() >= MAX_ALLOWED_DAYS) {
            return new BookingResult(false, "Maximum booking limit reached. Please delete a booking to add another.");
        }

        // Validate floor has seats
        Floor selectedFloor = floors.stream()
                .filter(f -> f.getFloorNumber() == floor)
                .findFirst()
                .orElse(null);

        if (selectedFloor == null || !selectedFloor.isHasSeats()) {
            return new BookingResult(false, "Selected floor does not have seats.");
        }

        // Validate seat exists on floor
        if (!selectedFloor.getSeats().contains(seatId)) {
            return new BookingResult(false, "Invalid seat ID for the selected floor.");
        }

        // Create and save booking
        Booking booking = new Booking(username, date, floor, seatId);
        userBookings.computeIfAbsent(username, k -> new ArrayList<>()).add(booking);
        seatBookings.put(seatKey, booking);

        return new BookingResult(true, "Booking successful!");
    }

    public BookingResult deleteBooking(String username, LocalDate date, String seatId) {
        String seatKey = date.toString() + "-" + seatId;
        Booking booking = seatBookings.get(seatKey);

        if (booking == null || !booking.getUsername().equals(username)) {
            return new BookingResult(false, "Booking not found or you don't have permission to delete it.");
        }

        userBookings.get(username).remove(booking);
        seatBookings.remove(seatKey);

        return new BookingResult(true, "Booking deleted successfully!");
    }

    public BookingValidationResult validateUserBookings(String username, int month) {
        List<Booking> bookings = getUserBookings(username, month);
        int bookingCount = bookings.size();

        if (bookingCount < MIN_REQUIRED_DAYS) {
            String monthName = getMonthName(month);
            return new BookingValidationResult(false, 
                "You do not meet the requirements for " + monthName + ". You need at least " + 
                MIN_REQUIRED_DAYS + " bookings, but you have " + bookingCount + ".");
        }

        return new BookingValidationResult(true, "You meet the requirements for " + getMonthName(month) + ".");
    }

    public Set<LocalDate> getBookedDatesForSeat(String seatId, int month) {
        return seatBookings.values().stream()
                .filter(b -> b.getSeatId().equals(seatId) && 
                            b.getMonth() == month && 
                            b.getYear() == YEAR)
                .map(Booking::getDate)
                .collect(Collectors.toSet());
    }

    public List<LocalDate> getAvailableDates(int month) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate startDate = LocalDate.of(YEAR, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    private String getMonthName(int month) {
        String[] monthNames = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return monthNames[month];
    }

    // Admin & reporting helpers

    /**
     * Returns all bookings for a given month across all users.
     */
    public List<Booking> getBookingsForMonth(int month) {
        return seatBookings.values().stream()
                .filter(b -> b.getMonth() == month && b.getYear() == YEAR)
                .sorted(Comparator
                        .comparing(Booking::getDate)
                        .thenComparing(Booking::getFloor)
                        .thenComparing(Booking::getSeatId)
                        .thenComparing(Booking::getUsername))
                .collect(Collectors.toList());
    }

    /**
     * Admin-only deletion of a booking, regardless of owning user.
     */
    public BookingResult adminDeleteBooking(LocalDate date, String seatId) {
        String seatKey = date.toString() + "-" + seatId;
        Booking booking = seatBookings.get(seatKey);

        if (booking == null) {
            return new BookingResult(false, "Booking not found.");
        }

        List<Booking> bookingsForUser = userBookings.get(booking.getUsername());
        if (bookingsForUser != null) {
            bookingsForUser.remove(booking);
        }

        seatBookings.remove(seatKey);
        return new BookingResult(true, "Booking deleted successfully.");
    }

    public static class BookingResult {
        private final boolean success;
        private final String message;

        public BookingResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class BookingValidationResult {
        private final boolean valid;
        private final String message;

        public BookingValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
