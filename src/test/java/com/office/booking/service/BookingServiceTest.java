package com.office.booking.service;

import com.office.booking.model.Booking;
import com.office.booking.model.Floor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingServiceTest {

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService();
    }

    @Test
    void testGetFloors() {
        List<Floor> floors = bookingService.getFloors();
        assertNotNull(floors);
        assertEquals(3, floors.size());
        
        // Ground floor should have no seats
        Floor groundFloor = floors.get(0);
        assertEquals(0, groundFloor.getFloorNumber());
        assertFalse(groundFloor.isHasSeats());
        
        // First floor should have seats
        Floor firstFloor = floors.get(1);
        assertEquals(1, firstFloor.getFloorNumber());
        assertTrue(firstFloor.isHasSeats());
        assertFalse(firstFloor.getSeats().isEmpty());
        
        // Second floor should have seats
        Floor secondFloor = floors.get(2);
        assertEquals(2, secondFloor.getFloorNumber());
        assertTrue(secondFloor.isHasSeats());
        assertFalse(secondFloor.getSeats().isEmpty());
    }

    @Test
    void testAddBooking() {
        String username = "testuser";
        LocalDate date = LocalDate.of(2026, 2, 15);
        int floor = 1;
        String seatId = "F1-S1";

        BookingService.BookingResult result = bookingService.addBooking(username, date, floor, seatId);
        
        assertTrue(result.isSuccess());
        assertEquals("Booking successful!", result.getMessage());
        
        List<Booking> bookings = bookingService.getUserBookings(username, 2);
        assertEquals(1, bookings.size());
        assertEquals(date, bookings.get(0).getDate());
        assertEquals(floor, bookings.get(0).getFloor());
        assertEquals(seatId, bookings.get(0).getSeatId());
    }

    @Test
    void testAddBookingDuplicateSeat() {
        String username1 = "user1";
        String username2 = "user2";
        LocalDate date = LocalDate.of(2026, 2, 15);
        int floor = 1;
        String seatId = "F1-S1";

        // First booking should succeed
        BookingService.BookingResult result1 = bookingService.addBooking(username1, date, floor, seatId);
        assertTrue(result1.isSuccess());

        // Second booking for same seat on same date should fail
        BookingService.BookingResult result2 = bookingService.addBooking(username2, date, floor, seatId);
        assertFalse(result2.isSuccess());
        assertTrue(result2.getMessage().contains("already booked"));
    }

    @Test
    void testMaximumBookingLimit() {
        String username = "testuser";
        LocalDate baseDate = LocalDate.of(2026, 2, 1);
        int floor = 1;

        // Add 10 bookings (maximum allowed)
        for (int i = 0; i < 10; i++) {
            LocalDate date = baseDate.plusDays(i);
            String seatId = "F1-S" + (i + 1);
            BookingService.BookingResult result = bookingService.addBooking(username, date, floor, seatId);
            assertTrue(result.isSuccess(), "Booking " + (i + 1) + " should succeed");
        }

        // 11th booking should fail
        LocalDate date11 = baseDate.plusDays(10);
        BookingService.BookingResult result = bookingService.addBooking(username, date11, floor, "F1-S11");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Maximum booking limit"));
    }

    @Test
    void testValidateUserBookings() {
        String username = "testuser";
        LocalDate baseDate = LocalDate.of(2026, 2, 1);
        int floor = 1;

        // Add 5 bookings (less than minimum required 6)
        for (int i = 0; i < 5; i++) {
            LocalDate date = baseDate.plusDays(i);
            String seatId = "F1-S" + (i + 1);
            bookingService.addBooking(username, date, floor, seatId);
        }

        BookingService.BookingValidationResult validation = bookingService.validateUserBookings(username, 2);
        assertFalse(validation.isValid());
        assertTrue(validation.getMessage().contains("do not meet the requirements"));

        // Add one more booking to meet minimum
        bookingService.addBooking(username, baseDate.plusDays(5), floor, "F1-S6");
        validation = bookingService.validateUserBookings(username, 2);
        assertTrue(validation.isValid());
    }

    @Test
    void testDeleteBooking() {
        String username = "testuser";
        LocalDate date = LocalDate.of(2026, 2, 15);
        int floor = 1;
        String seatId = "F1-S1";

        // Add booking
        bookingService.addBooking(username, date, floor, seatId);
        List<Booking> bookings = bookingService.getUserBookings(username, 2);
        assertEquals(1, bookings.size());

        // Delete booking
        BookingService.BookingResult result = bookingService.deleteBooking(username, date, seatId);
        assertTrue(result.isSuccess());

        bookings = bookingService.getUserBookings(username, 2);
        assertEquals(0, bookings.size());
    }

    @Test
    void testGetAvailableDates() {
        List<LocalDate> dates = bookingService.getAvailableDates(2); // February 2026
        assertNotNull(dates);
        assertEquals(28, dates.size()); // February 2026 has 28 days
        assertEquals(LocalDate.of(2026, 2, 1), dates.get(0));
        assertEquals(LocalDate.of(2026, 2, 28), dates.get(dates.size() - 1));
    }
}
