package com.office.booking.service;

import com.office.booking.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testLoginSuccess() {
        Optional<User> user = userService.login("user1@seatsync.dev", "password1");
        assertTrue(user.isPresent());
        assertEquals("user1@seatsync.dev", user.get().getEmail());
    }

    @Test
    void testLoginFailure() {
        Optional<User> user = userService.login("user1@seatsync.dev", "wrongpassword");
        assertFalse(user.isPresent());
    }

    @Test
    void testLogoutDoesNotThrow() {
        userService.login("user1@seatsync.dev", "password1");
        assertDoesNotThrow(() -> userService.logout("user1@seatsync.dev"));
    }

    @Test
    void testRegisterEmployee() {
        String email = "newuser-" + System.currentTimeMillis() + "@seatsync.dev";
        User created = userService.registerEmployee("company-test", email, "pass1234", "New User");
        assertNotNull(created);
        assertEquals(email, created.getEmail());
        assertEquals("New User", created.getName());
        assertEquals("company-test", created.getCompanyId());
    }

    @Test
    void testFindByUsername() {
        userService.login("user1@seatsync.dev", "password1");
        Optional<User> user = userService.findByEmailOrDisplayName("user1@seatsync.dev");
        assertTrue(user.isPresent());
        assertEquals("user1@seatsync.dev", user.get().getEmail());
    }
}
