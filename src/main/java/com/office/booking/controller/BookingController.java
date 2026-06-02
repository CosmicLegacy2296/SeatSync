package com.office.booking.controller;

import com.office.booking.model.Booking;
import com.office.booking.model.BookingExtensionRequest;
import com.office.booking.model.Floor;
import com.office.booking.service.BookingService;
import com.office.booking.service.CompanyService;
import com.office.booking.model.Company;
import com.office.booking.service.ExtensionRequestService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class BookingController {

    private static final String SESSION_SELECTED_DATES = "selectedDatesList";
    private static final String SESSION_SEAT_ASSIGNMENTS = "seatAssignments";
    private static final String SESSION_BOOKING_MONTH = "bookingMonth";
    private static final String SESSION_EDIT_MODE = "seatSelectionEditMode";
    @Autowired
    private BookingService bookingService;

    @Autowired
    private ExtensionRequestService extensionRequestService;

    @Autowired
    private CompanyService companyService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        String companyId = (String) session.getAttribute("companyId");
        if (companyId == null) {
            return "redirect:/login";
        }

        // Build per-month booking counts for status badges on month cards
        // Months 2–12 are bookable (year is hardcoded to 2026)
        java.util.Map<Integer, Integer> monthBookingCounts = new java.util.LinkedHashMap<>();
        for (int m = 2; m <= 12; m++) {
            monthBookingCounts.put(m, bookingService.getUserBookings(companyId, username, m).size());
        }

        model.addAttribute("username", username);
        model.addAttribute("displayName", session.getAttribute("displayName"));
        String companyName = companyId != null ? companyService.findById(companyId).map(Company::getDisplayName).orElse("Workspace") : "Workspace";
        model.addAttribute("companyName", companyName);
        model.addAttribute("monthBookingCounts", monthBookingCounts);
        model.addAttribute("currentMonth", java.time.LocalDate.now().getMonthValue());
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
        String companyId = (String) session.getAttribute("companyId");
        if (companyId == null) {
            return "redirect:/login";
        }

        if (month < 2 || month > 12) {
            redirectAttributes.addFlashAttribute("error", "Invalid month selected");
            return "redirect:/dashboard";
        }

        List<LocalDate> availableDates = bookingService.getAvailableDates(month);
        List<Booking> userBookings = bookingService.getUserBookings(companyId, username, month);
        BookingService.BookingValidationResult validation = bookingService.validateUserBookings(companyId, username, month);

        model.addAttribute("month", month);
        model.addAttribute("monthName", getMonthName(month));
        model.addAttribute("year", 2026);
        model.addAttribute("availableDates", availableDates);
        model.addAttribute("userBookings", userBookings);
        model.addAttribute("validation", validation);
        model.addAttribute("username", username);
        model.addAttribute("displayName", session.getAttribute("displayName"));
        model.addAttribute("maxAllowedDays", extensionRequestService.getApprovedMaxForUserMonthYear(companyId, username, month, 2026));
        model.addAttribute("daysInMonth", java.time.Year.of(2026).atMonth(month).lengthOfMonth());

        return "calendar";
    }

    @PostMapping("/start-seat-selection")
    public String startSeatSelection(@RequestParam int month,
                                    @RequestParam String selectedDates,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        String companyId = (String) session.getAttribute("companyId");
        if (companyId == null) {
            return "redirect:/login";
        }
        if (month < 2 || month > 12) {
            redirectAttributes.addFlashAttribute("error", "Invalid month");
            return "redirect:/dashboard";
        }
        List<String> datesList = Arrays.stream(selectedDates.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        if (datesList.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one date");
            return "redirect:/calendar/" + month;
        }
        int maxAllowed = extensionRequestService.getApprovedMaxForUserMonthYear(companyId, username, month, 2026);
        if (datesList.size() > maxAllowed) {
            redirectAttributes.addFlashAttribute("error", "You may select at most " + maxAllowed + " days.");
            return "redirect:/calendar/" + month;
        }
        session.setAttribute(SESSION_SELECTED_DATES, datesList);
        session.setAttribute(SESSION_SEAT_ASSIGNMENTS, new LinkedHashMap<String, String>());
        session.setAttribute(SESSION_BOOKING_MONTH, month);
        session.setAttribute(SESSION_EDIT_MODE, false);
        return "redirect:/select-seat?index=0";
    }

    @GetMapping("/start-seat-selection")
    public String startSeatSelectionEdit(@RequestParam int month,
                                        @RequestParam String editDate,
                                        HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        String companyId = (String) session.getAttribute("companyId");
        if (companyId == null) {
            return "redirect:/login";
        }
        try {
            LocalDate.parse(editDate);
        } catch (DateTimeParseException e) {
            return "redirect:/calendar/" + month;
        }
        session.setAttribute(SESSION_SELECTED_DATES, List.of(editDate.trim()));
        session.setAttribute(SESSION_SEAT_ASSIGNMENTS, new LinkedHashMap<String, String>());
        session.setAttribute(SESSION_BOOKING_MONTH, month);
        session.setAttribute(SESSION_EDIT_MODE, true);
        return "redirect:/select-seat?index=0";
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/select-seat")
    public String selectSeatSingle(@RequestParam int index,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        String companyId = (String) session.getAttribute("companyId");
        if (companyId == null) {
            return "redirect:/login";
        }
        List<String> selectedDates = (List<String>) session.getAttribute(SESSION_SELECTED_DATES);
        if (selectedDates == null || selectedDates.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No dates selected. Please start from the calendar.");
            return "redirect:/dashboard";
        }
        if (index < 0 || index >= selectedDates.size()) {
            return "redirect:/select-seat?index=0";
        }
        String dateStr = selectedDates.get(index);
        LocalDate currentDate;
        try {
            currentDate = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return "redirect:/calendar/" + session.getAttribute(SESSION_BOOKING_MONTH);
        }
        Boolean editMode = (Boolean) session.getAttribute(SESSION_EDIT_MODE);
        Booking currentBooking = Boolean.TRUE.equals(editMode)
                ? bookingService.getBookingForUserAndDate(companyId, username, currentDate)
                : null;
        Set<String> bookedSeats = bookingService.getBookedSeatsForDate(companyId, currentDate);
        if (currentBooking != null && currentBooking.getSeatId() != null) {
            bookedSeats = new HashSet<>(bookedSeats);
            bookedSeats.remove(currentBooking.getSeatId());
        }
        List<Floor> floors = bookingService.getFloors(companyId);
        Map<String, String> assignments = (Map<String, String>) session.getAttribute(SESSION_SEAT_ASSIGNMENTS);
        if (assignments == null) assignments = new LinkedHashMap<>();
        String existingValue = assignments.get(dateStr);
        Integer existingFloor = null;
        String existingSeatId = null;
        if (existingValue != null && existingValue.contains("|")) {
            String[] parts = existingValue.split("\\|", 2);
            try {
                existingFloor = Integer.parseInt(parts[0]);
                existingSeatId = parts[1];
            } catch (NumberFormatException ignored) {}
        } else if (currentBooking != null) {
            existingFloor = currentBooking.getFloor();
            existingSeatId = currentBooking.getSeatId();
            assignments.put(dateStr, existingFloor + "|" + existingSeatId);
            session.setAttribute(SESSION_SEAT_ASSIGNMENTS, assignments);
        }
        model.addAttribute("currentDate", currentDate);
        model.addAttribute("dateStr", dateStr);
        model.addAttribute("index", index);
        model.addAttribute("totalDates", selectedDates.size());
        model.addAttribute("bookedSeats", bookedSeats);
        model.addAttribute("floors", floors);
        model.addAttribute("username", username);
        model.addAttribute("displayName", session.getAttribute("displayName"));
        model.addAttribute("editMode", Boolean.TRUE.equals(editMode));
        model.addAttribute("currentBooking", currentBooking);
        model.addAttribute("existingSeatId", existingSeatId);
        model.addAttribute("existingFloor", existingFloor);
        model.addAttribute("month", session.getAttribute(SESSION_BOOKING_MONTH));
        return "select-seat-single";
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/select-seat")
    public String submitSeatSelection(@RequestParam int index,
                                     @RequestParam int floor,
                                     @RequestParam String seatId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        List<String> selectedDates = (List<String>) session.getAttribute(SESSION_SELECTED_DATES);
        if (selectedDates == null || index < 0 || index >= selectedDates.size()) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please start from the calendar.");
            return "redirect:/dashboard";
        }
        String dateStr = selectedDates.get(index);
        Map<String, String> assignments = (Map<String, String>) session.getAttribute(SESSION_SEAT_ASSIGNMENTS);
        if (assignments == null) {
            assignments = new LinkedHashMap<>();
            session.setAttribute(SESSION_SEAT_ASSIGNMENTS, assignments);
        }
        assignments.put(dateStr, floor + "|" + seatId);
        session.setAttribute(SESSION_SEAT_ASSIGNMENTS, assignments);
        if (index < selectedDates.size() - 1) {
            return "redirect:/select-seat?index=" + (index + 1);
        }
        return "redirect:/review";
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/review")
    public String reviewBookings(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        List<String> selectedDates = (List<String>) session.getAttribute(SESSION_SELECTED_DATES);
        Map<String, String> assignments = (Map<String, String>) session.getAttribute(SESSION_SEAT_ASSIGNMENTS);
        if (selectedDates == null || assignments == null || selectedDates.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No bookings to review. Please start from the calendar.");
            return "redirect:/dashboard";
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String dateStr : selectedDates) {
            String val = assignments.get(dateStr);
            if (val == null || !val.contains("|")) continue;
            String[] parts = val.split("\\|", 2);
            try {
                LocalDate d = LocalDate.parse(dateStr);
                rows.add(Map.<String, Object>of(
                    "date", d,
                    "dateStr", dateStr,
                    "floor", Integer.parseInt(parts[0]),
                    "seatId", parts[1]
                ));
            } catch (Exception ignored) {}
        }
        Integer month = (Integer) session.getAttribute(SESSION_BOOKING_MONTH);
        Boolean editMode = (Boolean) session.getAttribute(SESSION_EDIT_MODE);
        model.addAttribute("rows", rows);
        model.addAttribute("username", username);
        model.addAttribute("displayName", session.getAttribute("displayName"));
        model.addAttribute("month", month != null ? month : 2);
        model.addAttribute("editMode", Boolean.TRUE.equals(editMode));
        model.addAttribute("totalDates", selectedDates.size());
        return "review";
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/confirm-booking")
    public String confirmBooking(HttpSession session, RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        String companyId = (String) session.getAttribute("companyId");
        if (companyId == null) {
            return "redirect:/login";
        }
        List<String> selectedDates = (List<String>) session.getAttribute(SESSION_SELECTED_DATES);
        Map<String, String> assignments = (Map<String, String>) session.getAttribute(SESSION_SEAT_ASSIGNMENTS);
        Integer month = (Integer) session.getAttribute(SESSION_BOOKING_MONTH);
        Boolean editMode = (Boolean) session.getAttribute(SESSION_EDIT_MODE);
        if (selectedDates == null || assignments == null || month == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please start from the calendar.");
            return "redirect:/dashboard";
        }
        for (String dateStr : selectedDates) {
            String val = assignments.get(dateStr);
            if (val == null || !val.contains("|")) continue;
            String[] parts = val.split("\\|", 2);
            try {
                LocalDate date = LocalDate.parse(dateStr);
                int floor = Integer.parseInt(parts[0]);
                String seatId = parts[1];
                BookingService.BookingResult result;
                if (Boolean.TRUE.equals(editMode)) {
                    result = bookingService.updateBooking(companyId, username, date, floor, seatId);
                } else {
                    result = bookingService.addBooking(companyId, username, date, floor, seatId);
                }
                if (!result.isSuccess()) {
                    redirectAttributes.addFlashAttribute("error", result.getMessage());
                    return "redirect:/review";
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Invalid booking data");
                return "redirect:/review";
            }
        }
        session.removeAttribute(SESSION_SELECTED_DATES);
        session.removeAttribute(SESSION_SEAT_ASSIGNMENTS);
        session.removeAttribute(SESSION_BOOKING_MONTH);
        session.removeAttribute(SESSION_EDIT_MODE);
        redirectAttributes.addFlashAttribute("success", "Bookings confirmed successfully!");
        return "redirect:/calendar/" + month;
    }

    @PostMapping("/book")
    public String bookSeat(@RequestParam int month,
                          @RequestParam String date,
                          @RequestParam int floor,
                          @RequestParam String seatId,
                          @RequestParam(required = false) Boolean editMode,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        String companyId = (String) session.getAttribute("companyId");
        if (companyId == null) {
            return "redirect:/login";
        }

        LocalDate bookingDate = LocalDate.parse(date);
        BookingService.BookingResult result;

        if (Boolean.TRUE.equals(editMode)) {
            // Update existing booking
            result = bookingService.updateBooking(companyId, username, bookingDate, floor, seatId);
        } else {
            // Create new booking
            result = bookingService.addBooking(companyId, username, bookingDate, floor, seatId);
        }

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
        String companyId = (String) session.getAttribute("companyId");
        if (companyId == null) {
            return "redirect:/login";
        }

        LocalDate bookingDate = LocalDate.parse(date);
        BookingService.BookingResult result = bookingService.deleteBooking(companyId, username, bookingDate, seatId);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
        }

        return "redirect:/calendar/" + month;
    }

    @PostMapping("/request-extension")
    public String requestExtension(@RequestParam int month,
                                  @RequestParam int year,
                                  @RequestParam int requestedDays,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        String companyId = (String) session.getAttribute("companyId");
        if (companyId == null) {
            return "redirect:/login";
        }
        if (month < 2 || month > 12) {
            redirectAttributes.addFlashAttribute("error", "Invalid month");
            return "redirect:/calendar/" + month;
        }
        if (requestedDays <= 10) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid number of days for this month.");
            return "redirect:/calendar/" + month;
        }
        int daysInMonth = java.time.Year.of(year).atMonth(month).lengthOfMonth();
        if (requestedDays > daysInMonth) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid number of days for this month.");
            return "redirect:/calendar/" + month;
        }
        if (extensionRequestService.getApprovedMaxForUserMonthYear(companyId, username, month, year) != 10) {
            redirectAttributes.addFlashAttribute("error", "You already have an approved extension for this month.");
            return "redirect:/calendar/" + month;
        }
        if (extensionRequestService.hasPendingRequest(companyId, username, month, year)) {
            redirectAttributes.addFlashAttribute("error", "You already have a pending request for this month.");
            return "redirect:/calendar/" + month;
        }
        BookingExtensionRequest req = new BookingExtensionRequest(companyId, username, requestedDays, month, year);
        extensionRequestService.save(req);
        redirectAttributes.addFlashAttribute("success", "Request submitted to admin for approval.");
        return "redirect:/calendar/" + month;
    }

    @GetMapping("/check-seat-availability")
    @ResponseBody
    public Set<LocalDate> checkSeatAvailability(@RequestParam String seatId,
                                               @RequestParam int month,
                                               HttpSession session) {
        String username = (String) session.getAttribute("username");
        String companyId = (String) session.getAttribute("companyId");
        if (username == null || companyId == null) {
            return java.util.Set.of();
        }
        return bookingService.getBookedDatesForSeat(companyId, seatId, month);
    }

    private String getMonthName(int month) {
        String[] monthNames = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return monthNames[month];
    }
}
