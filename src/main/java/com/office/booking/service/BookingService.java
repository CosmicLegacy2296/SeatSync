package com.office.booking.service;

import com.office.booking.model.Booking;
import com.office.booking.model.Floor;
import com.office.booking.repository.BookingRepository;
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
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private BookingRepository bookingRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private WorkspaceLayoutService workspaceLayoutService;
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

    public List<Floor> getFloors(String companyId) {
        if (workspaceLayoutService != null && companyId != null && !companyId.isBlank()) {
            return workspaceLayoutService.getFloorsForCompany(companyId);
        }
        return floors;
    }

    public List<Booking> getUserBookings(String companyId, String username, int month) {
        if (bookingRepository != null) {
            return bookingRepository.findByCompanyIdAndUsernameAndMonthAndYear(companyId, username, month, YEAR);
        }
        return userBookings.getOrDefault(userKey(companyId, username), new ArrayList<>())
                .stream()
                .filter(b -> b.getMonth() == month && b.getYear() == YEAR)
                .collect(Collectors.toList());
    }

    public List<Booking> getAllUserBookings(String companyId, String username) {
        if (bookingRepository != null) {
            return bookingRepository.findByCompanyIdAndUsername(companyId, username);
        }
        return userBookings.getOrDefault(userKey(companyId, username), new ArrayList<>());
    }

    public BookingResult addBooking(String companyId, String username, LocalDate date, int floor, String seatId) {
        // Check if seat is already booked for this date
        boolean seatTaken = bookingRepository != null
                ? bookingRepository.existsByCompanyIdAndDateAndSeatId(companyId, date, seatId)
                : seatBookings.containsKey(seatKey(companyId, date, seatId));
        if (seatTaken) {
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
            ? extensionRequestService.getApprovedMaxForUserMonthYear(companyId, username, date.getMonthValue(), date.getYear())
                : DEFAULT_MAX_DAYS;
        if (userBookingsForMonth.size() >= maxAllowed) {
            return new BookingResult(false, "Maximum booking limit reached. Please delete a booking to add another.");
        }

        if (workspaceLayoutService != null) {
            if (!workspaceLayoutService.isValidSeat(companyId, floor, seatId)) {
                return new BookingResult(false, "Invalid seat ID for the selected floor.");
            }
        } else {
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
        }

        // Create and save booking
        Booking booking = new Booking(companyId, username, date, floor, seatId);
        if (bookingRepository != null) {
            bookingRepository.save(booking);
        } else {
            String seatKey = seatKey(companyId, date, seatId);
            userBookings.computeIfAbsent(userKey(companyId, username), k -> new ArrayList<>()).add(booking);
            seatBookings.put(seatKey, booking);
        }

        return new BookingResult(true, "Booking successful!");
    }

    public BookingResult deleteBooking(String companyId, String username, LocalDate date, String seatId) {
        Booking booking;
        if (bookingRepository != null) {
            booking = bookingRepository.findByCompanyIdAndDateAndSeatId(companyId, date, seatId).orElse(null);
        } else {
            booking = seatBookings.get(seatKey(companyId, date, seatId));
        }

        if (booking == null || !booking.getUsername().equals(username)) {
            return new BookingResult(false, "Booking not found or you don't have permission to delete it.");
        }

        if (bookingRepository != null) {
            bookingRepository.delete(booking);
        } else {
            userBookings.get(userKey(companyId, username)).remove(booking);
            seatBookings.remove(seatKey(companyId, date, seatId));
        }

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

        // Check if new seat is already booked by someone else
        if (!Objects.equals(existingBooking.getSeatId(), newSeatId)) {
            boolean seatTakenByAnother;
            if (bookingRepository != null) {
                seatTakenByAnother = bookingRepository.findByCompanyIdAndDateAndSeatId(companyId, date, newSeatId)
                        .map(b -> !username.equals(b.getUsername()))
                        .orElse(false);
            } else {
                String newSeatKey = seatKey(companyId, date, newSeatId);
                seatTakenByAnother = seatBookings.containsKey(newSeatKey)
                        && !seatBookings.get(newSeatKey).getUsername().equals(username);
            }
            if (seatTakenByAnother) {
                return new BookingResult(false, "This seat is already booked for the selected date.");
            }
        }

        if (workspaceLayoutService != null) {
            if (!workspaceLayoutService.isValidSeat(companyId, floor, newSeatId)) {
                return new BookingResult(false, "Invalid seat ID for the selected floor.");
            }
        } else {
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
        }

        String oldSeatId = existingBooking.getSeatId();

        // Update booking
        existingBooking.setFloor(floor);
        existingBooking.setSeatId(newSeatId);

        if (bookingRepository != null) {
            bookingRepository.save(existingBooking);
        } else {
            String oldSeatKey = seatKey(companyId, date, oldSeatId);
            String newSeatKey = seatKey(companyId, date, newSeatId);
            seatBookings.remove(oldSeatKey);
            seatBookings.put(newSeatKey, existingBooking);
        }

        return new BookingResult(true, "Booking updated successfully!");
    }

    public Booking getBookingForUserAndDate(String companyId, String username, LocalDate date) {
        if (bookingRepository != null) {
            return bookingRepository.findByCompanyIdAndUsernameAndDate(companyId, username, date).orElse(null);
        }
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
        if (bookingRepository != null) {
            return bookingRepository.findByCompanyIdAndSeatIdAndMonthAndYear(companyId, seatId, month, YEAR).stream()
                    .map(Booking::getDate)
                    .collect(Collectors.toSet());
        }
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
        if (bookingRepository != null) {
            return bookingRepository.findByCompanyIdAndDate(companyId, date).stream()
                    .map(Booking::getSeatId)
                    .collect(Collectors.toSet());
        }
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
        if (bookingRepository != null) {
            return bookingRepository.findByMonthAndYear(month, YEAR).stream()
                .sorted(Comparator
                    .comparing(Booking::getDate)
                    .thenComparing(Booking::getFloor)
                    .thenComparing(Booking::getSeatId)
                    .thenComparing(Booking::getUsername))
                .collect(Collectors.toList());
        }
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
    public BookingResult adminDeleteBooking(String companyId, LocalDate date, String seatId) {
        if (bookingRepository != null) {
            Booking booking = bookingRepository.findByCompanyIdAndDateAndSeatId(companyId, date, seatId).orElse(null);
            if (booking == null) {
                return new BookingResult(false, "Booking not found.");
            }
            bookingRepository.delete(booking);
            return new BookingResult(true, "Booking deleted successfully.");
        }

        List<String> keysToRemove = seatBookings.entrySet().stream()
                .filter(e -> companyId.equals(e.getValue().getCompanyId())
                        && e.getValue().getDate().equals(date)
                        && e.getValue().getSeatId().equals(seatId))
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

    public long getActiveBookingCountForCompany(String companyId) {
        if (bookingRepository != null) {
            return bookingRepository.countByCompanyId(companyId);
        }
        return seatBookings.values().stream()
                .filter(b -> companyId.equals(b.getCompanyId()))
                .count();
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
