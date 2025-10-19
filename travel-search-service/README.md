# Travel Search Service

A Spring Boot microservice for searching flights in the Flight Booking System.

## Features

- **Flight Search API** - Search flights by source, destination, and date
- **Multiple Sorting Options** - Sort by price, time, duration, or stops
- **Airline Filtering** - Filter flights by preferred airlines
- **Stop Filtering** - Filter by maximum number of stops
- **Type-Safe Enums** - Uses enums for sorting options

## API Endpoints

### Search Flights
```
GET /travel-search/api/v1/search/flights
```

**Parameters:**
- `source` (required) - Source location
- `destination` (required) - Destination location  
- `flightDate` (required) - Flight date in YYYY-MM-DD format
- `departner` (optional) - List of preferred airlines
- `maximumStops` (optional) - List of maximum stops allowed (default: 0)
- `sortBy` (optional) - Sort option: "price", "time", "duration", "stops" (default: "price")

**Example:**
```
GET /travel-search/api/v1/search/flights?source=Delhi&destination=Mumbai&flightDate=2024-01-15&sortBy=price
```

## Running the Service

### Prerequisites
- Java 17+
- Maven 3.6+

### Build and Run
```bash
mvn clean install
mvn spring-boot:run
```

The service will start on port 8081 with context path `/travel-search`.

## Configuration

The service can be configured via `application.properties`:
- Server port: `server.port=8081`
- Context path: `server.servlet.context-path=/travel-search`
- Logging levels and management endpoints

## Architecture

- **Controller Layer** - REST API endpoints
- **Service Layer** - Business logic and flight search
- **DTO Layer** - Data transfer objects with type-safe enums
- **Mock Data** - Generates sample flight data for testing

## Technology Stack

- Spring Boot 3.5.6
- Kotlin 1.9.25
- Maven
- Java 17
