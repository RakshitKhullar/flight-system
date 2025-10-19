# Flight Booking System - Complete Docker Guide

This comprehensive guide will help you run the Flight Booking System using Docker and Docker Compose with all dependencies.

## 🏗️ Architecture Overview

The system runs as a complete microservices stack:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Reservation   │    │   PostgreSQL    │    │     Redis       │
│     System      │────│    Database     │    │     Cache       │
│   (Port 8080)   │    │   (Port 5432)   │    │   (Port 6379)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │
         │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Cassandra     │    │   Prometheus    │    │    Grafana      │
│   Database      │    │   Monitoring    │    │  Visualization  │
│  (Port 9042)    │    │   (Port 9090)   │    │   (Port 3000)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │
┌─────────────────┐
│  Elasticsearch  │
│  Search Engine  │
│  (Port 9200)    │
└─────────────────┘
```

## 📋 Prerequisites

- **Docker Desktop** (latest version)
- **Docker Compose** (included with Docker Desktop)
- **At least 6GB RAM** available for containers
- **At least 15GB disk space** for images and volumes
- **Ports available**: 8080, 5432, 6379, 9042, 9090, 3000, 9200

## 🚀 Quick Start

### 1. Clone and Setup
```bash
git clone <repository-url>
cd reservation-system-cred
```

### 2. Configure Environment
```bash
cp .env.example .env
# Edit .env file if needed
```

### 3. One-Command Start
```bash
chmod +x docker-run.sh
./docker-run.sh
```

This script will:
- ✅ Start all infrastructure services
- ✅ Wait for databases to be ready
- ✅ Build and deploy the application
- ✅ Verify system health
- ✅ Display service URLs

## 🛠️ Manual Setup

### Build Application Image
```bash
chmod +x docker-build.sh
./docker-build.sh [version-tag]
```

### Start Services Step by Step
```bash
# 1. Start databases first
docker-compose up -d postgres redis cassandra

# 2. Wait for databases (30-60 seconds)
docker-compose logs postgres

# 3. Start monitoring
docker-compose up -d prometheus grafana elasticsearch

# 4. Start application
docker-compose up -d --build reservation-system
```

## 🌐 Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Reservation API** | http://localhost:8080 | - |
| **Health Check** | http://localhost:8080/actuator/health | - |
| **API Metrics** | http://localhost:8080/actuator/prometheus | - |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | admin/admin |
| **Elasticsearch** | http://localhost:9200 | - |

### Database Connections
| Database | Connection | Credentials |
|----------|------------|-------------|
| **PostgreSQL** | localhost:5432 | reservation_user/reservation_pass |
| **Redis** | localhost:6379 | No password |
| **Cassandra** | localhost:9042 | No auth |

## 🔍 Health Monitoring

### Application Health
```bash
# Overall health
curl http://localhost:8080/actuator/health

# Detailed health with components
curl http://localhost:8080/actuator/health | jq '.'
```

### Database Health
```bash
# PostgreSQL
docker-compose exec postgres pg_isready -U reservation_user -d reservation_db

# Redis
docker-compose exec redis redis-cli ping

# Cassandra
docker-compose exec cassandra cqlsh -e "describe cluster"

# Elasticsearch
curl http://localhost:9200/_cluster/health
```

### Service Status
```bash
# Check all containers
docker-compose ps

# Check specific service logs
docker-compose logs -f reservation-system
```

## 📊 Monitoring & Observability

### Prometheus Metrics
- **URL**: http://localhost:9090
- **Application Metrics**: http://localhost:8080/actuator/prometheus
- **Custom Metrics**: Booking rates, cache hits, database connections

### Grafana Dashboards
- **URL**: http://localhost:3000
- **Login**: admin/admin
- **Pre-configured**: Prometheus datasource
- **Dashboards**: Create custom dashboards for your metrics

### Application Logs
```bash
# Real-time logs
docker-compose logs -f reservation-system

# Structured logs with timestamps
docker-compose logs --timestamps reservation-system

# Filter logs by level
docker-compose logs reservation-system | grep ERROR
```

## 🔧 Configuration

### Environment Variables (.env)
```bash
# Database
DB_USERNAME=reservation_user
DB_PASSWORD=reservation_pass

# Redis
REDIS_HOST=redis
REDIS_PASSWORD=

# Application
SPRING_PROFILES_ACTIVE=docker
LOG_LEVEL=INFO
```

### Application Profiles
- **Default**: `application.yml`
- **Docker**: `application-docker.yml` (optimized for containers)
- **Custom**: Override via environment variables

## 🐛 Troubleshooting

### Common Issues

#### 1. Port Conflicts
```bash
# Check port usage
lsof -i :8080
lsof -i :5432

# Kill conflicting processes
sudo kill -9 $(lsof -t -i:8080)
```

#### 2. Memory Issues
```bash
# Check Docker memory allocation
docker system df
docker stats

# Increase Docker Desktop memory to 6GB+
# Docker Desktop → Settings → Resources → Memory
```

#### 3. Database Connection Failures
```bash
# Check database startup order
docker-compose logs postgres
docker-compose logs cassandra

# Restart with proper dependencies
docker-compose down
docker-compose up -d postgres redis cassandra
# Wait 30 seconds
docker-compose up -d reservation-system
```

#### 4. Application Startup Issues
```bash
# Check application logs
docker-compose logs reservation-system

# Common issues:
# - Database not ready: Wait longer for DB startup
# - Memory issues: Increase Docker memory
# - Port conflicts: Check port availability
```

### Debug Commands
```bash
# Container shell access
docker-compose exec reservation-system bash
docker-compose exec postgres psql -U reservation_user -d reservation_db

# Network connectivity
docker-compose exec reservation-system ping postgres
docker-compose exec reservation-system ping redis

# Resource usage
docker stats
```

## 🧹 Cleanup

### Stop Services
```bash
# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ DATA LOSS)
docker-compose down -v

# Remove images
docker rmi reservation-system:latest
```

### Complete Cleanup
```bash
# Remove everything including networks
docker-compose down -v --remove-orphans
docker system prune -a
```

## 🚀 Development Workflow

### Code Changes
```bash
# Rebuild and restart application
docker-compose up -d --build reservation-system

# Or use build script
./docker-build.sh
docker-compose up -d reservation-system
```

### Database Changes
```bash
# Reset databases (⚠️ DATA LOSS)
docker-compose down -v
docker-compose up -d postgres redis cassandra
# Wait for startup, then start application
```

### Testing
```bash
# Run tests in container
docker-compose exec reservation-system ./mvnw test

# Or build with tests
docker build --target builder -t reservation-system:test .
docker run --rm reservation-system:test mvn test
```

## 🏭 Production Considerations

### Security
- [ ] Change all default passwords
- [ ] Enable Redis authentication
- [ ] Configure Cassandra authentication
- [ ] Set up HTTPS/TLS
- [ ] Use Docker secrets for sensitive data

### Performance
- [ ] Increase JVM heap size
- [ ] Configure database connection pools
- [ ] Set up Redis clustering
- [ ] Configure Cassandra cluster

### High Availability
- [ ] Use external managed databases
- [ ] Set up load balancer
- [ ] Configure health checks
- [ ] Implement backup strategies

### Monitoring
- [ ] Set up alerting rules
- [ ] Configure log aggregation
- [ ] Set up distributed tracing
- [ ] Monitor business metrics

## 📞 Support

For issues and questions:

1. **Check logs first**: `docker-compose logs reservation-system`
2. **Review troubleshooting section** above
3. **Check system resources**: `docker stats`
4. **Verify network connectivity**: `docker network ls`
5. **Create issue** in the repository with:
   - Error logs
   - System information
   - Steps to reproduce

## 🎯 Next Steps

After successful deployment:

1. **API Testing**: Use the health check endpoints
2. **Load Testing**: Test with concurrent requests
3. **Monitoring Setup**: Configure Grafana dashboards
4. **Security Hardening**: Implement authentication
5. **Performance Tuning**: Optimize based on metrics
