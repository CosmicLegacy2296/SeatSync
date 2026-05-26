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
        Optional<User> user = userService.login("user1", "password1");
        assertTrue(user.isPresent());
        assertEquals("user1", user.get().getUsername());
    }

    @Test
    void testLoginFailure() {
        Optional<User> user = userService.login("user1", "wrongpassword");
        assertFalse(user.isPresent());
    }

    @Test
    void testLogoutDoesNotThrow() {
        userService.login("user1", "password1");
        assertDoesNotThrow(() -> userService.logout("user1"));
    }

    @Test
    void testRegisterEmployee() {
        String username = "newuser-" + System.currentTimeMillis();
        User created = userService.registerEmployee("company-test", username, "pass1234", "New User");
        assertNotNull(created);
        assertEquals(username, created.getUsername());
        assertEquals("New User", created.getName());
        assertEquals("company-test", created.getCompanyId());
    }

    @Test
    void testFindByUsername() {
        userService.login("user1", "password1");
        Optional<User> user = userService.findByUsername("user1");
        assertTrue(user.isPresent());
        assertEquals("user1", user.get().getUsername());
    }
}
