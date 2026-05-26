package com.office.booking.service;

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
        // (Passwords remain plaintext for dev/demo.)
        createUserIfNotExists("user1", "password1", "John Doe");
        createUserIfNotExists("user2", "password2", "Jane Smith");
        createUserIfNotExists("user3", "password3", "User Three");
        createUserIfNotExists("admin", "admin", "Admin User");
    }

    private void createUserIfNotExists(String username, String password, String name) {
        if (username == null || username.isBlank()) return;
        if (!userRepository.existsById(username)) {
            userRepository.save(new User(username, password, name));
        }
    }

    public void logout(String username) {
        // No persisted "logged in" state; session controls auth.
    }

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return userRepository.findById(username);
    }

    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) return false;
        return userRepository.existsById(username);
    }

    public Optional<User> login(String username, String password) {
        if (username == null || username.isBlank() || password == null) return Optional.empty();
        return userRepository.findById(username)
                .filter(u -> u.getPassword() != null && u.getPassword().equals(password));
    }

    public void setMaxAllowedDays(String username, int maxAllowedDays) {
        if (username == null || username.isBlank()) return;
        userRepository.findById(username).ifPresent(u -> {
            u.setMaxAllowedDays(maxAllowedDays);
            userRepository.save(u);
        });
    }

    public int getMaxAllowedDays(String username) {
        if (username == null || username.isBlank()) return 10;
        return userRepository.findById(username)
                .map(User::getMaxAllowedDays)
                .orElse(10);
    }

    /**
     * Registers a new employee scoped to a specific company.
     * Used by company owners via the company dashboard.
     *
     * @param companyId the UUID of the company this employee belongs to
     * @param username  the desired username (must be unique)
     * @param password  the temporary plaintext password
     * @param name      the employee's full name
     * @return the created User, or null if username is already taken
     */
    public User registerEmployee(String companyId, String username, String password, String name) {
        if (username == null || username.isBlank()) return null;
        if (userRepository.existsById(username)) {
            return null; // username already taken
        }
        User user = new User(username, password, name, companyId);
        return userRepository.save(user);
    }

    /**
     * Updates an existing user's company membership.
     *
     * @return updated user, or empty if the user doesn't exist.
     */
    public Optional<User> assignCompanyToUser(String username, String companyId) {
        if (username == null || username.isBlank()) return Optional.empty();
        return userRepository.findById(username).map(u -> {
            u.setCompanyId(companyId);
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
}
