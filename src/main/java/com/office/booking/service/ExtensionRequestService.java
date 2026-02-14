package com.office.booking.service;

import com.office.booking.model.BookingExtensionRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExtensionRequestService {
    private final Map<Long, BookingExtensionRequest> requests = new ConcurrentHashMap<>();

    public BookingExtensionRequest save(BookingExtensionRequest request) {
        requests.put(request.getId(), request);
        return request;
    }

    public Optional<BookingExtensionRequest> findById(Long id) {
        return Optional.ofNullable(requests.get(id));
    }

    public List<BookingExtensionRequest> findByStatus(String status) {
        return requests.values().stream()
                .filter(r -> status.equals(r.getStatus()))
                .toList();
    }

    public boolean hasPendingRequest(String username, int month, int year) {
        return requests.values().stream()
                .anyMatch(r -> username.equals(r.getUsername())
                        && r.getMonth() == month && r.getYear() == year
                        && BookingExtensionRequest.PENDING.equals(r.getStatus()));
    }

    public int getApprovedMaxForUserMonthYear(String username, int month, int year) {
        return requests.values().stream()
                .filter(r -> username.equals(r.getUsername())
                        && r.getMonth() == month && r.getYear() == year
                        && BookingExtensionRequest.APPROVED.equals(r.getStatus()))
                .findFirst()
                .map(BookingExtensionRequest::getRequestedDays)
                .orElse(10);
    }

    public List<BookingExtensionRequest> findPendingRequests() {
        return findByStatus(BookingExtensionRequest.PENDING);
    }
}
