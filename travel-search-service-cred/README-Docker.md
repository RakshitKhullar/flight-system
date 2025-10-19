# Flight Search Service - Docker Setup

This document provides comprehensive instructions for running the Flight Search Service using Docker and Docker Compose.

## 📋 Prerequisites

- Docker 20.10+ 
- Docker Compose 2.0+
- 4GB+ RAM available
- 10GB+ disk space

## 🚀 Quick Start

### 1. Initial Setup
```bash
# Make setup script executable
chmod +x scripts/docker-setup.sh

# Run initial setup
./scripts/docker-setup.sh setup
```

### 2. Start Development Environment
```bash
# Start development services (PostgreSQL + Redis)
./scripts/docker-setup.sh start dev

# Or use docker-compose directly
docker-compose up -d postgres redis
```

### 3. Start Full Application
```bash
# Start all services
./scripts/docker-setup.sh start

# Or use docker-compose directly
docker-compose up -d
```

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Nginx       │    │  Travel Search  │    │   PostgreSQL    │
│  Load Balancer  │────│    Service      │────│    Database     │
│   (Port 80)     │    │  (Port 8080)    │    │   (Port 5432)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                       ┌─────────────────┐
                       │      Redis      │
                       │     Cache       │
                       │   (Port 6379)   │
                       └─────────────────┘
```

## 📁 Directory Structure

```
travel-search-service-cred/
├── docker-compose.yml              # Main compose file
├── docker-compose.override.yml     # Development overrides
├── docker-compose.prod.yml         # Production configuration
├── .env.example                    # Environment variables template
├── .dockerignore                   # Docker ignore file
├── Dockerfile                      # Application container
├── redis.conf                      # Redis configuration
├── nginx/
│   └── nginx.conf                  # Nginx configuration
├── monitoring/
│   ├── prometheus.yml              # Prometheus configuration
│   └── grafana/                    # Grafana dashboards
├── scripts/
│   └── docker-setup.sh             # Setup automation script
└── logs/                           # Application logs
```

## 🔧 Configuration Files

### Environment Variables (.env)

Copy `.env.example` to `.env` and customize:

```bash
# Database
POSTGRES_DB=flight_search_db
POSTGRES_USER=flight_user
POSTGRES_PASSWORD=your_secure_password

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Application
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=8080
```

### Docker Compose Profiles

- **Default**: Basic services (app, db, redis)
- **monitoring**: Adds Prometheus + Grafana
- **search**: Adds Elasticsearch + Kibana
- **production**: Production optimizations

## 🚀 Deployment Options

### Development Environment
```bash
# Start only dependencies
docker-compose up -d postgres redis

# Run application locally with IDE
# Application connects to containerized DB and Redis
```

### Staging Environment
```bash
# Start with monitoring
docker-compose --profile monitoring up -d
```

### Production Environment
```bash
# Use production configuration
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## 📊 Monitoring & Observability

### Access Points
- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8081/actuator/prometheus
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090

### Key Metrics
- HTTP request rates and latencies
- Database connection pool status
- Redis cache hit rates
- JVM memory and GC metrics
- Custom business metrics

## 🔍 API Endpoints

### Flight Search
```bash
# Search flights
curl "http://localhost:8080/api/v1/search/flights?source=Delhi&destination=Mumbai&flightDate=2024-01-15"

# Add flight data
curl -X POST http://localhost:8080/api/v1/elastic-search/flight-data \
  -H "Content-Type: application/json" \
  -d '{
    "flightId": "AI101",
    "source": "Delhi",
    "destination": "Mumbai",
    "flightDate": "2024-01-15",
    "maximumStops": 0,
    "departner": "Air India"
  }'
```

### Cache Management
```bash
# Cache info
curl http://localhost:8080/api/v1/cache/info

# Clear cache
curl -X DELETE http://localhost:8080/api/v1/cache/all
```

## 🛠️ Troubleshooting

### Common Issues

#### 1. Port Conflicts
```bash
# Check port usage
netstat -tulpn | grep :8080

# Use different ports in .env file
SERVER_PORT=8081
```

#### 2. Memory Issues
```bash
# Check container memory usage
docker stats

# Adjust memory limits in docker-compose.yml
```

#### 3. Database Connection Issues
```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Verify database is ready
docker-compose exec postgres pg_isready -U flight_user
```

#### 4. Redis Connection Issues
```bash
# Check Redis logs
docker-compose logs redis

# Test Redis connection
docker-compose exec redis redis-cli ping
```

### Debugging Commands

```bash
# View all service logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f travel-search-service

# Execute commands in containers
docker-compose exec postgres psql -U flight_user -d flight_search_db
docker-compose exec redis redis-cli

# Check service health
docker-compose ps
```

## 🔒 Security Considerations

### Production Security
- Change default passwords
- Enable SSL/TLS for Nginx
- Configure firewall rules
- Use secrets management
- Enable audit logging

### Network Security
```yaml
# Custom network configuration
networks:
  flight-search-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

## 📈 Performance Tuning

### Database Optimization
```yaml
postgres:
  command: >
    postgres
    -c max_connections=200
    -c shared_buffers=256MB
    -c effective_cache_size=1GB
```

### Application Optimization
```yaml
travel-search-service:
  environment:
    JAVA_OPTS: "-Xmx1g -Xms512m -XX:+UseG1GC"
```

### Redis Optimization
```yaml
redis:
  command: >
    redis-server
    --maxmemory 512mb
    --maxmemory-policy allkeys-lru
```

## 🔄 Backup & Recovery

### Database Backup
```bash
# Create backup
docker-compose exec postgres pg_dump -U flight_user flight_search_db > backup.sql

# Restore backup
docker-compose exec -T postgres psql -U flight_user flight_search_db < backup.sql
```

### Redis Backup
```bash
# Create Redis snapshot
docker-compose exec redis redis-cli BGSAVE
```

## 📝 Maintenance

### Regular Tasks
```bash
# Update images
docker-compose pull

# Restart services
./scripts/docker-setup.sh restart

# Clean up unused resources
docker system prune -f

# View resource usage
docker system df
```

### Log Rotation
```bash
# Configure log rotation in docker-compose.yml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

## 🆘 Support

For issues and questions:
1. Check the troubleshooting section
2. Review container logs
3. Verify configuration files
4. Check system resources

## 📚 Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [Redis Docker Hub](https://hub.docker.com/_/redis)
