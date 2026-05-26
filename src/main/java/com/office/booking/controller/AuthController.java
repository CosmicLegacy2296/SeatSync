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
    private static final String PENDING_EMP_USERNAME = "pendingEmployeeSignupUsername";
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
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        if (normalizedUsername.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }

        Optional<User> user = userService.login(normalizedUsername, password);
        if (user.isPresent()) {
            session.setAttribute("username", normalizedUsername);

            // Check if this user is a registered company owner — redirect accordingly
            boolean isOwner = companyService.findByOwnerEmail(normalizedUsername).isPresent();
            if (isOwner) {
                companyService.findByOwnerEmail(normalizedUsername).ifPresent(company -> {
                    session.setAttribute("companyId", company.getId());
                    session.setAttribute("companyOwner", true);
                });
                return "redirect:/company-dashboard";
            }
            
            // Employee logic
            if (user.get().getCompanyId() == null) {
                session.setAttribute("companyOwner", false);
                return "redirect:/join-company";
            }
            
            session.setAttribute("companyId", user.get().getCompanyId());
            session.setAttribute("companyOwner", false);
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
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        String normalizedName = fullName == null ? "" : fullName.trim();

        if (normalizedUsername.isBlank() || normalizedName.isBlank() || password == null || password.isBlank()) {
            redirectAttrs.addFlashAttribute("error", "Please fill out all fields.");
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
        if (userService.existsByUsername(normalizedUsername)) {
            redirectAttrs.addFlashAttribute("error", "That username is already taken.");
            return "redirect:/employee-signup";
        }

        // Step 1: hold registration info in session until invite code is provided.
        session.setAttribute(PENDING_EMP_USERNAME, normalizedUsername);
        session.setAttribute(PENDING_EMP_PASSWORD, password);
        session.setAttribute(PENDING_EMP_NAME, normalizedName);

        return "redirect:/employee-signup/invite";
    }

    @GetMapping("/employee-signup/invite")
    public String employeeInvitePage(HttpSession session, RedirectAttributes redirectAttrs) {
        if (session.getAttribute(PENDING_EMP_USERNAME) == null) {
            return "redirect:/employee-signup";
        }
        return "employee-invite";
    }

    @PostMapping("/employee-signup/invite")
    public String employeeInviteSubmit(
            @RequestParam String companyCode,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        String pendingUsername = (String) session.getAttribute(PENDING_EMP_USERNAME);
        String pendingPassword = (String) session.getAttribute(PENDING_EMP_PASSWORD);
        String pendingName = (String) session.getAttribute(PENDING_EMP_NAME);

        if (pendingUsername == null || pendingPassword == null || pendingName == null) {
            redirectAttrs.addFlashAttribute("error", "Your signup session expired. Please start again.");
            return "redirect:/employee-signup";
        }

        Optional<com.office.booking.model.Company> companyOpt = companyService.findByCompanyCode(companyCode.trim());
        if (companyOpt.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Invalid invite code. Please try again.");
            return "redirect:/employee-signup/invite";
        }

        var company = companyOpt.get();
        User created = userService.registerEmployee(company.getId(), pendingUsername, pendingPassword, pendingName);
        if (created == null) {
            redirectAttrs.addFlashAttribute("error", "That username is already taken. Please try again.");
            return "redirect:/employee-signup";
        }

        // Log the employee in immediately and route to their dashboard.
        session.setAttribute("username", created.getUsername());
        session.setAttribute("companyId", created.getCompanyId());
        session.setAttribute("companyOwner", false);
        session.removeAttribute(PENDING_EMP_USERNAME);
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
