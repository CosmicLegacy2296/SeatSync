#!/bin/bash

echo "Building and running Office Seat Booking System..."
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null
then
    echo "Error: Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null
then
    echo "Error: Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Build and run
mvn clean spring-boot:run
