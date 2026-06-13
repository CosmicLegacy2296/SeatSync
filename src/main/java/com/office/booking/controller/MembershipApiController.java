package com.office.booking.controller;

import com.office.booking.model.Booking;
import com.office.booking.model.BookingExtensionRequest;
import com.office.booking.model.Company;
import com.office.booking.model.User;
import com.office.booking.repository.BookingExtensionRequestRepository;
import com.office.booking.repository.BookingRepository;
import com.office.booking.repository.CompanyRepository;
import com.office.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class MembershipApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private BookingExtensionRequestRepository bookingExtensionRequestRepository;

    @GetMapping("/api/membership")
    public Map<String, Object> getMembership(@RequestParam String displayName) {
        Map<String, Object> response = new HashMap<>();
        
        if (displayName == null || displayName.isBlank()) {
            response.put("found", false);
            return response;
        }

        // Look up user by display_name
        var userOpt = userRepository.findByDisplayName(displayName);
        if (userOpt.isEmpty()) {
            response.put("found", false);
            return response;
        }

        User user = userOpt.get();
        response.put("found", true);
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("displayName", user.getDisplayName());
        response.put("maxAllowedDays", user.getMaxAllowedDays());
        response.put("companyId", user.getCompanyId());
        response.put("organizationName", user.getOrganizationName());
        response.put("role", user.getRole());

        String role = user.getRole();
        if (role == null) {
            role = "EMPLOYEE";
        }

        if ("EMPLOYEE".equals(role)) {
            // Fetch upcoming bookings for employee
            if (user.getCompanyId() != null) {
                List<Booking> allBookings = bookingRepository.findByCompanyIdAndUsername(user.getCompanyId(), user.getEmail());
                LocalDate today = LocalDate.now();
                List<Map<String, Object>> upcomingBookings = allBookings.stream()
                        .filter(b -> b.getDate() != null && b.getDate().isAfter(today.minusDays(1)))
                        .sorted((b1, b2) -> b1.getDate().compareTo(b2.getDate()))
                        .limit(5)
                        .map(b -> {
                            Map<String, Object> booking = new HashMap<>();
                            booking.put("bookingDate", b.getDate().toString());
                            booking.put("floor", b.getFloor());
                            booking.put("seatId", b.getSeatId());
                            return booking;
                        })
                        .collect(Collectors.toList());
                response.put("upcomingBookings", upcomingBookings);
            }
        } else if ("ADMIN".equals(role) || "OWNER".equals(role)) {
            // Fetch company stats for admin/owner
            if (user.getCompanyId() != null) {
                Map<String, Object> companyStats = new HashMap<>();
                
                // Get company info
                var companyOpt = companyRepository.findById(user.getCompanyId());
                if (companyOpt.isPresent()) {
                    Company company = companyOpt.get();
                    companyStats.put("companyName", company.getDisplayName());
                    companyStats.put("employeeJoinCode", company.getCompanyCode());
                    companyStats.put("adminJoinCode", company.getAdminCode());
                    
                    if ("OWNER".equals(role)) {
                        companyStats.put("industry", company.getIndustry());
                        companyStats.put("companySize", company.getCompanySize());
                    }
                }
                
                // Count employees and admins
                List<User> companyUsers = userRepository.findByCompanyId(user.getCompanyId());
                long totalEmployees = companyUsers.stream()
                        .filter(u -> u.getRole() == null || "EMPLOYEE".equals(u.getRole()))
                        .count();
                long totalAdmins = companyUsers.stream()
                        .filter(u -> "ADMIN".equals(u.getRole()))
                        .count();
                companyStats.put("totalEmployees", totalEmployees);
                companyStats.put("totalAdmins", totalAdmins);
                
                // Count pending extension requests
                List<BookingExtensionRequest> pendingRequests = bookingExtensionRequestRepository
                        .findByCompanyIdAndStatus(user.getCompanyId(), "PENDING");
                companyStats.put("pendingExtensionRequests", pendingRequests.size());
                
                if ("OWNER".equals(role)) {
                    // Count total bookings
                    long totalBookings = bookingRepository.countByCompanyId(user.getCompanyId());
                    companyStats.put("totalBookings", totalBookings);
                    
                    // Count current month bookings
                    int currentMonth = LocalDate.now().getMonthValue();
                    int currentYear = LocalDate.now().getYear();
                    List<Booking> currentMonthBookingsList = bookingRepository.findByCompanyIdAndUsername(user.getCompanyId(), user.getEmail());
                    long currentMonthBookings = currentMonthBookingsList.stream()
                            .filter(b -> b.getMonth() == currentMonth && b.getYear() == currentYear)
                            .count();
                    companyStats.put("currentMonthBookings", currentMonthBookings);
                }
                
                response.put("companyStats", companyStats);
            }
        }

        return response;
    }
}
