package com.office.booking.service;

import com.office.booking.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> loggedInUsers = new ConcurrentHashMap<>();

    public UserService() {
        // Initialize with some default users for testing
        createUser("user1", "password1", "John Doe");
        createUser("user2", "password2", "Jane Smith");
        createUser("admin", "admin", "Admin User");
    }

    public User createUser(String username, String password, String name) {
        User user = new User(username, password, name);
        users.put(username, user);
        return user;
    }

    public Optional<User> login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            user.setLoggedIn(true);
            loggedInUsers.put(username, user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public void logout(String username) {
        User user = users.get(username);
        if (user != null) {
            user.setLoggedIn(false);
            loggedInUsers.remove(username);
        }
    }

    public Optional<User> getLoggedInUser(String username) {
        return Optional.ofNullable(loggedInUsers.get(username));
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public boolean isUserLoggedIn(String username) {
        return loggedInUsers.containsKey(username);
    }
}
