package com.office.booking.service;

import com.office.booking.model.BookingExtensionRequest;
import com.office.booking.repository.BookingExtensionRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExtensionRequestService {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private BookingExtensionRequestRepository requestRepository;
    private final Map<Long, BookingExtensionRequest> requests = new ConcurrentHashMap<>();

    public BookingExtensionRequest save(BookingExtensionRequest request) {
        if (requestRepository != null) {
            return requestRepository.save(request);
        }
        requests.put(request.getId(), request);
        return request;
    }

    public Optional<BookingExtensionRequest> findById(Long id) {
        if (requestRepository != null) {
            return requestRepository.findById(id);
        }
        return Optional.ofNullable(requests.get(id));
    }

    public List<BookingExtensionRequest> findByStatus(String status) {
        if (requestRepository != null) {
            return requestRepository.findByStatus(status);
        }
        return requests.values().stream()
                .filter(r -> status.equals(r.getStatus()))
                .toList();
    }

    public List<BookingExtensionRequest> findPendingRequests(String companyId) {
        if (requestRepository != null) {
            return requestRepository.findByCompanyIdAndStatus(companyId, BookingExtensionRequest.PENDING);
        }
        return requests.values().stream()
                .filter(r -> companyId.equals(r.getCompanyId())
                        && BookingExtensionRequest.PENDING.equals(r.getStatus()))
                .toList();
    }

    public boolean hasPendingRequest(String companyId, String username, int month, int year) {
        if (requestRepository != null) {
            return requestRepository.existsByCompanyIdAndUsernameAndMonthAndYearAndStatus(
                    companyId, username, month, year, BookingExtensionRequest.PENDING);
        }
        return requests.values().stream()
                .anyMatch(r -> companyId.equals(r.getCompanyId())
                        && username.equals(r.getUsername())
                        && r.getMonth() == month && r.getYear() == year
                        && BookingExtensionRequest.PENDING.equals(r.getStatus()));
    }

    public int getApprovedMaxForUserMonthYear(String companyId, String username, int month, int year) {
        if (requestRepository != null) {
            return requestRepository.findTopByCompanyIdAndUsernameAndMonthAndYearAndStatusOrderByIdDesc(
                            companyId, username, month, year, BookingExtensionRequest.APPROVED)
                    .map(BookingExtensionRequest::getRequestedDays)
                    .orElse(10);
        }
        return requests.values().stream()
                .filter(r -> companyId.equals(r.getCompanyId())
                        && username.equals(r.getUsername())
                        && r.getMonth() == month && r.getYear() == year
                        && BookingExtensionRequest.APPROVED.equals(r.getStatus()))
                .findFirst()
                .map(BookingExtensionRequest::getRequestedDays)
                .orElse(10);
    }

    public boolean hasPendingRequest(String username, int month, int year) {
        if (requestRepository != null) {
            return requestRepository.existsByUsernameAndMonthAndYearAndStatus(
                    username, month, year, BookingExtensionRequest.PENDING);
        }
        return requests.values().stream()
            .anyMatch(r -> username.equals(r.getUsername())
                && r.getMonth() == month
                && r.getYear() == year
                && BookingExtensionRequest.PENDING.equals(r.getStatus()));
    }

    public int getApprovedMaxForUserMonthYear(String username, int month, int year) {
        if (requestRepository != null) {
            return requestRepository.findTopByUsernameAndMonthAndYearAndStatusOrderByIdDesc(
                            username, month, year, BookingExtensionRequest.APPROVED)
                    .map(BookingExtensionRequest::getRequestedDays)
                    .orElse(10);
        }
        return requests.values().stream()
            .filter(r -> username.equals(r.getUsername())
                && r.getMonth() == month
                && r.getYear() == year
                && BookingExtensionRequest.APPROVED.equals(r.getStatus()))
            .findFirst()
            .map(BookingExtensionRequest::getRequestedDays)
            .orElse(10);
    }

    public List<BookingExtensionRequest> findPendingRequests() {
        return findByStatus(BookingExtensionRequest.PENDING);
    }
}
