#!/bin/bash

echo "Building and running Seat Sync..."
echo ""

# Determine Maven command
if [ -f "./maven_local/apache-maven-3.9.9/bin/mvn" ]; then
    MVN_CMD="./maven_local/apache-maven-3.9.9/bin/mvn"
elif command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
else
    echo "Error: Maven is not installed. Please install Maven first, or download it to ./maven_local."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null
then
    echo "Error: Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Build and run
$MVN_CMD clean spring-boot:run

