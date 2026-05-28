# How to Run the Seat Sync

## Prerequisites

1. **Java 17 or higher** - Check if installed:
   ```bash
   java -version
   ```
   If not installed, download from: https://adoptium.net/

2. **Node.js 16+** (for `npm start`) - Check with `node -version`

3. **Maven** (optional) - `npm start` will download Maven into `./maven_local` automatically if it is not installed. You can also install Maven yourself:
   ```bash
   mvn -version
   ```
   - **macOS**: `brew install maven`

## Running the Application

### Option 1: Using npm Scripts (Recommended)

If you have Node.js installed, use the npm scripts in `package.json`. On first run, Maven may be downloaded once into `./maven_local` (gitignored).

1. **Run the application**:
   ```bash
   npm install   # optional; no npm dependencies, but validates Node is available
   npm start
   ```

2. **Run in development mode (with automatic hot swap / DevTools restart)**:
   ```bash
   npm run dev
   ```

3. **Build the production package (JAR)**:
   ```bash
   npm run build
   ```

### Option 2: Using the Run Script

```bash
./run.sh
```

### Option 3: Using Maven Directly

1. **First time setup** - Download dependencies:
   ```bash
   mvn clean install
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

### Option 4: Build JAR and Run

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
