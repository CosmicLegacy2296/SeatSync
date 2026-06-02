package com.office.booking.repository;

import com.office.booking.model.BookingExtensionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingExtensionRequestRepository extends JpaRepository<BookingExtensionRequest, Long> {
    List<BookingExtensionRequest> findByStatus(String status);

    List<BookingExtensionRequest> findByCompanyIdAndStatus(String companyId, String status);

    boolean existsByCompanyIdAndUsernameAndMonthAndYearAndStatus(String companyId, String username, int month, int year, String status);

    boolean existsByUsernameAndMonthAndYearAndStatus(String username, int month, int year, String status);

    Optional<BookingExtensionRequest> findTopByCompanyIdAndUsernameAndMonthAndYearAndStatusOrderByIdDesc(
            String companyId,
            String username,
            int month,
            int year,
            String status);

    Optional<BookingExtensionRequest> findTopByUsernameAndMonthAndYearAndStatusOrderByIdDesc(
            String username,
            int month,
            int year,
            String status);
}
