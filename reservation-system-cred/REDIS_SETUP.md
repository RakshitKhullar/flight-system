# Redis Setup Guide for Flight Booking System

This guide explains how to set up and use Redis for the seat booking cache functionality in the Flight Booking System.

## Overview

The system has been upgraded to use Redis instead of in-memory caching for seat booking operations. This provides:

- **Distributed caching** across multiple application instances
- **Persistence** of blocked seats across application restarts
- **Automatic expiration** of blocked seats (TTL)
- **Better performance** and scalability
- **Atomic operations** for concurrent seat booking

## Redis Installation

### Option 1: Docker (Recommended)

```bash
# Run Redis in Docker
docker run -d \
  --name redis-flight-booking \
  -p 6379:6379 \
  redis:7-alpine \
  redis-server --appendonly yes

# Verify Redis is running
docker logs redis-flight-booking
```

### Option 2: Local Installation

#### macOS (using Homebrew)
```bash
brew install redis
brew services start redis
```

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install redis-server
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

#### Windows
Download and install from: https://redis.io/download

## Configuration

### Application Properties

The application uses the following Redis configuration in `application.yml`:

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
```

### Environment Variables

Set these environment variables for different environments:

```bash
# Development
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# Production
export REDIS_HOST=your-redis-cluster.amazonaws.com
export REDIS_PORT=6379
export REDIS_PASSWORD=your-secure-password
```

## Features

### Seat Booking Cache

The Redis-based `SeatBookingCacheService` provides:

#### Key Features:
- **Atomic seat blocking** using `SETNX` operations
- **Automatic expiration** (10 minutes TTL by default)
- **Segment-aware caching** for multi-stop flights
- **Connection health monitoring**
- **Bulk operations** for managing blocked seats

#### Key Patterns:
```
seat_booking:{flightId}:{seatId}:{flightTime} -> "BLOCKED"
```

Example keys:
```
seat_booking:AI101:550e8400-e29b-41d4-a716-446655440000:10:30
seat_booking:FL123:550e8400-e29b-41d4-a716-446655440001:DEL:BOM
```

### API Endpoints

#### Health Check
```bash
GET /api/book-tickets/cache/health
```

Response:
```json
{
  "redis_connected": true,
  "blocked_seats_count": 5,
  "status": "healthy",
  "timestamp": 1640995200000
}
```

#### Clear All Blocked Seats (Admin)
```bash
DELETE /api/book-tickets/cache/blocked-seats
Header: admin-id: {uuid}
```

Response:
```json
{
  "message": "Cleared all blocked seats",
  "seats_cleared": 5,
  "admin_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1640995200000
}
```

## Usage Examples

### Booking Flow with Redis

1. **Check seat availability**
   ```kotlin
   val isBlocked = seatBookingCacheService.isSeatBookingInProgress(seatKey)
   ```

2. **Block seat atomically**
   ```kotlin
   val blocked = seatBookingCacheService.blockSeatForBooking(seatKey)
   if (blocked) {
       // Proceed with booking
   } else {
       // Seat already blocked by another user
   }
   ```

3. **Complete booking and release**
   ```kotlin
   try {
       // Process payment and create ticket
       val ticket = processBooking(bookingRequest)
       return ticket
   } finally {
       // Always release the seat from cache
       seatBookingCacheService.releaseSeatBooking(seatKey)
   }
   ```

### Multi-Stop Flight Segments

For multi-stop flights, the system creates segment-specific cache keys:

```kotlin
// Flight: DEL -> BOM -> BLR
// User books DEL -> BOM segment
val segmentKey = "AI101:${seatId}:DEL:BOM"
seatBookingCacheService.blockSeatForBooking(segmentKey)

// This blocks the seat for overlapping segments:
// - DEL -> BOM (direct)
// - DEL -> BLR (full route)
```

## Monitoring and Troubleshooting

### Redis CLI Commands

```bash
# Connect to Redis
redis-cli

# Check all seat booking keys
KEYS seat_booking:*

# Get specific seat status
GET seat_booking:AI101:550e8400-e29b-41d4-a716-446655440000:10:30

# Check TTL for a key
TTL seat_booking:AI101:550e8400-e29b-41d4-a716-446655440000:10:30

# Clear all seat booking keys
DEL seat_booking:*

# Monitor Redis operations in real-time
MONITOR
```

### Health Monitoring

1. **Application Health Check**
   ```bash
   curl http://localhost:8080/api/book-tickets/cache/health
   ```

2. **Redis Server Info**
   ```bash
   redis-cli INFO server
   redis-cli INFO memory
   redis-cli INFO clients
   ```

3. **Check Connection**
   ```bash
   redis-cli PING
   # Should return: PONG
   ```

## Performance Tuning

### Redis Configuration

For production, consider these Redis settings:

```conf
# redis.conf
maxmemory 2gb
maxmemory-policy allkeys-lru
timeout 300
tcp-keepalive 60
save 900 1
save 300 10
save 60 10000
```

### Connection Pool Settings

Adjust based on your load:

```yaml
spring:
  redis:
    jedis:
      pool:
        max-active: 20    # Max connections
        max-idle: 10      # Max idle connections
        min-idle: 2       # Min idle connections
        max-wait: 5000ms  # Max wait for connection
```

## Testing

### Unit Tests
```bash
./mvnw test -Dtest=SeatBookingCacheServiceTest
```

### Integration Tests
```bash
./mvnw test -Dtest=RedisIntegrationTest
```

### Load Testing
```bash
# Use Redis benchmark tool
redis-benchmark -h localhost -p 6379 -c 50 -n 10000 -d 3
```

## Security Considerations

### Production Setup

1. **Enable Authentication**
   ```conf
   requirepass your-secure-password
   ```

2. **Network Security**
   - Use Redis in private network
   - Enable TLS/SSL for Redis connections
   - Configure firewall rules

3. **Access Control**
   ```conf
   # Redis 6+ ACL
   user booking-app on >password ~seat_booking:* +@all
   ```

### Environment Variables
```bash
export REDIS_PASSWORD=your-secure-password
export REDIS_TLS_ENABLED=true
export REDIS_SSL_CERT_PATH=/path/to/cert.pem
```

## Troubleshooting

### Common Issues

1. **Connection Refused**
   ```
   Solution: Check if Redis server is running
   redis-cli ping
   ```

2. **Memory Issues**
   ```
   Solution: Monitor Redis memory usage
   redis-cli INFO memory
   ```

3. **Slow Performance**
   ```
   Solution: Check Redis slow log
   redis-cli SLOWLOG GET 10
   ```

4. **Keys Not Expiring**
   ```
   Solution: Check TTL settings
   redis-cli TTL seat_booking:your-key
   ```

### Logs Analysis

Check application logs for Redis-related issues:
```bash
grep -i redis application.log
grep -i "seat booking" application.log
```

## Migration from In-Memory Cache

The Redis implementation maintains the same interface as the previous in-memory cache, so no code changes are required in the booking logic. The migration provides:

- ✅ **Same API interface** - no breaking changes
- ✅ **Enhanced reliability** - distributed caching
- ✅ **Better performance** - Redis optimizations
- ✅ **Automatic cleanup** - TTL-based expiration
- ✅ **Monitoring capabilities** - health checks and metrics

## Support

For issues related to Redis setup or configuration, check:

1. **Application logs** - `/logs/application.log`
2. **Redis logs** - Redis server logs
3. **Health endpoint** - `/api/book-tickets/cache/health`
4. **Metrics endpoint** - `/actuator/metrics`

---

**Note**: This Redis setup supports the multi-stop flight segment booking system with proper isolation and conflict resolution for overlapping flight segments.
