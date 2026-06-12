package com.office.booking.service;

import com.office.booking.model.Company;
import com.office.booking.model.User;
import com.office.booking.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        seedDefaultUsersIfNeeded();
    }

    private void seedDefaultUsersIfNeeded() {
        // Seed basic accounts so the UI doesn't break on first run.
        // These are legacy/dev accounts and are not tied to a company (companyId stays null).
        createUserIfNotExists("employee@seatsync.dev", "password1", "Employee One", "EMPLOYEE");
        createUserIfNotExists("admin@seatsync.dev", "password2", "Admin One", "ADMIN");
        createUserIfNotExists("owner@seatsync.dev", "password3", "Owner One", "OWNER");
    }

    private void createUserIfNotExists(String email, String password, String name, String role) {
        if (email == null || email.isBlank()) return;
        if (!userRepository.existsById(email)) {
            User user = new User(email, password, name);
            user.setRole(role == null ? "EMPLOYEE" : role.trim().toUpperCase());
            userRepository.save(user);
        }
    }


    public void logout(String email) {
        // No persisted "logged in" state; session controls auth.
    }

    public Optional<User> findByEmailOrDisplayName(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        Optional<User> byEmail = userRepository.findById(id);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        return userRepository.findByDisplayName(id);
    }

    public boolean existsByEmailOrDisplayName(String id) {
        if (id == null || id.isBlank()) return false;
        return userRepository.existsById(id) || userRepository.existsByDisplayName(id);
    }

    public boolean existsByDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) return false;
        return userRepository.existsByDisplayName(displayName);
    }

    public Optional<User> login(String loginId, String password) {
        if (loginId == null || loginId.isBlank() || password == null) return Optional.empty();
        Optional<User> user = userRepository.findById(loginId);
        if (user.isEmpty()) {
            user = userRepository.findByDisplayName(loginId);
        }
        return user.filter(u -> u.getPassword() != null && u.getPassword().equals(password));
    }

    public void setMaxAllowedDays(String email, int maxAllowedDays) {
        if (email == null || email.isBlank()) return;
        userRepository.findById(email).ifPresent(u -> {
            u.setMaxAllowedDays(maxAllowedDays);
            userRepository.save(u);
        });
    }

    public int getMaxAllowedDays(String email) {
        if (email == null || email.isBlank()) return 10;
        return userRepository.findById(email)
                .map(User::getMaxAllowedDays)
                .orElse(10);
    }

    /**
     * Registers a new employee scoped to a specific company with a custom display name.
     */
    public User registerEmployee(String companyId, String email, String password, String name, String displayName) {
        if (email == null || email.isBlank()) return null;
        if (userRepository.existsById(email)) {
            return null; // email already taken
        }
        if (displayName != null && !displayName.isBlank() && userRepository.existsByDisplayName(displayName)) {
            return null; // displayName already taken
        }
        User user = new User(email, password, name, displayName, companyId);
        return userRepository.save(user);
    }

    /**
     * Registers a new employee scoped to a specific company (auto-generating unique display name).
     * Used by company owners via the company dashboard.
     */
    public User registerEmployee(String companyId, String email, String password, String name) {
        if (email == null || email.isBlank()) return null;
        String displayName = null;
        if (email.contains("@")) {
            displayName = email.substring(0, email.indexOf("@"));
        } else {
            displayName = email;
        }
        
        int count = 1;
        String baseDisplayName = displayName;
        while (userRepository.existsByDisplayName(displayName)) {
            displayName = baseDisplayName + count;
            count++;
        }
        
        return registerEmployee(companyId, email, password, name, displayName);
    }

    /**
     * Updates an existing user's company membership.
     *
     * @return updated user, or empty if the user doesn't exist.
     */
    public Optional<User> assignCompanyToUser(String email, String companyId) {
        return assignCompanyToUser(email, companyId, null, null);
    }

    /**
     * Updates an existing user's company membership and optional profile fields.
     *
     * @return updated user, or empty if the user doesn't exist.
     */
    public Optional<User> assignCompanyToUser(String email, String companyId, String organizationName, String role) {
        if (email == null || email.isBlank()) return Optional.empty();
        return userRepository.findById(email).map(u -> {
            u.setCompanyId(companyId);
            if (organizationName != null && !organizationName.isBlank()) {
                u.setOrganizationName(organizationName.trim());
            }
            if (role != null && !role.isBlank()) {
                u.setRole(role.trim().toUpperCase());
            }
            return userRepository.save(u);
        });
    }

    /**
     * Returns all users belonging to a specific company.
     *
     * @param companyId the company UUID to filter by
     * @return list of users in that company (excludes legacy/dev accounts with null companyId)
     */
    public List<User> getUsersByCompany(String companyId) {
        if (companyId == null || companyId.isBlank()) return List.of();
        return userRepository.findByCompanyId(companyId);
    }

    /**
     * Registers a new admin scoped to a specific company with a custom display name.
     */
    public User registerAdmin(String companyId, String email, String password, String name, String displayName) {
        if (email == null || email.isBlank()) return null;
        if (userRepository.existsById(email)) {
            return null; // email already taken
        }
        if (displayName != null && !displayName.isBlank() && userRepository.existsByDisplayName(displayName)) {
            return null; // displayName already taken
        }
        User user = new User(email, password, name, displayName, companyId, "ADMIN");
        return userRepository.save(user);
    }

    /**
     * Registers a new admin scoped to a specific company (auto-generating unique display name).
     */
    public User registerAdmin(String companyId, String email, String password, String name) {
        if (email == null || email.isBlank()) return null;
        String displayName = null;
        if (email.contains("@")) {
            displayName = email.substring(0, email.indexOf("@"));
        } else {
            displayName = email;
        }
        
        int count = 1;
        String baseDisplayName = displayName;
        while (userRepository.existsByDisplayName(displayName)) {
            displayName = baseDisplayName + count;
            count++;
        }
        
        return registerAdmin(companyId, email, password, name, displayName);
    }

    /**
     * Promotes a user to the ADMIN role.
     */
    public Optional<User> promoteToAdmin(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return userRepository.findById(email).map(u -> {
            u.setRole("ADMIN");
            return userRepository.save(u);
        });
    }

    /**
     * Ensures the company workspace creator is stored as ADMIN (fixes legacy registrations).
     */
    public void ensureWorkspaceOwnerIsAdmin(Company company) {
        if (company == null || company.getOwnerEmail() == null || company.getOwnerEmail().isBlank()) {
            return;
        }
        userRepository.findById(company.getOwnerEmail().toLowerCase()).ifPresent(u -> {
            if (company.getId().equals(u.getCompanyId()) && !"ADMIN".equals(u.getRole())) {
                u.setRole("ADMIN");
                userRepository.save(u);
            }
        });
    }

    /**
     * Demotes an admin back to the EMPLOYEE role.
     */
    public Optional<User> demoteToEmployee(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return userRepository.findById(email).map(u -> {
            u.setRole("EMPLOYEE");
            return userRepository.save(u);
        });
    }
}
