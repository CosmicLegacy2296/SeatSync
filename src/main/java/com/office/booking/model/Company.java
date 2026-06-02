package com.office.booking.model;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

/**
 * Represents a registered company in the SeatSync system.
 * Each company has its own workspace, employees, and booking scope.
 *
 * NOTE: Passwords are stored in plaintext for development purposes only.
 * In production, use BCrypt or Argon2 hashing.
 */
@Entity
@Table(name = "companies")
public class Company {

    @Id
    private String id;
    private String companyName;
    private String tradingName;
    private String industry;
    private String companySize;
    private String website;
    private String ownerName;
    private String ownerTitle;
    @Column(nullable = false, unique = true)
    private String ownerEmail;
    private String ownerPassword; // plaintext — hash in production
    private int floor1Seats;
    private int floor2Seats;
    private String startTime;
    private String endTime;
    private String timezone;
    @Lob
    private String logoBase64;    // optional — stored as base64 data URI
    @Column(nullable = false, unique = true)
    private String companyCode;   // short code for employees to join
    @Column(nullable = false, unique = true)
    private String adminCode;     // short code for admins to join
    private LocalDateTime registeredAt;
    @Column(columnDefinition = "TEXT")
    private String floorSeatConfig; // JSON e.g. {"1":20,"2":15}

    public Company() {
        this.id = UUID.randomUUID().toString();
        this.registeredAt = LocalDateTime.now();
        this.floor1Seats = 20;
        this.floor2Seats = 20;
        this.companyCode = generateShortCode();
        this.adminCode = generateAdminCode();
    }

    @PrePersist
    private void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.registeredAt == null) {
            this.registeredAt = LocalDateTime.now();
        }
        if (this.companyCode == null || this.companyCode.isBlank()) {
            this.companyCode = generateShortCode();
        }
        if (this.adminCode == null || this.adminCode.isBlank()) {
            this.adminCode = generateAdminCode();
        }
    }

    private String generateShortCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    private String generateAdminCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("ADM-");
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    // --- Getters & Setters ---

    public String getAdminCode() { return adminCode; }
    public void setAdminCode(String adminCode) { this.adminCode = adminCode; }

    /** @return unique UUID for this company */
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    /** @return the registered legal company name */
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    /** @return trading/brand name; may be null if same as company name */
    public String getTradingName() { return tradingName; }
    public void setTradingName(String tradingName) { this.tradingName = tradingName; }

    /** @return the industry sector (e.g. Technology, Finance) */
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    /** @return company size band (e.g. "1-10", "11-50") */
    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }

    /** @return optional company website URL */
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    /** @return full name of the company owner/admin */
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    /** @return job title of the company owner (e.g. CEO, Founder) */
    public String getOwnerTitle() { return ownerTitle; }
    public void setOwnerTitle(String ownerTitle) { this.ownerTitle = ownerTitle; }

    /** @return work email used as owner's login credential */
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    /** @return plaintext password — hash in production */
    public String getOwnerPassword() { return ownerPassword; }
    public void setOwnerPassword(String ownerPassword) { this.ownerPassword = ownerPassword; }

    /** @return number of bookable seats configured on Floor 1 */
    public int getFloor1Seats() { return floor1Seats; }
    public void setFloor1Seats(int floor1Seats) { this.floor1Seats = floor1Seats; }

    /** @return number of bookable seats configured on Floor 2 */
    public int getFloor2Seats() { return floor2Seats; }
    public void setFloor2Seats(int floor2Seats) { this.floor2Seats = floor2Seats; }

    /** @return workspace start time (e.g. "08:00") */
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    /** @return workspace end time (e.g. "18:00") */
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    /** @return IANA timezone string (e.g. "America/New_York") */
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    /** @return optional base64-encoded logo image data URI */
    public String getLogoBase64() { return logoBase64; }
    public void setLogoBase64(String logoBase64) { this.logoBase64 = logoBase64; }

    /** @return timestamp when the company was registered */
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    /** @return short code for employees to join this company */
    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }

    /**
     * Returns the display name: trading name if set, otherwise company name.
     * @return the display name for this company
     */
    public String getDisplayName() {
        return (tradingName != null && !tradingName.isBlank()) ? tradingName : companyName;
    }

    /** @return JSON floor/seat config string, e.g. {"1":20,"2":15} */
    public String getFloorSeatConfig() { return floorSeatConfig; }
    public void setFloorSeatConfig(String floorSeatConfig) { this.floorSeatConfig = floorSeatConfig; }

    /**
     * Returns total bookable seats across both floors.
     * @return sum of floor1Seats + floor2Seats
     */
    public int getTotalSeats() {
        return floor1Seats + floor2Seats;
    }
}
