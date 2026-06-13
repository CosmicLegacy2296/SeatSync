package com.office.booking.controller;

import com.office.booking.model.User;
import com.office.booking.service.CompanyService;
import com.office.booking.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {
    private static final String PENDING_EMP_EMAIL = "pendingEmployeeSignupEmail";
    private static final String PENDING_EMP_DISPLAY_NAME = "pendingEmployeeSignupDisplayName";
    private static final String PENDING_EMP_PASSWORD = "pendingEmployeeSignupPassword";
    private static final String PENDING_EMP_NAME = "pendingEmployeeSignupName";

    @Autowired
    private UserService userService;

    @Autowired
    private CompanyService companyService;

    @GetMapping("/")
    public String index() {
        return "landing";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("username") != null) {
            // Route logged-in users away from the login screen.
            if (Boolean.TRUE.equals(session.getAttribute("companyOwner"))) {
                return "redirect:/company-dashboard";
            }
            if (session.getAttribute("companyId") == null) {
                return "redirect:/join-company";
            }
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                       @RequestParam String password,
                       HttpSession session,
                       Model model) {
        String trimmedUsername = username == null ? "" : username.trim();
        if (trimmedUsername.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }

        // Try login with original input first (preserves case for usernames)
        Optional<User> user = userService.login(trimmedUsername, password);
        
        // If that fails, try with lowercase for email case-insensitivity
        if (user.isEmpty()) {
            String lowercaseUsername = trimmedUsername.toLowerCase();
            user = userService.login(lowercaseUsername, password);
        }
        
        if (user.isPresent()) {
            session.setAttribute("username", user.get().getEmail());
            session.setAttribute("displayName", user.get().getDisplayName());

            // Check if this user is a registered company owner — redirect accordingly
            Optional<com.office.booking.model.Company> companyOpt = companyService.findByOwnerEmail(user.get().getEmail());
            if (companyOpt.isPresent()) {
                com.office.booking.model.Company company = companyOpt.get();
                session.setAttribute("companyId", company.getId());
                session.setAttribute("companyOwner", true);
                
                String title = company.getOwnerTitle();
                if (title != null) {
                    String lowerTitle = title.toLowerCase();
                    if (lowerTitle.contains("ceo") || lowerTitle.contains("cto") || 
                        lowerTitle.contains("manager") || lowerTitle.contains("admin") ||
                        lowerTitle.contains("director") || lowerTitle.contains("president") ||
                        lowerTitle.contains("founder") || lowerTitle.contains("owner")) {
                        session.setAttribute("role", "ADMIN");
                    }
                }
                return "redirect:/company-dashboard";
            }
            
            // Employee / Admin logic
            if (user.get().getCompanyId() == null) {
                session.setAttribute("companyOwner", false);
                return "redirect:/join-company";
            }
            
            session.setAttribute("companyId", user.get().getCompanyId());
            session.setAttribute("companyOwner", false);
            session.setAttribute("role", user.get().getRole());
            if (user.get().getOrganizationName() != null && !user.get().getOrganizationName().isBlank()) {
                session.setAttribute("organizationName", user.get().getOrganizationName());
            }
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    // -------------------------------------------------------------------------
    // Employee Self-Signup + Invite Code Join
    // -------------------------------------------------------------------------

    @GetMapping("/employee-signup")
    public String employeeSignupPage() {
        return "employee-signup";
    }

    @PostMapping("/employee-signup")
    public String employeeSignupSubmit(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String displayName,
            @RequestParam String password,
            @RequestParam(required = false) String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedDisplayName = displayName == null ? "" : displayName.trim();
        String normalizedName = fullName == null ? "" : fullName.trim();

        if (normalizedEmail.isBlank() || normalizedDisplayName.isBlank() || normalizedName.isBlank() || password == null || password.isBlank()) {
            redirectAttrs.addFlashAttribute("error", "Please fill out all fields.");
            return "redirect:/employee-signup";
        }
        if (!normalizedEmail.contains("@")) {
            redirectAttrs.addFlashAttribute("error", "Please enter a valid email address.");
            return "redirect:/employee-signup";
        }
        if (password.length() < 8) {
            redirectAttrs.addFlashAttribute("error", "Password must be at least 8 characters.");
            return "redirect:/employee-signup";
        }
        if (confirmPassword != null && !confirmPassword.equals(password)) {
            redirectAttrs.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/employee-signup";
        }
        if (userService.existsByEmailOrDisplayName(normalizedEmail)) {
            redirectAttrs.addFlashAttribute("error", "That email address is already registered.");
            return "redirect:/employee-signup";
        }
        if (userService.existsByDisplayName(normalizedDisplayName)) {
            redirectAttrs.addFlashAttribute("error", "That display name is already in use. Please choose another.");
            return "redirect:/employee-signup";
        }

        // Step 1: hold registration info in session until invite code is provided.
        session.setAttribute(PENDING_EMP_EMAIL, normalizedEmail);
        session.setAttribute(PENDING_EMP_DISPLAY_NAME, normalizedDisplayName);
        session.setAttribute(PENDING_EMP_PASSWORD, password);
        session.setAttribute(PENDING_EMP_NAME, normalizedName);

        return "redirect:/employee-signup/invite";
    }

    @GetMapping("/employee-signup/invite")
    public String employeeInvitePage(HttpSession session, RedirectAttributes redirectAttrs) {
        if (session.getAttribute(PENDING_EMP_EMAIL) == null) {
            return "redirect:/employee-signup";
        }
        return "employee-invite";
    }

    @PostMapping("/employee-signup/invite")
    public String employeeInviteSubmit(
            @RequestParam String companyCode,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        String pendingEmail = (String) session.getAttribute(PENDING_EMP_EMAIL);
        String pendingDisplayName = (String) session.getAttribute(PENDING_EMP_DISPLAY_NAME);
        String pendingPassword = (String) session.getAttribute(PENDING_EMP_PASSWORD);
        String pendingName = (String) session.getAttribute(PENDING_EMP_NAME);

        if (pendingEmail == null || pendingPassword == null || pendingName == null) {
            redirectAttrs.addFlashAttribute("error", "Your signup session expired. Please start again.");
            return "redirect:/employee-signup";
        }

        Optional<com.office.booking.model.Company> companyOpt = companyService.findByCompanyCode(companyCode.trim());
        if (companyOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Invalid invite code. Please try again.");
            return "redirect:/employee-signup/invite";
        }

        var company = companyOpt.get();
        User created = userService.registerEmployee(company.getId(), pendingEmail, pendingPassword, pendingName, pendingDisplayName);
        if (created == null) {
            redirectAttrs.addFlashAttribute("error", "Signup failed. The email or display name may have been taken in the meantime.");
            return "redirect:/employee-signup";
        }

        // Log the employee in immediately and route to their dashboard.
        session.setAttribute("username", created.getEmail());
        session.setAttribute("displayName", created.getDisplayName());
        session.setAttribute("companyId", created.getCompanyId());
        session.setAttribute("companyOwner", false);
        
        session.removeAttribute(PENDING_EMP_EMAIL);
        session.removeAttribute(PENDING_EMP_DISPLAY_NAME);
        session.removeAttribute(PENDING_EMP_PASSWORD);
        session.removeAttribute(PENDING_EMP_NAME);

        redirectAttrs.addFlashAttribute("success", "Welcome to " + company.getDisplayName() + "!");
        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            userService.logout(username);
        }
        session.invalidate();
        return "redirect:/";
    }
}
