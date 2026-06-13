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
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ExtensionRequestService extensionRequestService;

    @Autowired
    private com.office.booking.service.CompanyService companyService;

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
                             @RequestParam("adminCode") String adminCode,
                             HttpSession session,
                             Model model) {
        if (adminCode == null || adminCode.isBlank()) {
            model.addAttribute("error", "Admin code is required.");
            return "admin-login";
        }

        Optional<com.office.booking.model.Company> companyOpt = companyService.findByAdminCode(adminCode.trim());
        if (companyOpt.isEmpty()) {
            model.addAttribute("error", "Invalid admin code.");
            return "admin-login";
        }
        com.office.booking.model.Company company = companyOpt.get();

        String trimmedUsername = username == null ? "" : username.trim();
        
        // Try login with original input first (preserves case for usernames)
        var userOpt = userService.login(trimmedUsername, password);
        
        // If that fails, try with lowercase for email case-insensitivity
        if (userOpt.isEmpty()) {
            String lowercaseUsername = trimmedUsername.toLowerCase();
            userOpt = userService.login(lowercaseUsername, password);
        }
        
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Invalid email or password.");
            return "admin-login";
        }
        com.office.booking.model.User user = userOpt.get();

        // Onboard user to the company workspace as an admin if they have no workspace
        if (user.getCompanyId() == null) {
            userService.assignCompanyToUser(user.getEmail(), company.getId(), company.getDisplayName(), "ADMIN");
            userService.promoteToAdmin(user.getEmail());
            user.setCompanyId(company.getId());
            user.setRole("ADMIN");
        } else if (!user.getCompanyId().equals(company.getId())) {
            model.addAttribute("error", "This user is registered to a different company.");
            return "admin-login";
        } else {
            // Ensure they are promoted to admin role in database
            if (!"ADMIN".equals(user.getRole())) {
                userService.promoteToAdmin(user.getEmail());
                user.setRole("ADMIN");
            }
        }

        session.setAttribute("username", user.getEmail());
        session.setAttribute("displayName", user.getDisplayName());
        session.setAttribute("companyId", company.getId());
        session.setAttribute("companyOwner", false);
        session.setAttribute("role", "ADMIN");

        return "redirect:/admin/bookings";
    }

    @GetMapping("/admin/bookings")
    public String adminBookings(@RequestParam(name = "month", required = false) Integer month,
                                HttpSession session,
                                Model model) {
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        if (username == null) {
            return "redirect:/admin-login";
        }

        // Auto-promote company owners with high-level titles to ADMIN role in their active session
        if (!"ADMIN".equals(role) && Boolean.TRUE.equals(session.getAttribute("companyOwner"))) {
            String companyId = (String) session.getAttribute("companyId");
            if (companyId != null) {
                Optional<com.office.booking.model.Company> companyOpt = companyService.findById(companyId);
                if (companyOpt.isPresent()) {
                    String title = companyOpt.get().getOwnerTitle();
                    if (title != null) {
                        String lowerTitle = title.toLowerCase();
                        if (lowerTitle.contains("ceo") || lowerTitle.contains("cto") || 
                            lowerTitle.contains("manager") || lowerTitle.contains("admin") ||
                            lowerTitle.contains("director") || lowerTitle.contains("president") ||
                            lowerTitle.contains("founder") || lowerTitle.contains("owner")) {
                            session.setAttribute("role", "ADMIN");
                            role = "ADMIN";
                        }
                    }
                }
            }
        }

        if (!"ADMIN".equals(role)) {
            return "redirect:/admin-login";
        }

        int selectedMonth = (month == null || month < 1 || month > 12) ? 2 : month;

        List<Booking> bookings = bookingService.getBookingsForMonth(selectedMonth);
        List<BookingExtensionRequest> pendingExtensionRequests = java.util.List.of();

        // Scope bookings and stats to this company workspace if connected
        String companyName = null;
        String companyCode = null;
        boolean hasWorkspace = false;

        Optional<com.office.booking.model.User> adminUserOpt = userService.findByEmailOrDisplayName(username);
        if (adminUserOpt.isPresent() && adminUserOpt.get().getCompanyId() != null) {
            String adminCompanyId = adminUserOpt.get().getCompanyId();
            bookings = bookings.stream()
                    .filter(b -> adminCompanyId.equals(b.getCompanyId()))
                    .collect(Collectors.toList());
            pendingExtensionRequests = extensionRequestService.findPendingRequests(adminCompanyId);
            
            Optional<com.office.booking.model.Company> companyOpt = companyService.findById(adminCompanyId);
            if (companyOpt.isPresent()) {
                companyName = companyOpt.get().getDisplayName();
                companyCode = companyOpt.get().getCompanyCode();
                hasWorkspace = true;
            }
        }

        long uniqueEmployees = bookings.stream()
                .map(Booking::getUsername).distinct().count();

        model.addAttribute("username", username);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("monthName", getMonthName(selectedMonth));
        model.addAttribute("bookings", bookings);
        model.addAttribute("pendingExtensionRequests", pendingExtensionRequests);
        model.addAttribute("totalBookings", bookings.size());
        model.addAttribute("uniqueEmployees", uniqueEmployees);
        model.addAttribute("companyName", companyName);
        model.addAttribute("companyCode", companyCode);
        model.addAttribute("hasWorkspace", hasWorkspace);

        return "admin-bookings";
    }

    @PostMapping("/admin/join-workspace")
    public String adminJoinWorkspace(@RequestParam String companyCode,
                                     HttpSession session,
                                     RedirectAttributes redirectAttrs) {
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");
        if (username == null || !"ADMIN".equals(role)) {
            return "redirect:/admin-login";
        }

        Optional<com.office.booking.model.Company> companyOpt = companyService.findByCompanyCode(companyCode.trim());
        if (companyOpt.isPresent()) {
            com.office.booking.model.Company company = companyOpt.get();
            Optional<com.office.booking.model.User> updatedUserOpt = userService.assignCompanyToUser(
                    username,
                    company.getId(),
                    company.getDisplayName(),
                    "ADMIN");
            if (updatedUserOpt.isPresent()) {
                session.setAttribute("companyId", company.getId());
                session.setAttribute("role", "ADMIN");
                session.setAttribute("organizationName", company.getDisplayName());
                redirectAttrs.addFlashAttribute("success", "Successfully connected Admin Console to workspace: " + company.getDisplayName() + "!");
                return "redirect:/admin/bookings";
            }
        }

        redirectAttrs.addFlashAttribute("error", "Invalid company code. Please try again.");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/admin/delete-booking")
    public String adminDeleteBooking(@RequestParam int month,
                                     @RequestParam String date,
                                     @RequestParam String seatId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");
        String companyId = (String) session.getAttribute("companyId");

        if (username == null || !"ADMIN".equals(role) || companyId == null) {
            return "redirect:/admin-login";
        }

        LocalDate bookingDate = LocalDate.parse(date);
        BookingService.BookingResult result = bookingService.adminDeleteBooking(companyId, bookingDate, seatId);

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
        String companyId = (String) session.getAttribute("companyId");
        if (!"ADMIN".equals(role) || companyId == null) {
            return "redirect:/admin-login";
        }
        var reqOpt = extensionRequestService.findById(id);
        if (reqOpt.isEmpty()
                || !BookingExtensionRequest.PENDING.equals(reqOpt.get().getStatus())
                || !companyId.equals(reqOpt.get().getCompanyId())) {
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
        String companyId = (String) session.getAttribute("companyId");
        if (!"ADMIN".equals(role) || companyId == null) {
            return "redirect:/admin-login";
        }
        var reqOpt = extensionRequestService.findById(id);
        if (reqOpt.isEmpty()
                || !BookingExtensionRequest.PENDING.equals(reqOpt.get().getStatus())
                || !companyId.equals(reqOpt.get().getCompanyId())) {
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
        String companyId = (String) session.getAttribute("companyId");
        if (username == null || !"ADMIN".equals(role) || companyId == null) {
            response.sendRedirect("/admin-login");
            return;
        }
        int selectedMonth = (month < 1 || month > 12) ? 1 : month;
        List<Booking> bookings = bookingService.getBookingsForMonth(selectedMonth).stream()
                .filter(b -> companyId.equals(b.getCompanyId()))
                .collect(Collectors.toList());

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

