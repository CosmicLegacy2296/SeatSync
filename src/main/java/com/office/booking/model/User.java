package com.office.booking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {
    @Id
    private String username;
    private String password;
    private String name;
    @Transient
    private boolean loggedIn;
    private int maxAllowedDays = 10;
    /** ID of the company this user belongs to; null for legacy/dev accounts */
    private String companyId;

    public User() {
    }

    public User(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.loggedIn = false;
        this.maxAllowedDays = 10;
        this.companyId = null;
    }

    public User(String username, String password, String name, String companyId) {
        this(username, password, name);
        this.companyId = companyId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public int getMaxAllowedDays() {
        return maxAllowedDays;
    }

    public void setMaxAllowedDays(int maxAllowedDays) {
        this.maxAllowedDays = maxAllowedDays;
    }

    /** @return the company ID this user belongs to, or null for dev/legacy accounts */
    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
