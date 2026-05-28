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
    private String email;
    private String password;
    private String name;
    private String displayName;
    @Transient
    private boolean loggedIn;
    private int maxAllowedDays = 10;
    /** ID of the company this user belongs to; null for legacy/dev accounts */
    private String companyId;
    private String role = "EMPLOYEE";

    public User() {
    }

    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.loggedIn = false;
        this.maxAllowedDays = 10;
        this.companyId = null;
        this.role = "EMPLOYEE";
        if (email != null && email.contains("@")) {
            this.displayName = email.substring(0, email.indexOf("@"));
        } else {
            this.displayName = email;
        }
    }

    public User(String email, String password, String name, String companyId) {
        this(email, password, name);
        this.companyId = companyId;
        this.role = "EMPLOYEE";
    }

    public User(String email, String password, String name, String displayName, String companyId) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.displayName = displayName;
        this.companyId = companyId;
        this.role = "EMPLOYEE";
        this.loggedIn = false;
        this.maxAllowedDays = 10;
    }

    public User(String email, String password, String name, String displayName, String companyId, String role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.displayName = displayName;
        this.companyId = companyId;
        this.role = role;
        this.loggedIn = false;
        this.maxAllowedDays = 10;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
