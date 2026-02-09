# How to Run the Office Seat Booking System

## Prerequisites

1. **Java 17 or higher** - Check if installed:
   ```bash
   java -version
   ```
   If not installed, download from: https://adoptium.net/

2. **Maven 3.6 or higher** - Check if installed:
   ```bash
   mvn -version
   ```
   If not installed:
   - **macOS**: `brew install maven`
   - **Linux**: `sudo apt-get install maven` (Ubuntu/Debian) or `sudo yum install maven` (CentOS/RHEL)
   - **Windows**: Download from https://maven.apache.org/download.cgi

## Running the Application

### Option 1: Using the Run Script (Recommended)

```bash
./run.sh
```

### Option 2: Using Maven Directly

1. **First time setup** - Download dependencies:
   ```bash
   mvn clean install
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

### Option 3: Build JAR and Run

1. **Build the JAR file**:
   ```bash
   mvn clean package
   ```

2. **Run the JAR**:
   ```bash
   java -jar target/seat-booking-system-1.0.0.jar
   ```

## Access the Application

Once the application starts, you should see:
```
Started SeatBookingApplication in X.XXX seconds
```

Open your web browser and navigate to:
```
http://localhost:8080
```

## Test Accounts

- **Username**: `user1`, **Password**: `password1`
- **Username**: `user2`, **Password**: `password2`
- **Username**: `admin`, **Password**: `admin`

## Troubleshooting

### Port 8080 Already in Use

If you get an error that port 8080 is already in use:

1. **Option 1**: Stop the other application using port 8080
2. **Option 2**: Change the port in `src/main/resources/application.properties`:
   ```
   server.port=8081
   ```
   Then access at `http://localhost:8081`

### Maven Dependencies Not Downloading

If Maven can't download dependencies:

1. Check your internet connection
2. Try: `mvn clean install -U` (force update)
3. Check Maven settings: `~/.m2/settings.xml`

### Java Version Issues

Make sure you have Java 17 or higher:
```bash
java -version
```

If you have multiple Java versions, set JAVA_HOME:
```bash
export JAVA_HOME=/path/to/java17
```

## Stopping the Application

Press `Ctrl+C` in the terminal where the application is running.
