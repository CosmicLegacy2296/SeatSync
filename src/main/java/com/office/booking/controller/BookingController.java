package com.office.booking.controller;

import com.office.booking.model.Booking;
import com.office.booking.model.Floor;
import com.office.booking.service.BookingService;
import com.office.booking.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Controller
public class BookingController {
    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", username);
        return "dashboard";
    }

    @GetMapping("/calendar/{month}")
    public String calendar(@PathVariable int month, 
                          HttpSession session, 
                          Model model,
                          RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        if (month < 2 || month > 12) {
            redirectAttributes.addFlashAttribute("error", "Invalid month selected");
            return "redirect:/dashboard";
        }

        List<LocalDate> availableDates = bookingService.getAvailableDates(month);
        List<Booking> userBookings = bookingService.getUserBookings(username, month);
        BookingService.BookingValidationResult validation = bookingService.validateUserBookings(username, month);

        model.addAttribute("month", month);
        model.addAttribute("monthName", getMonthName(month));
        model.addAttribute("year", 2026);
        model.addAttribute("availableDates", availableDates);
        model.addAttribute("userBookings", userBookings);
        model.addAttribute("validation", validation);
        model.addAttribute("username", username);

        return "calendar";
    }

    @GetMapping("/select-seats/{month}")
    public String selectSeats(@PathVariable int month,
                             @RequestParam(required = false) String selectedDates,
                             HttpSession session,
                             Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        if (month < 2 || month > 12) {
            return "redirect:/dashboard";
        }

        List<Floor> floors = bookingService.getFloors();
        List<Booking> existingBookings = bookingService.getUserBookings(username, month);

        model.addAttribute("month", month);
        model.addAttribute("monthName", getMonthName(month));
        model.addAttribute("year", 2026);
        model.addAttribute("floors", floors);
        model.addAttribute("selectedDates", selectedDates);
        model.addAttribute("existingBookings", existingBookings);
        model.addAttribute("username", username);

        return "select-seats";
    }

    @PostMapping("/book")
    public String bookSeat(@RequestParam int month,
                          @RequestParam String date,
                          @RequestParam int floor,
                          @RequestParam String seatId,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        LocalDate bookingDate = LocalDate.parse(date);
        BookingService.BookingResult result = bookingService.addBooking(username, bookingDate, floor, seatId);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
        }

        return "redirect:/calendar/" + month;
    }

    @PostMapping("/delete-booking")
    public String deleteBooking(@RequestParam int month,
                               @RequestParam String date,
                               @RequestParam String seatId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        LocalDate bookingDate = LocalDate.parse(date);
        BookingService.BookingResult result = bookingService.deleteBooking(username, bookingDate, seatId);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
        }

        return "redirect:/calendar/" + month;
    }

    @GetMapping("/check-seat-availability")
    @ResponseBody
    public Set<LocalDate> checkSeatAvailability(@RequestParam String seatId,
                                               @RequestParam int month) {
        return bookingService.getBookedDatesForSeat(seatId, month);
    }

    private String getMonthName(int month) {
        String[] monthNames = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return monthNames[month];
    }
}
