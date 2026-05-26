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
    private static final int DEFAULT_MAX_DAYS = 10;
    private static final int YEAR = 2026;

    private final Map<String, List<Booking>> userBookings = new ConcurrentHashMap<>();
    private com.office.booking.service.ExtensionRequestService extensionRequestService;

    @org.springframework.beans.factory.annotation.Autowired
    public void setExtensionRequestService(com.office.booking.service.ExtensionRequestService extensionRequestService) {
        this.extensionRequestService = extensionRequestService;
    }
    // Keyed by "<companyId>:<date>-<seatId>"
    private final Map<String, Booking> seatBookings = new ConcurrentHashMap<>();
    private final List<Floor> floors;

    private String userKey(String companyId, String username) {
        return companyId + ":" + username;
    }

    private String seatKey(String companyId, LocalDate date, String seatId) {
        return companyId + ":" + date.toString() + "-" + seatId;
    }

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

    public List<Booking> getUserBookings(String companyId, String username, int month) {
        return userBookings.getOrDefault(userKey(companyId, username), new ArrayList<>())
                .stream()
                .filter(b -> b.getMonth() == month && b.getYear() == YEAR)
                .collect(Collectors.toList());
    }

    public List<Booking> getAllUserBookings(String companyId, String username) {
        return userBookings.getOrDefault(userKey(companyId, username), new ArrayList<>());
    }

    public BookingResult addBooking(String companyId, String username, LocalDate date, int floor, String seatId) {
        // Check if seat is already booked for this date
        String seatKey = seatKey(companyId, date, seatId);
        if (seatBookings.containsKey(seatKey)) {
            return new BookingResult(false, "This seat is already booked for the selected date.");
        }

        // Check if user already has a booking for this date
        List<Booking> userBookingsForMonth = getUserBookings(companyId, username, date.getMonthValue());
        boolean hasBookingForDate = userBookingsForMonth.stream()
                .anyMatch(b -> b.getDate().equals(date));

        if (hasBookingForDate) {
            return new BookingResult(false, "You already have a booking for this date.");
        }

        // Check maximum booking limit (month-specific: default 10, or approved extension for this month/year)
        int maxAllowed = extensionRequestService != null
                ? extensionRequestService.getApprovedMaxForUserMonthYear(username, date.getMonthValue(), date.getYear())
                : DEFAULT_MAX_DAYS;
        if (userBookingsForMonth.size() >= maxAllowed) {
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
        Booking booking = new Booking(companyId, username, date, floor, seatId);
        userBookings.computeIfAbsent(userKey(companyId, username), k -> new ArrayList<>()).add(booking);
        seatBookings.put(seatKey, booking);

        return new BookingResult(true, "Booking successful!");
    }

    public BookingResult deleteBooking(String companyId, String username, LocalDate date, String seatId) {
        String seatKey = seatKey(companyId, date, seatId);
        Booking booking = seatBookings.get(seatKey);

        if (booking == null || !booking.getUsername().equals(username)) {
            return new BookingResult(false, "Booking not found or you don't have permission to delete it.");
        }

        userBookings.get(userKey(companyId, username)).remove(booking);
        seatBookings.remove(seatKey);

        return new BookingResult(true, "Booking deleted successfully!");
    }

    public BookingResult updateBooking(String companyId, String username, LocalDate date, int floor, String newSeatId) {
        // Find existing booking for this user and date
        List<Booking> userBookingsForMonth = getUserBookings(companyId, username, date.getMonthValue());
        Booking existingBooking = userBookingsForMonth.stream()
                .filter(b -> b.getDate().equals(date))
                .findFirst()
                .orElse(null);

        if (existingBooking == null) {
            return new BookingResult(false, "No existing booking found for this date.");
        }

        String oldSeatKey = seatKey(companyId, date, existingBooking.getSeatId());
        String newSeatKey = seatKey(companyId, date, newSeatId);

        // Check if new seat is already booked by someone else
        if (seatBookings.containsKey(newSeatKey) && !seatBookings.get(newSeatKey).getUsername().equals(username)) {
            return new BookingResult(false, "This seat is already booked for the selected date.");
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
        if (!selectedFloor.getSeats().contains(newSeatId)) {
            return new BookingResult(false, "Invalid seat ID for the selected floor.");
        }

        // Remove old booking from seatBookings map
        seatBookings.remove(oldSeatKey);

        // Update booking
        existingBooking.setFloor(floor);
        existingBooking.setSeatId(newSeatId);

        // Add updated booking to seatBookings map
        seatBookings.put(newSeatKey, existingBooking);

        return new BookingResult(true, "Booking updated successfully!");
    }

    public Booking getBookingForUserAndDate(String companyId, String username, LocalDate date) {
        List<Booking> userBookingsForMonth = getUserBookings(companyId, username, date.getMonthValue());
        return userBookingsForMonth.stream()
                .filter(b -> b.getDate().equals(date))
                .findFirst()
                .orElse(null);
    }

    public BookingValidationResult validateUserBookings(String companyId, String username, int month) {
        List<Booking> bookings = getUserBookings(companyId, username, month);
        int bookingCount = bookings.size();

        if (bookingCount < MIN_REQUIRED_DAYS) {
            String monthName = getMonthName(month);
            return new BookingValidationResult(false, 
                "You do not meet the requirements for " + monthName + ". You need at least " + 
                MIN_REQUIRED_DAYS + " bookings, but you have " + bookingCount + ".");
        }

        return new BookingValidationResult(true, "You meet the requirements for " + getMonthName(month) + ".");
    }

    public Set<LocalDate> getBookedDatesForSeat(String companyId, String seatId, int month) {
        return seatBookings.values().stream()
                .filter(b -> companyId.equals(b.getCompanyId()) &&
                             b.getSeatId().equals(seatId) &&
                             b.getMonth() == month &&
                             b.getYear() == YEAR)
                .map(Booking::getDate)
                .collect(Collectors.toSet());
    }

    /**
     * Returns all seat IDs booked for a specific date.
     */
    public Set<String> getBookedSeatsForDate(String companyId, LocalDate date) {
        return seatBookings.values().stream()
                .filter(b -> companyId.equals(b.getCompanyId()) && b.getDate().equals(date))
                .map(Booking::getSeatId)
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
        List<String> keysToRemove = seatBookings.entrySet().stream()
                .filter(e -> e.getValue().getDate().equals(date) && e.getValue().getSeatId().equals(seatId))
                .map(Map.Entry::getKey)
                .toList();

        if (keysToRemove.isEmpty()) {
            return new BookingResult(false, "Booking not found.");
        }

        for (String key : keysToRemove) {
            Booking booking = seatBookings.get(key);
            if (booking != null) {
                String userKey = userKey(booking.getCompanyId(), booking.getUsername());
                List<Booking> bookingsForUser = userBookings.get(userKey);
                if (bookingsForUser != null) {
                    bookingsForUser.remove(booking);
                }
            }
            seatBookings.remove(key);
        }

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
