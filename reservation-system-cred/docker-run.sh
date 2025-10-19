#!/bin/bash

# Flight Booking System - Docker Run Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install it and try again."
    exit 1
fi

print_status "🚀 Starting Flight Booking System..."

# Create necessary directories
print_status "Creating monitoring directories..."
mkdir -p monitoring/grafana/dashboards
mkdir -p monitoring/grafana/datasources
mkdir -p init-scripts

# Stop any existing containers
print_status "Stopping existing containers..."
docker-compose down --remove-orphans

# Start infrastructure services first
print_status "Starting infrastructure services (PostgreSQL, Redis, Cassandra)..."
docker-compose up -d postgres redis cassandra

# Wait for databases to be ready
print_status "Waiting for databases to be ready..."
sleep 30

# Check database health
print_status "Checking database connectivity..."
for i in {1..30}; do
    if docker-compose exec -T postgres pg_isready -U reservation_user -d reservation_db > /dev/null 2>&1; then
        print_success "PostgreSQL is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "PostgreSQL failed to start within timeout"
        exit 1
    fi
    sleep 2
done

for i in {1..30}; do
    if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
        print_success "Redis is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "Redis failed to start within timeout"
        exit 1
    fi
    sleep 2
done

# Start monitoring services
print_status "Starting monitoring services (Prometheus, Grafana)..."
docker-compose up -d prometheus grafana elasticsearch

# Build and start the application
print_status "Building and starting Reservation System..."
docker-compose up -d --build reservation-system

# Wait for application to be ready
print_status "Waiting for application to be ready..."
for i in {1..60}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        print_success "Reservation System is ready!"
        break
    fi
    if [ $i -eq 60 ]; then
        print_error "Application failed to start within timeout"
        docker-compose logs reservation-system
        exit 1
    fi
    sleep 5
done

print_success "🎉 Flight Booking System is now running!"
echo ""
print_status "Service URLs:"
echo "  📱 Reservation System API: http://localhost:8080"
echo "  📊 Health Check: http://localhost:8080/actuator/health"
echo "  📈 Metrics: http://localhost:8080/actuator/prometheus"
echo "  🔍 Prometheus: http://localhost:9090"
echo "  📊 Grafana: http://localhost:3000 (admin/admin)"
echo "  🔎 Elasticsearch: http://localhost:9200"
echo ""
print_status "Database Connections:"
echo "  🐘 PostgreSQL: localhost:5432 (reservation_user/reservation_pass)"
echo "  🔴 Redis: localhost:6379"
echo "  🏛️ Cassandra: localhost:9042"
echo ""
print_status "To view logs: docker-compose logs -f reservation-system"
print_status "To stop all services: docker-compose down"
