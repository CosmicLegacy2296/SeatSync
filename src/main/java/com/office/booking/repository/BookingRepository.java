package com.office.booking.repository;

import com.office.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCompanyIdAndUsernameAndMonthAndYear(String companyId, String username, int month, int year);

    List<Booking> findByCompanyIdAndUsername(String companyId, String username);

    Optional<Booking> findByCompanyIdAndUsernameAndDate(String companyId, String username, LocalDate date);

    boolean existsByCompanyIdAndDateAndSeatId(String companyId, LocalDate date, String seatId);

    List<Booking> findByCompanyIdAndSeatIdAndMonthAndYear(String companyId, String seatId, int month, int year);

    List<Booking> findByCompanyIdAndDate(String companyId, LocalDate date);

    List<Booking> findByMonthAndYear(int month, int year);

    Optional<Booking> findByCompanyIdAndDateAndSeatId(String companyId, LocalDate date, String seatId);

    long countByCompanyId(String companyId);
}
