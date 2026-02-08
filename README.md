# Office Seat Booking System

A Java-based web application for employees to book office seats. Built with Spring Boot and HTML/Thymeleaf.

## Features

- **User Authentication**: Login/logout functionality with session management
- **Monthly Calendar**: View and select dates for February through December 2026
- **Seat Selection**: Choose seats from Floor 1 and Floor 2 (Ground floor has no seats)
- **Booking Validation**: 
  - Minimum 6 days required per month
  - Maximum 10 days allowed per month
  - Prevents double-booking of seats
- **Persistent Storage**: User bookings are saved and persist across sessions

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Setup Instructions

1. **Install Dependencies**:
   ```bash
   mvn clean install
   ```

2. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Access the Application**:
   Open your browser and navigate to: `http://localhost:8080`

## Default Test Accounts

- **Username**: `user1`, **Password**: `password1`
- **Username**: `user2`, **Password**: `password2`
- **Username**: `admin`, **Password**: `admin`

## Usage

1. **Login**: Use one of the test accounts to log in
2. **Select Month**: Choose a month from February to December 2026
3. **Select Dates**: Click on dates in the calendar (minimum 6, maximum 10)
4. **Choose Seats**: Select seats for each date from Floor 1 or Floor 2
5. **Confirm Bookings**: Review and confirm your bookings
6. **Logout**: Log out when done (next user can log in)

## Project Structure

```
src/
├── main/
│   ├── java/com/office/booking/
│   │   ├── SeatBookingApplication.java    # Main application class
│   │   ├── controller/                    # REST controllers
│   │   │   ├── AuthController.java        # Authentication endpoints
│   │   │   └── BookingController.java     # Booking endpoints
│   │   ├── model/                         # Data models
│   │   │   ├── User.java
│   │   │   ├── Booking.java
│   │   │   └── Floor.java
│   │   └── service/                       # Business logic
│   │       ├── UserService.java
│   │       └── BookingService.java
│   └── resources/
│       ├── templates/                     # HTML templates
│       │   ├── login.html
│       │   ├── dashboard.html
│       │   ├── calendar.html
│       │   └── select-seats.html
│       ├── static/css/
│       │   └── style.css                 # Styling
│       └── application.properties        # Configuration
└── pom.xml                                # Maven configuration
```

## Booking Rules

- **Minimum Days**: Employees must book at least 6 days per month
- **Maximum Days**: Employees can book up to 10 days per month
- **Seat Availability**: Each seat can only be booked once per date
- **Floor Restrictions**: Only Floor 1 and Floor 2 have seats (Ground floor has none)

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.2.0
- **Frontend**: HTML, CSS, JavaScript
- **Templating**: Thymeleaf
- **Build Tool**: Maven

## Notes

- Data is currently stored in memory (ConcurrentHashMap)
- To persist data across server restarts, consider implementing database storage
- The application runs on port 8080 by default (configurable in `application.properties`)
