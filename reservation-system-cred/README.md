# Flight Booking System - Multi-Service Architecture

A comprehensive flight booking system built with **Spring Boot**, **Kotlin**, and **microservices architecture**. The system supports multi-stop flight bookings with segment-based seat management and distributed caching using Redis.

## 🏗️ System Architecture

### High-Level Design
```
Flight Booking System Architecture
┌─────────┐    ┌─────────────────────────────────┐    ┌─────────────────────┐
│ Client  │────▶│                                │────▶│ Users               │
│         │    │                                │    │ Service             │
└─────────┘    │                                │    └─────────────────────┘
               │                                │
┌─────────┐    │ Load Balancer & API Gateway    │    ┌─────────────────────┐
│ Client  │────▶│                                │────▶│ Search              │
│         │    │ • Authentication               │    │ Service             │
└─────────┘    │ • Internal Routing             │    └─────────────────────┘
               │ • Rate Limiting                │
┌─────────┐    │                                │    ┌─────────────────────┐
│ Client  │────▶│                                │────▶│ Reservation System  │
│         │    └─────────────────────────────────┘    │ (Core)              │
└─────────┘                                           └─────────────────────┘
                                                               │
                                                               ▼
                                                      ┌─────────────────────┐
                                                      │ Payment Gateway     │
                                                      └─────────────────────┘
```

### Service Architecture
```
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│ Customer Profile    │ │ Reservation System  │ │ Travel Search       │
│ Service             │ │ (Core)              │ │ Service             │
│                     │ │                     │ │                     │
│ - User Management   │ │ - Flight Booking    │ │ - Flight Search     │
│ - Profile Data      │ │ - Multi-Stop        │ │ - Route Planning    │
│ - Authentication    │ │ - Seat Management   │ │ - Price Comparison  │
│                     │ │ - Redis Cache       │ │                     │
└─────────────────────┘ └─────────────────────┘ └─────────────────────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │
                 ┌─────────────────────────────────┐
                 │ Shared Resources                │
                 │                                 │
                 │ • PostgreSQL Database           │
                 │ • Cassandra (Flight Data)       │
                 │ • Redis Cache                   │
                 │ • Prometheus Metrics            │
                 └─────────────────────────────────┘
```

## 🚀 Features

### Core Features
- ✅ **Multi-Stop Flight Booking** - Book segments of flights with layovers
- ✅ **Segment-Based Seat Management** - Intelligent seat allocation across flight segments
- ✅ **Distributed Caching** - Redis-powered seat booking cache
- ✅ **Concurrent Booking Protection** - Race condition prevention
- ✅ **Real-time Seat Availability** - Live seat status updates
- ✅ **Comprehensive Flight Search** - Multi-criteria flight discovery

### Technical Features
- ✅ **Microservices Architecture** - Scalable service separation
- ✅ **Spring Boot + Kotlin** - Modern JVM stack
- ✅ **Multi-Database Support** - PostgreSQL + Cassandra + Redis + Elasticsearch
- ✅ **Metrics & Monitoring** - Prometheus integration
- ✅ **Containerized Deployment** - Docker support
- ✅ **Comprehensive Testing** - Unit + Integration tests

## 📁 Repository Structure

```
flight-system/
├── docs/                           # Documentation
│   └── images/                     # Architecture diagrams
├── customer-profile-service-cred/  # User Management Service
│   └── customer-profile-service/
├── travel-search-service-cred/     # Flight Search Service
│   └── travel-search-service/
├── reservation-system-cred/        # Core Booking Service (Current)
│   └── reservation-system/
├── docker-compose.yml              # Infrastructure setup
├── Dockerfile                      # Application container
├── DOCKER-GUIDE.md                 # Docker deployment guide
└── README.md                       # This file
```

## 🔧 Tech Stack

- **Backend**: Spring Boot 3.5.6, Kotlin 1.9.25, Java 17
- **Databases**: PostgreSQL, Cassandra, Redis, Elasticsearch
- **Caching**: Redis with TTL-based seat booking
- **Monitoring**: Prometheus, Grafana, Spring Actuator
- **Testing**: JUnit 5, MockK, TestContainers
- **Containerization**: Docker, Docker Compose
- **Build Tool**: Maven

## 📊 API Documentation

### 🎫 Reservation System APIs (Core Service)

#### Booking Management
| Method | Endpoint | Description | Headers | Body |
|--------|----------|-------------|---------|------|
| `POST` | `/api/book-tickets` | Create new booking | `user-id: UUID` | `BookingRequest` |
| `DELETE` | `/api/book-tickets/{bookingId}` | Cancel booking | `user-id: UUID` | - |
| `GET` | `/api/book-tickets/{bookingId}` | Get booking details | `user-id: UUID` | - |

#### Flight Management (Admin)
| Method | Endpoint | Description | Headers | Body |
|--------|----------|-------------|---------|------|
| `POST` | `/api/book-tickets/flights` | Create flight | `admin-id: UUID` | `FlightCreationRequest` |
| `GET` | `/api/book-tickets/flights/search` | Search flights | - | - |
| `GET` | `/api/book-tickets/flights/{flightNumber}` | Get flight details | - | - |
| `PUT` | `/api/book-tickets/flights/{flightNumber}/seats/{seatId}/status` | Update seat status | `admin-id: UUID` | `SeatStatusUpdateRequest` |
| `DELETE` | `/api/book-tickets/flights/{flightNumber}` | Cancel flight | `admin-id: UUID` | - |

#### Cache Management
| Method | Endpoint | Description | Headers | Body |
|--------|----------|-------------|---------|------|
| `GET` | `/api/book-tickets/cache/health` | Redis health check | - | - |
| `DELETE` | `/api/book-tickets/cache/blocked-seats` | Clear all blocked seats | `admin-id: UUID` | - |

### 🗓️ Flight Schedule Management
| Method | Endpoint | Description | Body |
|--------|----------|-------------|------|
| `POST` | `/api/flight-schedules` | Create flight schedule | `List<ScheduleItem>` |
| `GET` | `/api/flight-schedules/flight/{flightId}` | Get flight schedules | - |
| `GET` | `/api/flight-schedules` | Get all schedules | - |
| `PUT` | `/api/flight-schedules/flight/{flightId}/seat/{seatId}/status` | Update seat status | - |
| `POST` | `/api/flight-schedules/flight/{flightId}/seat/{seatId}/confirm` | Confirm blocked seat | - |
| `GET` | `/api/flight-schedules/blocked-seats/{flightId}` | Get blocked seats | - |
| `DELETE` | `/api/flight-schedules/{scheduleId}` | Delete schedule | - |

### 🪑 Seat Management
| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| `POST` | `/api/seats` | Create seat | `vehicleId`, `seatNumber`, `seatType`, `isWindowSeat` |
| `GET` | `/api/seats/vehicle/{vehicleId}` | Get seats by vehicle | - |
| `GET` | `/api/seats/vehicle/{vehicleId}/available` | Get available seats | - |
| `GET` | `/api/seats/vehicle/{vehicleId}/window` | Get window seats | - |
| `GET` | `/api/seats/vehicle/{vehicleId}/type/{seatType}` | Get seats by type | - |
| `GET` | `/api/seats/vehicle/{vehicleId}/seat/{seatNumber}` | Get specific seat | - |
| `GET` | `/api/seats/vehicle/{vehicleId}/count/available` | Get available count | - |
| `PUT` | `/api/seats/{seatId}/availability` | Update availability | `isAvailable` |
| `DELETE` | `/api/seats/{seatId}` | Delete seat | - |

### 🚗 Vehicle Management
| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| `POST` | `/api/vehicles` | Create vehicle | `vehicleType`, `ownerType` |
| `GET` | `/api/vehicles` | Get all vehicles | - |
| `GET` | `/api/vehicles/{vehicleId}` | Get vehicle by ID | - |
| `GET` | `/api/vehicles/type/{vehicleType}` | Get vehicles by type | - |
| `GET` | `/api/vehicles/available` | Get available vehicles | - |
| `GET` | `/api/vehicles/available/type/{vehicleType}` | Get available by type | - |
| `PUT` | `/api/vehicles/{vehicleId}/availability` | Update availability | `isAvailable` |
| `DELETE` | `/api/vehicles/{vehicleId}` | Delete vehicle | - |

### 🏙️ City Management
| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| `POST` | `/api/cities` | Create city mapping | `cityCode`, `cityName` |
| `GET` | `/api/cities` | Get all cities | - |
| `GET` | `/api/cities/code/{cityCode}` | Get city by code | - |
| `GET` | `/api/cities/name/{cityName}` | Get city by name | - |
| `GET` | `/api/cities/search` | Search cities | `cityName` |
| `PUT` | `/api/cities/{id}` | Update city | `cityCode`, `cityName` |
| `DELETE` | `/api/cities/{id}` | Delete city | - |
| `POST` | `/api/cities/initialize` | Initialize default cities | - |

### 🔄 Booking Status Management
| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| `PUT` | `/api/booking-status/release-seat` | Release seat and update status | `flightId`, `seatId`, `flightTime` |
| `PUT` | `/api/booking-status/release-seat-by-key` | Release seat by key | `seatKey` |
| `GET` | `/api/booking-status/seat-status` | Get seat status | `flightId`, `seatId`, `flightTime` |
| `PUT` | `/api/booking-status/unblock-seat` | Unblock seat | `flightId`, `seatId`, `flightTime` |

### 🗄️ Seat Cache Management (Admin)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/admin/seat-cache/blocked-seats` | Get all blocked seats |
| `GET` | `/api/admin/seat-cache/seat-status/{seatKey}` | Get seat status by key |
| `DELETE` | `/api/admin/seat-cache/clear-all` | Clear all blocked seats |
| `DELETE` | `/api/admin/seat-cache/release/{seatKey}` | Release specific seat |

### 📊 Metrics & Monitoring
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/metrics/dashboard` | Get dashboard metrics |
| `GET` | `/api/metrics/health-check` | Get health metrics |
| `GET` | `/api/metrics/performance` | Get performance metrics |
| `GET` | `/actuator/health` | Spring Boot health check |
| `GET` | `/actuator/prometheus` | Prometheus metrics |
| `GET` | `/actuator/metrics` | Application metrics |

## 🎯 API Usage Examples

### Booking a Flight
```bash
# 1. Search for flights
curl -X GET "http://localhost:8080/api/book-tickets/flights/search?sourceCode=DEL&destinationCode=BOM&date=2024-01-15"

# 2. Create booking
curl -X POST "http://localhost:8080/api/book-tickets" \
  -H "Content-Type: application/json" \
  -H "user-id: 123e4567-e89b-12d3-a456-426614174000" \
  -d '{
    "bookingType": "FLIGHT",
    "bookingDetails": {
      "flightId": "AI101",
      "seatId": "12A",
      "flightTime": "10:30"
    }
  }'

# 3. Check booking status
curl -X GET "http://localhost:8080/api/book-tickets/{bookingId}?bookingType=FLIGHT" \
  -H "user-id: 123e4567-e89b-12d3-a456-426614174000"
```

### Admin Operations
```bash
# Create a flight
curl -X POST "http://localhost:8080/api/book-tickets/flights" \
  -H "Content-Type: application/json" \
  -H "admin-id: 123e4567-e89b-12d3-a456-426614174000" \
  -d '{
    "flightNumber": "AI101",
    "sourceCode": "DEL",
    "destinationCode": "BOM",
    "travelStartTime": "10:30",
    "travelEndTime": "12:30"
  }'

# Check Redis cache health
curl -X GET "http://localhost:8080/api/book-tickets/cache/health"
```

### Monitoring
```bash
# Application health
curl -X GET "http://localhost:8080/actuator/health"

# Metrics dashboard
curl -X GET "http://localhost:8080/api/metrics/dashboard"

# Prometheus metrics
curl -X GET "http://localhost:8080/actuator/prometheus"
```

## 🐳 Docker Deployment

### Quick Start
```bash
# Clone repository
git clone <repository-url>
cd reservation-system-cred

# Start complete system
chmod +x docker-run.sh
./docker-run.sh
```

### Manual Docker Commands
```bash
# Build application
docker build -t reservation-system:latest .

# Start infrastructure
docker-compose up -d postgres redis cassandra

# Start application
docker-compose up -d --build reservation-system
```

For detailed Docker setup instructions, see [DOCKER-GUIDE.md](./DOCKER-GUIDE.md).

## 🔍 Service URLs (Docker)

| Service | URL | Credentials |
|---------|-----|-------------|
| **Reservation API** | http://localhost:8080 | - |
| **Health Check** | http://localhost:8080/actuator/health | - |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | admin/admin |
| **PostgreSQL** | localhost:5432 | reservation_user/reservation_pass |
| **Redis** | localhost:6379 | No password |
| **Cassandra** | localhost:9042 | No auth |

## 🧪 Testing

### Run Tests
```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw test -Dtest=**/*IntegrationTest

# With Docker
docker-compose exec reservation-system ./mvnw test
```

### Test Coverage
- **Unit Tests**: Service layer, controllers, utilities
- **Integration Tests**: Database operations, Redis caching
- **Mock Testing**: External service interactions
- **Container Testing**: TestContainers for database integration

## 📈 Monitoring & Metrics

### Built-in Metrics
- **Booking Metrics**: Creation, cancellation, confirmation rates
- **Cache Metrics**: Hit/miss ratios, blocked seats count
- **Database Metrics**: Connection pool, query performance
- **JVM Metrics**: Memory usage, garbage collection
- **Custom Business Metrics**: Flight searches, seat availability

### Health Checks
- **Database Connectivity**: PostgreSQL, Cassandra, Redis
- **Service Health**: Application components status
- **Cache Health**: Redis connection and performance
- **External Dependencies**: Payment gateway, search service

## 🔒 Security Considerations

### Current Implementation
- **Input Validation**: Request parameter validation
- **Error Handling**: Secure error responses
- **Logging**: Comprehensive audit trails

### Recommended Enhancements
- **Authentication**: JWT token-based authentication
- **Authorization**: Role-based access control (RBAC)
- **Rate Limiting**: API throttling and protection
- **HTTPS**: Secure communication
- **API Keys**: Service-to-service authentication

## 🚀 Performance Optimization

### Current Features
- **Redis Caching**: Distributed seat booking cache
- **Connection Pooling**: Database connection optimization
- **Async Processing**: Non-blocking operations with coroutines
- **Metrics Collection**: Performance monitoring

### Scaling Strategies
- **Horizontal Scaling**: Multiple service instances
- **Database Sharding**: Distribute data across nodes
- **Cache Clustering**: Redis cluster setup
- **Load Balancing**: Distribute traffic efficiently

## 🤝 Contributing

### Development Setup
1. **Prerequisites**: Java 17, Maven, Docker
2. **Database Setup**: Run `docker-compose up -d postgres redis cassandra`
3. **Application Start**: `./mvnw spring-boot:run`
4. **Testing**: `./mvnw test`

### Code Standards
- **Language**: Kotlin with Spring Boot
- **Testing**: Comprehensive unit and integration tests
- **Documentation**: API documentation with examples
- **Logging**: Structured logging with correlation IDs

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Documentation
- **API Documentation**: This README
- **Docker Guide**: [DOCKER-GUIDE.md](./DOCKER-GUIDE.md)
- **Redis Setup**: [REDIS_SETUP.md](./REDIS_SETUP.md)

### Contact
For questions, issues, or contributions:
1. **Create an issue** in the repository
2. **Check existing documentation** first
3. **Provide detailed information** about your setup and issue

---

## 🎯 Quick Commands Reference

```bash
# Development
./mvnw spring-boot:run                    # Start application
./mvnw test                               # Run tests
./mvnw clean package                      # Build application

# Docker
./docker-run.sh                           # Start complete system
docker-compose up -d                      # Start infrastructure
docker-compose logs -f reservation-system # View logs

# Health Checks
curl http://localhost:8080/actuator/health # Application health
curl http://localhost:8080/api/metrics/dashboard # Metrics dashboard

# Database
docker-compose exec postgres psql -U reservation_user -d reservation_db
docker-compose exec redis redis-cli
docker-compose exec cassandra cqlsh
```

Built with ❤️ using Spring Boot, Kotlin, and modern microservices architecture.
