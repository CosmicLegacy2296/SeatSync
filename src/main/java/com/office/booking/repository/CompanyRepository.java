package com.office.booking.repository;

import com.office.booking.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, String> {
    Optional<Company> findByOwnerEmailIgnoreCase(String ownerEmail);

    Optional<Company> findByCompanyCodeIgnoreCase(String companyCode);

    boolean existsByOwnerEmailIgnoreCase(String ownerEmail);
}

