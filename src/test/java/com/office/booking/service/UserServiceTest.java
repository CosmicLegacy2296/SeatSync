package com.office.booking.service;

import com.office.booking.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    void testLoginSuccess() {
        Optional<User> user = userService.login("user1", "password1");
        assertTrue(user.isPresent());
        assertEquals("user1", user.get().getUsername());
        assertTrue(user.get().isLoggedIn());
        assertTrue(userService.isUserLoggedIn("user1"));
    }

    @Test
    void testLoginFailure() {
        Optional<User> user = userService.login("user1", "wrongpassword");
        assertFalse(user.isPresent());
        assertFalse(userService.isUserLoggedIn("user1"));
    }

    @Test
    void testLogout() {
        // Login first
        userService.login("user1", "password1");
        assertTrue(userService.isUserLoggedIn("user1"));

        // Logout
        userService.logout("user1");
        assertFalse(userService.isUserLoggedIn("user1"));
        
        Optional<User> loggedInUser = userService.getLoggedInUser("user1");
        assertFalse(loggedInUser.isPresent());
    }

    @Test
    void testCreateUser() {
        User newUser = userService.createUser("newuser", "newpass", "New User");
        assertNotNull(newUser);
        assertEquals("newuser", newUser.getUsername());
        assertEquals("New User", newUser.getName());
    }

    @Test
    void testGetLoggedInUser() {
        userService.login("user1", "password1");
        Optional<User> user = userService.getLoggedInUser("user1");
        assertTrue(user.isPresent());
        assertEquals("user1", user.get().getUsername());
    }
}
