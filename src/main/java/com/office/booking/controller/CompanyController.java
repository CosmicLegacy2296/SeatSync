package com.office.booking.controller;

import com.office.booking.model.Company;
import com.office.booking.model.User;
import com.office.booking.service.BookingService;
import com.office.booking.service.CompanyService;
import com.office.booking.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Handles company registration, the company dashboard, and owner-level employee management.
 * All routes are scoped to company owners identified by session attributes.
 */
@Controller
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    // -------------------------------------------------------------------------
    // Registration Wizard
    // -------------------------------------------------------------------------

    /**
     * Renders the 3-step company registration wizard.
     * Redirects to the company dashboard if the user is already a registered owner.
     *
     * @param session current HTTP session
     * @return template name or redirect
     */
    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute("companyOwner"))) {
            return "redirect:/company-dashboard";
        }
        return "register";
    }

    /**
     * Handles the company registration form submission (all 3 steps sent at once).
     * Validates uniqueness of the owner email, saves the company, and sets session state.
     *
     * @param companyName   legal company name (required)
     * @param tradingName   optional trading/brand name
     * @param industry      selected industry sector
     * @param companySize   selected size band
     * @param website       optional website URL
     * @param ownerName     full name of owner (required)
     * @param ownerTitle    job title of owner (required)
     * @param ownerEmail    work email — used as username for login (required)
     * @param ownerPassword plaintext password (min 8 chars; hash in production)
     * @param floor1Seats   number of bookable seats on Floor 1
     * @param floor2Seats   number of bookable seats on Floor 2
     * @param startTime     workspace start time
     * @param endTime       workspace end time
     * @param timezone      IANA timezone string
     * @param logoBase64    optional base64 logo data URI
     * @param session       current HTTP session
     * @param redirectAttrs flash attributes for success/error messages
     * @return redirect to /company-dashboard on success, back to /register on failure
     */
    @PostMapping("/register")
    public String registerCompany(
            @RequestParam(required = false, defaultValue = "") String companyName,
            @RequestParam(required = false) String tradingName,
            @RequestParam(required = false, defaultValue = "Other") String industry,
            @RequestParam(required = false, defaultValue = "1-50") String companySize,
            @RequestParam(required = false) String website,
            @RequestParam(required = false, defaultValue = "Admin") String ownerName,
            @RequestParam(required = false, defaultValue = "Administrator") String ownerTitle,
            @RequestParam(required = false, defaultValue = "") String ownerEmail,
            @RequestParam(required = false, defaultValue = "") String ownerPassword,
            @RequestParam(required = false, defaultValue = "20") int floor1Seats,
            @RequestParam(required = false, defaultValue = "20") int floor2Seats,
            @RequestParam(required = false, defaultValue = "09:00") String startTime,
            @RequestParam(required = false, defaultValue = "18:00") String endTime,
            @RequestParam(required = false, defaultValue = "America/New_York") String timezone,
            @RequestParam(required = false) String logoBase64,
            @RequestParam(required = false) String floorSeatConfig,
            @RequestParam(required = false) String workingHours,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        // Parse combined workingHours field (e.g. "09:00-17:00") if startTime/endTime weren't sent directly
        if (workingHours != null && !workingHours.isBlank() && workingHours.contains("-")) {
            String[] parts = workingHours.split("-", 2);
            startTime = parts[0].trim();
            endTime = parts[1].trim();
        }

        // Parse JSON floorSeatConfig (e.g. {"1":20,"2":20}) if floor1Seats/floor2Seats weren't sent directly
        if (floorSeatConfig != null && !floorSeatConfig.isBlank()) {
            try {
                floorSeatConfig = floorSeatConfig.trim();
                // Simple JSON parsing without external library
                String clean = floorSeatConfig.replaceAll("[{}\"]", "");
                for (String entry : clean.split(",")) {
                    String[] kv = entry.trim().split(":");
                    if (kv.length == 2) {
                        String key = kv[0].trim();
                        int val = Integer.parseInt(kv[1].trim());
                        if ("1".equals(key)) floor1Seats = val;
                        else if ("2".equals(key)) floor2Seats = val;
                    }
                }
            } catch (Exception ignored) { /* keep defaults */ }
        }

        // Validate email uniqueness
        if (companyService.isEmailRegistered(ownerEmail)) {
            redirectAttrs.addFlashAttribute("error",
                    "An account is already registered with that email address.");
            return "redirect:/register";
        }

        // Build company object
        Company company = new Company();
        company.setCompanyName(companyName.trim());
        company.setTradingName((tradingName != null && !tradingName.isBlank()) ? tradingName.trim() : null);
        company.setIndustry(industry);
        company.setCompanySize(companySize);
        company.setWebsite((website != null && !website.isBlank()) ? website.trim() : null);
        company.setOwnerName(ownerName.trim());
        company.setOwnerTitle(ownerTitle.trim());
        company.setOwnerEmail(ownerEmail.trim().toLowerCase());
        company.setOwnerPassword(ownerPassword);
        company.setFloor1Seats(Math.max(1, Math.min(100, floor1Seats)));
        company.setFloor2Seats(Math.max(1, Math.min(100, floor2Seats)));
        company.setStartTime(startTime);
        company.setEndTime(endTime);
        company.setTimezone(timezone);
        if (logoBase64 != null && !logoBase64.isBlank()) {
            company.setLogoBase64(logoBase64);
        }

        // Save company
        Company saved = companyService.registerCompany(company);

        // Also register owner as a user in UserService so they can log in via /login
        userService.registerEmployee(saved.getId(), saved.getOwnerEmail(),
                saved.getOwnerPassword(), saved.getOwnerName());

        // Set session attributes
        session.setAttribute("username", saved.getOwnerEmail());
        session.setAttribute("companyId", saved.getId());
        session.setAttribute("companyOwner", true);

        redirectAttrs.addFlashAttribute("welcomeMessage",
                "Welcome to SeatSync, " + saved.getDisplayName() + "!");
        return "redirect:/company-dashboard";
    }

    // -------------------------------------------------------------------------
    // Company Dashboard
    // -------------------------------------------------------------------------

    /**
     * Renders the company owner's dashboard with stats and employee management.
     * Requires an active session with companyOwner=true and a valid companyId.
     *
     * @param session current HTTP session
     * @param model   Spring MVC model for template data
     * @return template name or redirect to login
     */
    @GetMapping("/company-dashboard")
    public String companyDashboard(HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("companyOwner"))) {
            return "redirect:/login";
        }
        String companyId = (String) session.getAttribute("companyId");
        Optional<Company> companyOpt = companyService.findById(companyId);
        if (companyOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        Company company = companyOpt.get();
        List<User> employees = userService.getUsersByCompany(companyId);

        // Count active bookings this month for stats card
        int currentMonth = LocalDate.now().getMonthValue();
        long activeBookingsThisMonth = bookingService.getBookingsForMonth(currentMonth).stream()
                .filter(b -> employees.stream().anyMatch(e -> e.getUsername().equals(b.getUsername())))
                .count();

        model.addAttribute("company", company);
        model.addAttribute("employees", employees);
        model.addAttribute("totalSeats", company.getTotalSeats());
        model.addAttribute("totalEmployees", employees.size());
        model.addAttribute("activeBookingsThisMonth", activeBookingsThisMonth);
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("welcomeMessage", session.getAttribute("welcomeMessage"));
        session.removeAttribute("welcomeMessage");

        return "company-dashboard";
    }

    // -------------------------------------------------------------------------
    // Add Employee (from dashboard)
    // -------------------------------------------------------------------------

    /**
     * Adds a new employee to the company from the dashboard "Manage Employees" section.
     *
     * @param empName      full name of the new employee
     * @param empUsername  username for the new employee
     * @param empPassword  temporary password
     * @param session      current HTTP session
     * @param redirectAttrs flash attributes
     * @return redirect back to company dashboard
     */
    @PostMapping("/company/add-employee")
    public String addEmployee(
            @RequestParam String empName,
            @RequestParam String empUsername,
            @RequestParam String empPassword,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        if (!Boolean.TRUE.equals(session.getAttribute("companyOwner"))) {
            return "redirect:/login";
        }
        String companyId = (String) session.getAttribute("companyId");

        User created = userService.registerEmployee(companyId, empUsername.trim().toLowerCase(), empPassword, empName.trim());
        if (created == null) {
            redirectAttrs.addFlashAttribute("error",
                    "Username '" + empUsername + "' is already taken. Please choose another.");
        } else {
            redirectAttrs.addFlashAttribute("success",
                    "Employee " + empName + " added successfully!");
        }
        return "redirect:/company-dashboard";
    }

    // -------------------------------------------------------------------------
    // Employee Join Company Flow
    // -------------------------------------------------------------------------

    @GetMapping("/join-company")
    public String joinCompanyPage(HttpSession session) {
        if (session.getAttribute("username") == null) {
            return "redirect:/login";
        }
        String username = (String) session.getAttribute("username");
        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get().getCompanyId() != null) {
            return "redirect:/dashboard";
        }
        return "join-company";
    }

    @PostMapping("/join-company")
    public String joinCompanySubmit(@RequestParam String companyCode, HttpSession session, RedirectAttributes redirectAttrs) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Company> companyOpt = companyService.findByCompanyCode(companyCode.trim());
        if (companyOpt.isPresent()) {
            Company company = companyOpt.get();
            Optional<User> updatedUserOpt = userService.assignCompanyToUser(username, company.getId());
            if (updatedUserOpt.isPresent()) {
                session.setAttribute("companyId", company.getId());
                session.setAttribute("companyOwner", false);
                redirectAttrs.addFlashAttribute("success", "Successfully joined " + company.getDisplayName() + "!");
                return "redirect:/dashboard";
            }
        }

        redirectAttrs.addFlashAttribute("error", "Invalid company code. Please try again.");
        return "redirect:/join-company";
    }
}
