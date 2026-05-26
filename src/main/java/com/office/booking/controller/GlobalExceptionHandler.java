package com.office.booking.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handleAnyException(
            Exception ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String path = request.getRequestURI();
        // Log for debugging, but keep the UI friendly.
        log.warn("Request failed: {} - {}", path, ex.getMessage(), ex);

        String errorMessage = "Something went wrong. Please try again.";

        if (path.startsWith("/login")) {
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/login";
        }

        if (path.startsWith("/join-company")) {
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/join-company";
        }

        if (path.startsWith("/employee-signup") || path.startsWith("/employee-invite")) {
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/employee-signup";
        }

        redirectAttributes.addFlashAttribute("error", errorMessage);
        return "redirect:/";
    }
}

