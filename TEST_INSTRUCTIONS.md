# Test Instructions

## Fixed Issues

### 1. Template Syntax Errors Fixed
- **calendar.html**: Fixed Thymeleaf inline JavaScript syntax for `month` and `userBookings` variables
- **select-seats.html**: Fixed Thymeleaf inline JavaScript syntax for `month` variable

The syntax has been changed from:
```javascript
const month = [[${month}]];
```

To the safer format:
```javascript
const month = /*[[${month}]]*/ 2;
```

This prevents parsing errors and ensures proper Thymeleaf processing.

## Test Files Created

I've created comprehensive test files:

1. **BookingServiceTest.java** - Tests for booking service:
   - Floor initialization
   - Adding bookings
   - Duplicate seat prevention
   - Maximum booking limit (10 days)
   - Minimum booking validation (6 days)
   - Deleting bookings
   - Date availability

2. **UserServiceTest.java** - Tests for user service:
   - Login success/failure
   - Logout functionality
   - User creation
   - Logged-in user retrieval

3. **AuthControllerTest.java** - Tests for authentication controller:
   - Index redirect
   - Login page access

## Running Tests

### Prerequisites
1. **Install Maven** (if not already installed):
   ```bash
   brew install maven
   ```

2. **Verify Maven installation**:
   ```bash
   mvn --version
   ```

### Run All Tests
```bash
cd /Users/krishna.k/Documents/Project-One
mvn clean test
```

### Run Specific Test Class
```bash
# Run BookingService tests only
mvn test -Dtest=BookingServiceTest

# Run UserService tests only
mvn test -Dtest=UserServiceTest

# Run AuthController tests only
mvn test -Dtest=AuthControllerTest
```

### Run Tests with Detailed Output
```bash
mvn clean test -X
```

### Run Tests and Generate Report
```bash
mvn clean test surefire-report:report
# Then open: target/site/surefire-report.html
```

## Expected Test Results

All tests should pass:
- ✅ Floor initialization test
- ✅ Add booking test
- ✅ Duplicate seat prevention test
- ✅ Maximum booking limit test
- ✅ Minimum booking validation test
- ✅ Delete booking test
- ✅ Date availability test
- ✅ Login success/failure tests
- ✅ Logout test
- ✅ User creation test
- ✅ Controller redirect tests

## Running the Application After Tests

Once tests pass, run the application:

```bash
mvn clean spring-boot:run
```

Or use the run script:
```bash
./run.sh
```

## Troubleshooting

### If tests fail:
1. Check Java version: `java -version` (should be 17+)
2. Check Maven version: `mvn --version` (should be 3.6+)
3. Clean and rebuild: `mvn clean install`
4. Check for compilation errors: `mvn compile`

### If Maven dependencies fail to download:
1. Check internet connection
2. Try: `mvn clean install -U` (force update)
3. Clear Maven cache: `rm -rf ~/.m2/repository`
