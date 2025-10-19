# Reservation System - Docker Setup

This Docker Compose setup provides PostgreSQL and Cassandra databases for the reservation system.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose (included with Docker Desktop)

## Quick Start

### 1. Start Databases
```bash
./start-databases.sh
```

### 2. Stop Databases
```bash
./stop-databases.sh
```

### 3. Manual Commands
```bash
# Start databases
docker-compose up -d

# Stop databases
docker-compose down

# View logs
docker-compose logs -f

# Remove everything including data
docker-compose down -v
```

## Database Connections

### PostgreSQL
- **Host:** localhost
- **Port:** 5432
- **Database:** reservation_db
- **Username:** reservation_user
- **Password:** reservation_pass

### Cassandra
- **Host:** localhost
- **Port:** 9042
- **Cluster:** ReservationCluster
- **Datacenter:** datacenter1

## Application Configuration

Update your `application.yml` to use these database connections:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/reservation_db
    username: reservation_user
    password: reservation_pass
    
  data:
    cassandra:
      contact-points: localhost
      port: 9042
      keyspace-name: reservation_keyspace
      local-datacenter: datacenter1
```

## Useful Commands

### PostgreSQL
```bash
# Connect to PostgreSQL
docker exec -it reservation-postgres psql -U reservation_user -d reservation_db

# Run SQL commands
docker exec -it reservation-postgres psql -U reservation_user -d reservation_db -c "SELECT version();"
```

### Cassandra
```bash
# Connect to Cassandra
docker exec -it reservation-cassandra cqlsh

# Create keyspace
docker exec -it reservation-cassandra cqlsh -e "CREATE KEYSPACE IF NOT EXISTS reservation_keyspace WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};"
```

## Data Persistence

- PostgreSQL data is stored in Docker volume: `postgres_data`
- Cassandra data is stored in Docker volume: `cassandra_data`
- Data persists between container restarts
- Use `docker-compose down -v` to remove all data

## Health Checks

Both databases include health checks:
- PostgreSQL: Checks if database accepts connections
- Cassandra: Checks if CQL interface is responsive

## Troubleshooting

### PostgreSQL Issues
```bash
# Check PostgreSQL logs
docker logs reservation-postgres

# Check if PostgreSQL is ready
docker exec reservation-postgres pg_isready -U reservation_user -d reservation_db
```

### Cassandra Issues
```bash
# Check Cassandra logs
docker logs reservation-cassandra

# Check Cassandra status
docker exec reservation-cassandra nodetool status
```

### Port Conflicts
If ports 5432 or 9042 are already in use, modify the `docker-compose.yml` file:
```yaml
ports:
  - "15432:5432"  # Use port 15432 instead of 5432
  - "19042:9042"  # Use port 19042 instead of 9042
```
