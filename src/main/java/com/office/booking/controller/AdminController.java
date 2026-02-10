package com.office.booking.controller;

import com.office.booking.model.Booking;
import com.office.booking.service.BookingService;
import com.office.booking.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AdminController {

    private static final String ADMIN_SECURITY_CODE = "SEC123";

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/admin-login")
    public String adminLoginPage(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if ("ADMIN".equals(role)) {
            return "redirect:/admin/bookings";
        }
        return "admin-login";
    }

    @PostMapping("/admin-login")
    public String adminLogin(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam("securityCode") String securityCode,
                             HttpSession session,
                             Model model) {
        if (!"admin".equals(username)) {
            model.addAttribute("error", "Invalid admin credentials.");
            return "admin-login";
        }

        if (!ADMIN_SECURITY_CODE.equals(securityCode)) {
            model.addAttribute("error", "Invalid security code.");
            return "admin-login";
        }

        var userOpt = userService.login(username, password);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Invalid admin credentials.");
            return "admin-login";
        }

        session.setAttribute("username", username);
        session.setAttribute("user", userOpt.get());
        session.setAttribute("role", "ADMIN");

        return "redirect:/admin/bookings";
    }

    @GetMapping("/admin/bookings")
    public String adminBookings(@RequestParam(name = "month", required = false) Integer month,
                                HttpSession session,
                                Model model) {
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        if (username == null || !"ADMIN".equals(role)) {
            return "redirect:/admin-login";
        }

        int selectedMonth = (month == null || month < 1 || month > 12) ? 2 : month;

        List<Booking> bookings = bookingService.getBookingsForMonth(selectedMonth);

        model.addAttribute("username", username);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("monthName", getMonthName(selectedMonth));
        model.addAttribute("bookings", bookings);

        return "admin-bookings";
    }

    @PostMapping("/admin/delete-booking")
    public String adminDeleteBooking(@RequestParam int month,
                                     @RequestParam String date,
                                     @RequestParam String seatId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        if (username == null || !"ADMIN".equals(role)) {
            return "redirect:/admin-login";
        }

        LocalDate bookingDate = LocalDate.parse(date);
        BookingService.BookingResult result = bookingService.adminDeleteBooking(bookingDate, seatId);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
        }

        return "redirect:/admin/bookings?month=" + month;
    }

    private String getMonthName(int month) {
        String[] monthNames = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return monthNames[month];
    }
}

