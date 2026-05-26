package com.office.booking.service;

import com.office.booking.model.Company;
import com.office.booking.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for managing company registrations in SeatSync.
 */
@Service
public class CompanyService {
    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * Registers a new company, assigns a UUID, and indexes by owner email.
     *
     * @param company the company object populated from the registration form
     * @return the saved company (with id and registeredAt populated)
     * @throws IllegalArgumentException if a company is already registered with the same owner email
     */
    public Company registerCompany(Company company) {
        if (company.getOwnerEmail() != null && companyRepository.existsByOwnerEmailIgnoreCase(company.getOwnerEmail())) {
            throw new IllegalArgumentException("A company is already registered with the email: " + company.getOwnerEmail());
        }
        if (company.getOwnerEmail() != null) {
            company.setOwnerEmail(company.getOwnerEmail().trim().toLowerCase());
        }
        return companyRepository.save(company);
    }

    /**
     * Retrieves a company by its unique ID.
     *
     * @param id the company UUID
     * @return an Optional containing the company, or empty if not found
     */
    public Optional<Company> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return companyRepository.findById(id);
    }

    /**
     * Looks up a company by the owner's email address (case-insensitive).
     *
     * @param email the owner's work email
     * @return an Optional containing the company, or empty if not found
     */
    public Optional<Company> findByOwnerEmail(String email) {
        if (email == null) return Optional.empty();
        return companyRepository.findByOwnerEmailIgnoreCase(email);
    }

    /**
     * Looks up a company by its generated short code.
     *
     * @param code the company code
     * @return an Optional containing the company, or empty if not found
     */
    public Optional<Company> findByCompanyCode(String code) {
        if (code == null) return Optional.empty();
        return companyRepository.findByCompanyCodeIgnoreCase(code);
    }

    /**
     * Returns all registered companies.
     *
     * @return an unmodifiable list of all companies
     */
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    /**
     * Checks whether the given email is already registered as a company owner.
     *
     * @param email the email to check
     * @return true if already registered, false otherwise
     */
    public boolean isEmailRegistered(String email) {
        return email != null && companyRepository.existsByOwnerEmailIgnoreCase(email);
    }

    /**
     * Updates an existing company's details.
     *
     * @param company the company with updated fields (must have a valid id)
     * @return the updated company, or empty if the id was not found
     */
    public Optional<Company> updateCompany(Company company) {
        String id = company.getId();
        if (id == null || id.isBlank() || !companyRepository.existsById(id)) {
            return Optional.empty();
        }
        return Optional.of(companyRepository.save(company));
    }
}
