package com.office.booking.controller;

import com.office.booking.model.Booking;
import com.office.booking.model.BookingExtensionRequest;
import com.office.booking.service.BookingService;
import com.office.booking.service.ExtensionRequestService;
import com.office.booking.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    private static final String ADMIN_SECURITY_CODE = "SEC123";

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ExtensionRequestService extensionRequestService;

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
        List<BookingExtensionRequest> pendingExtensionRequests = extensionRequestService.findPendingRequests();

        long uniqueEmployees = bookings.stream()
                .map(Booking::getUsername).distinct().count();

        model.addAttribute("username", username);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("monthName", getMonthName(selectedMonth));
        model.addAttribute("bookings", bookings);
        model.addAttribute("pendingExtensionRequests", pendingExtensionRequests);
        model.addAttribute("totalBookings", bookings.size());
        model.addAttribute("uniqueEmployees", uniqueEmployees);

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

    @PostMapping("/admin/approve-extension")
    public String approveExtension(@RequestParam long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/admin-login";
        }
        var reqOpt = extensionRequestService.findById(id);
        if (reqOpt.isEmpty() || !BookingExtensionRequest.PENDING.equals(reqOpt.get().getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Request not found or already processed.");
        } else {
            BookingExtensionRequest req = reqOpt.get();
            req.setStatus(BookingExtensionRequest.APPROVED);
            extensionRequestService.save(req);
            redirectAttributes.addFlashAttribute("success", "Extension approved for " + req.getUsername() + " (" + getMonthName(req.getMonth()) + " " + req.getYear() + ").");
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/admin/reject-extension")
    public String rejectExtension(@RequestParam long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/admin-login";
        }
        var reqOpt = extensionRequestService.findById(id);
        if (reqOpt.isEmpty() || !BookingExtensionRequest.PENDING.equals(reqOpt.get().getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Request not found or already processed.");
        } else {
            BookingExtensionRequest req = reqOpt.get();
            req.setStatus(BookingExtensionRequest.REJECTED);
            extensionRequestService.save(req);
            redirectAttributes.addFlashAttribute("success", "Extension request rejected.");
        }
        return "redirect:/admin/bookings";
    }

    /**
     * Exports all bookings for a given month as a downloadable CSV file.
     *
     * @param month    the month number (1–12)
     * @param session  current HTTP session
     * @param response the HTTP response used to stream CSV bytes
     * @throws IOException if writing the CSV fails
     */
    @GetMapping("/admin/export-csv")
    public void exportCsv(@RequestParam(name = "month", defaultValue = "1") int month,
                          HttpSession session,
                          HttpServletResponse response) throws IOException {
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");
        if (username == null || !"ADMIN".equals(role)) {
            response.sendRedirect("/admin-login");
            return;
        }
        int selectedMonth = (month < 1 || month > 12) ? 1 : month;
        List<Booking> bookings = bookingService.getBookingsForMonth(selectedMonth);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"seatsync-bookings-" + getMonthName(selectedMonth) + "-2026.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Employee,Date,Day,Floor,Seat");
        for (Booking b : bookings) {
            writer.printf("%s,%s,%s,Floor %d,%s%n",
                    b.getUsername(),
                    b.getDate().toString(),
                    b.getDate().getDayOfWeek(),
                    b.getFloor(),
                    b.getSeatId());
        }
        writer.flush();
    }

    private String getMonthName(int month) {
        String[] monthNames = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return monthNames[month];
    }
}

