# Flight Booking System - Multi-Service Architecture

A comprehensive flight booking system built with **Spring Boot**, **Kotlin**, and **microservices architecture**. The system supports multi-stop flight bookings with segment-based seat management and distributed caching using Redis.

## 🏗️ System Architecture

### High-Level Design
![Flight Booking System Architecture](docs/images/flight-system-hld.png)

The system follows a **microservices architecture** with the following components:

### Service Architecture
```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│                     │    │                     │    │                     │
│  Customer Profile   │    │  Reservation System │    │  Travel Search      │
│     Service         │    │     (Core)          │    │     Service         │
│                     │    │                     │    │                     │
│  - User Management  │    │  - Flight Booking   │    │  - Flight Search    │
│  - Profile Data     │    │  - Multi-Stop       │    │  - Route Planning   │
│  - Authentication   │    │  - Seat Management  │    │  - Price Comparison │
│                     │    │  - Redis Cache      │    │                     │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
         │                           │                           │
         └───────────────────────────┼───────────────────────────┘
                                     │
                    ┌─────────────────────────────────┐
                    │        Shared Resources         │
                    │                                 │
                    │  • PostgreSQL Database         │
                    │  • Cassandra (Flight Data)     │
                    │  • Redis Cache                  │
                    │  • Prometheus Metrics          │
                    └─────────────────────────────────┘
```

### Architecture Components

#### **Load Balancer & API Gateway**
- **Authentication** - JWT token validation
- **Internal Routing** - Service discovery and routing
- **Rate Limiting** - API throttling and protection

#### **Microservices**
1. **Users Service** (Customer Profile)
   - User registration and authentication
   - Profile management
   - **Database**: PostgreSQL

2. **Search Service** (Travel Search)
   - Flight search and filtering
   - Route optimization
   - **Database**: Elasticsearch + Cassandra

3. **Book Tickets Service** (Reservation System)
   - Multi-stop flight booking
   - Segment-based seat management
   - **Cache**: Redis (TTL-based)
   - **Databases**: PostgreSQL + Cassandra

#### **Data Layer**
- **PostgreSQL** - User data, bookings, transactions
- **Cassandra** - Flight schedules, seat inventory, high-volume data
- **Redis** - Seat booking cache, session management
- **Elasticsearch** - Flight search indexing

#### **External Integration**
- **Payment Gateway** - Secure payment processing
- **CDC (Change Data Capture)** - Real-time data synchronization

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
- ✅ **Multi-Database Support** - PostgreSQL + Cassandra
- ✅ **Metrics & Monitoring** - Prometheus integration
- ✅ **Containerized Deployment** - Docker support
- ✅ **Comprehensive Testing** - Unit + Integration tests

## 📁 Repository Structure

```
flight-system/
├── docs/
│   └── images/
│       └── flight-system-hld.png          # Add your HLD diagram here
├── customer-profile-service-cred/
│   └── customer-profile-service/
├── travel-search-service-cred/
│   └── travel-search-service/
├── reservation-system-cred/
│   └── reservation-system/
├── docker-compose.yml
└── README.md
```

> **📝 Note**: Please add your HLD diagram image to `docs/images/flight-system-hld.png` for the architecture section to display correctly.

## 📋 Prerequisites

### Required Software
- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Git**

### Required Services
- **PostgreSQL 13+**
- **Apache Cassandra 4.0+**
- **Redis 6.0+**

## 🛠️ Quick Start Guide

### Step 1: Clone the Repository
```bash
git clone https://github.com/RakshitKhullar/flight-system.git
cd flight-system
```

### Step 2: Start Infrastructure Services
```bash
# Start PostgreSQL, Cassandra, and Redis using Docker
docker-compose up -d postgres cassandra redis

# Wait for services to be ready (30-60 seconds)
docker-compose logs -f postgres cassandra redis
```

### Step 3: Verify Infrastructure
```bash
# Check PostgreSQL
docker exec -it flight-postgres psql -U postgres -c "\l"

# Check Cassandra
docker exec -it flight-cassandra cqlsh -e "DESCRIBE KEYSPACES;"

# Check Redis
docker exec -it flight-redis redis-cli ping
```

### Step 4: Start Services (In Order)

#### 4.1 Start Customer Profile Service
```bash
cd customer-profile-service-cred/customer-profile-service

# Build the service
./mvnw clean compile

# Run the service
./mvnw spring-boot:run

# Verify service is running
curl http://localhost:8081/actuator/health
```

#### 4.2 Start Travel Search Service
```bash
# Open new terminal
cd travel-search-service-cred/travel-search-service

# Build the service
./mvnw clean compile

# Run the service
./mvnw spring-boot:run

# Verify service is running
curl http://localhost:8082/actuator/health
```

#### 4.3 Start Reservation System (Core Service)
```bash
# Open new terminal
cd reservation-system-cred/reservation-system

# Build the service
./mvnw clean compile

# Run the service
./mvnw spring-boot:run

# Verify service is running
curl http://localhost:8080/actuator/health
```

## 🔧 Service Configuration

### Service Ports
| Service | Port | Health Check |
|---------|------|--------------|
| Reservation System | 8080 | http://localhost:8080/actuator/health |
| Customer Profile Service | 8081 | http://localhost:8081/actuator/health |
| Travel Search Service | 8082 | http://localhost:8082/actuator/health |

### Database Configuration

#### PostgreSQL (User Data)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flight_db
    username: postgres
    password: admin
```

#### Cassandra (Flight Data)
```yaml
spring:
  cassandra:
    keyspace-name: reservation_system
    contact-points: 127.0.0.1
    port: 9042
    local-datacenter: datacenter1
```

#### Redis (Cache)
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: ""
```

## 🐳 Docker Compose Setup

Create `docker-compose.yml` in the root directory:

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:13
    container_name: flight-postgres
    environment:
      POSTGRES_DB: flight_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: admin
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  cassandra:
    image: cassandra:4.0
    container_name: flight-cassandra
    environment:
      CASSANDRA_CLUSTER_NAME: flight-cluster
    ports:
      - "9042:9042"
    volumes:
      - cassandra_data:/var/lib/cassandra

  redis:
    image: redis:7-alpine
    container_name: flight-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  cassandra_data:
  redis_data:
```

## 📊 API Documentation

### Reservation System APIs

#### Create Multi-Stop Flight
```bash
POST http://localhost:8080/api/book-tickets/flights
Content-Type: application/json
admin-id: {admin-uuid}

{
  "flightNumber": "AI101",
  "sourceCode": "DEL",
  "destinationCode": "BLR",
  "date": "2025-01-15",
  "travelStartTime": "06:00",
  "travelEndTime": "10:30",
  "numberOfStops": 1,
  "stops": [
    {
      "airportCode": "BOM",
      "airportName": "Mumbai Airport",
      "city": "Mumbai",
      "arrivalTime": "07:30",
      "departureTime": "08:15",
      "layoverDuration": 45,
      "stopSequence": 1
    }
  ],
  "seats": [
    {
      "seatNumber": "1A",
      "seatClass": "BUSINESS",
      "amount": 15000.00
    }
  ]
}
```

#### Book Flight Segment
```bash
POST http://localhost:8080/api/book-tickets
Content-Type: application/json
user-id: {user-uuid}

{
  "userId": "{user-uuid}",
  "bookingType": "FLIGHT",
  "bookingDetails": {
    "vehicleId": "AI101",
    "sourceCode": "DEL",
    "destinationCode": "BOM",
    "seatId": "{seat-uuid}",
    "flightStartTime": "06:00",
    "flightEndTime": "07:30",
    "flightTime": "1h 30m"
  }
}
```

#### Search Flights
```bash
GET http://localhost:8080/api/book-tickets/flights/search?sourceCode=DEL&destinationCode=BLR&date=2025-01-15&directOnly=false
```

#### View Flight Segments
```bash
GET http://localhost:8080/api/book-tickets/flights/AI101/segments?date=2025-01-15
```

#### Check Cache Health
```bash
GET http://localhost:8080/api/book-tickets/cache/health
```

## 🧪 Testing

### Run Unit Tests
```bash
# Test all services
./mvnw test

# Test specific service
cd reservation-system-cred/reservation-system
./mvnw test

# Test with coverage
./mvnw test jacoco:report
```

### Run Integration Tests
```bash
# Run Redis integration tests
./mvnw test -Dtest=RedisIntegrationTest

# Run booking integration tests  
./mvnw test -Dtest=BookingIntegrationTest
```

### Load Testing
```bash
# Test Redis performance
redis-benchmark -h localhost -p 6379 -c 50 -n 10000

# Test API endpoints
curl -X POST http://localhost:8080/api/book-tickets \
  -H "Content-Type: application/json" \
  -H "user-id: $(uuidgen)" \
  -d @test-booking.json
```

## 📈 Monitoring & Metrics

### Health Checks
```bash
# Check all services
curl http://localhost:8080/actuator/health  # Reservation System
curl http://localhost:8081/actuator/health  # Customer Profile
curl http://localhost:8082/actuator/health  # Travel Search
```

### Prometheus Metrics
```bash
# Access metrics
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus  
curl http://localhost:8082/actuator/prometheus
```

### Redis Monitoring
```bash
# Connect to Redis CLI
docker exec -it flight-redis redis-cli

# Monitor operations
MONITOR

# Check memory usage
INFO memory

# View blocked seats
KEYS seat_booking:*
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Service Won't Start
```bash
# Check if port is in use
netstat -tulpn | grep :8080

# Check logs
./mvnw spring-boot:run | grep ERROR

# Verify database connections
telnet localhost 5432  # PostgreSQL
telnet localhost 9042  # Cassandra
telnet localhost 6379  # Redis
```

#### 2. Database Connection Issues
```bash
# PostgreSQL
docker exec -it flight-postgres psql -U postgres -c "SELECT version();"

# Cassandra
docker exec -it flight-cassandra cqlsh -e "SELECT cluster_name FROM system.local;"

# Redis
docker exec -it flight-redis redis-cli ping
```

#### 3. Redis Cache Issues
```bash
# Clear cache
docker exec -it flight-redis redis-cli FLUSHALL

# Check cache health
curl http://localhost:8080/api/book-tickets/cache/health

# Monitor cache operations
docker exec -it flight-redis redis-cli MONITOR
```

### Log Locations
```bash
# Application logs
tail -f logs/application.log

# Service-specific logs
./mvnw spring-boot:run > service.log 2>&1 &
tail -f service.log
```

## 🚀 Production Deployment

### Environment Variables
```bash
# Database Configuration
export DB_HOST=your-postgres-host
export DB_USERNAME=your-db-user
export DB_PASSWORD=your-db-password

# Cassandra Configuration  
export CASSANDRA_HOST=your-cassandra-host
export CASSANDRA_KEYSPACE=reservation_system

# Redis Configuration
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password
```

### Docker Build
```bash
# Build service images
docker build -t flight-reservation:latest reservation-system-cred/reservation-system/
docker build -t flight-profile:latest customer-profile-service-cred/customer-profile-service/
docker build -t flight-search:latest travel-search-service-cred/travel-search-service/

# Run with Docker Compose
docker-compose up -d
```

## 🤝 Contributing

1. **Fork the repository**
2. **Create feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit changes**: `git commit -m 'Add amazing feature'`
4. **Push to branch**: `git push origin feature/amazing-feature`
5. **Open Pull Request**

### Development Setup
```bash
# Install pre-commit hooks
pip install pre-commit
pre-commit install

# Run code formatting
./mvnw ktlint:format

# Run all tests before commit
./mvnw clean test
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Documentation
- [Multi-Stop Flight Booking Guide](docs/MULTI_STOP_BOOKING.md)
- [Redis Setup Guide](REDIS_SETUP.md)
- [API Documentation](docs/API.md)

### Contact
- **GitHub Issues**: [Create an Issue](https://github.com/RakshitKhullar/flight-system/issues)
- **Email**: your-email@example.com
- **LinkedIn**: [Your LinkedIn Profile](https://linkedin.com/in/your-profile)

---

## 🎯 Quick Commands Reference

```bash
# Start everything
docker-compose up -d && sleep 30 && \
cd customer-profile-service-cred/customer-profile-service && ./mvnw spring-boot:run &
cd ../../travel-search-service-cred/travel-search-service && ./mvnw spring-boot:run &
cd ../../reservation-system-cred/reservation-system && ./mvnw spring-boot:run &

# Health check all services
curl -s http://localhost:8080/actuator/health | jq .status
curl -s http://localhost:8081/actuator/health | jq .status  
curl -s http://localhost:8082/actuator/health | jq .status

# Stop everything
pkill -f "spring-boot:run"
docker-compose down
```

**Happy Coding! ✈️**
