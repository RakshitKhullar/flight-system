# Makefile for Flight Search Service

.PHONY: help build build-dev build-test build-all run run-dev run-prod test integration-test clean setup

# Default target
help:
	@echo "Flight Search Service - Available Commands:"
	@echo ""
	@echo "Build Commands:"
	@echo "  build          Build production Docker image"
	@echo "  build-dev      Build development Docker image"
	@echo "  build-test     Build test Docker image"
	@echo "  build-all      Build all Docker images"
	@echo ""
	@echo "Run Commands:"
	@echo "  run            Start development environment"
	@echo "  run-dev        Start development with hot reload"
	@echo "  run-prod       Start production environment"
	@echo "  run-local      Start local development environment"
	@echo ""
	@echo "Test Commands:"
	@echo "  test           Run unit tests"
	@echo "  test-docker    Run tests in Docker container"
	@echo "  integration    Run integration tests"
	@echo ""
	@echo "Utility Commands:"
	@echo "  setup          Initial project setup"
	@echo "  clean          Clean up containers and images"
	@echo "  logs           Show application logs"
	@echo "  shell          Open shell in running container"

# Build commands
build:
	@echo "Building production Docker image..."
	docker build -t flight-search-service:latest .

build-dev:
	@echo "Building development Docker image..."
	docker build -f Dockerfile.dev -t flight-search-service:dev .

build-test:
	@echo "Building test Docker image..."
	docker build -f Dockerfile.test -t flight-search-service:test .

build-all: build build-dev build-test
	@echo "All images built successfully!"

# Run commands
run:
	@echo "Starting development environment..."
	docker-compose up -d postgres redis
	@echo "Services started. Run your application locally."

run-dev:
	@echo "Starting development environment with application..."
	docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d

run-prod:
	@echo "Starting production environment..."
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

run-local:
	@echo "Starting local development environment..."
	docker-compose -f docker-compose.local.yml up -d

# Test commands
test:
	@echo "Running unit tests..."
	./mvnw test

test-docker:
	@echo "Running tests in Docker container..."
	docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit test-runner

integration:
	@echo "Running integration tests..."
	docker-compose -f docker-compose.test.yml --profile integration up --build --abort-on-container-exit integration-test-runner

# Utility commands
setup:
	@echo "Setting up project..."
	chmod +x scripts/*.sh
	cp .env.example .env
	mkdir -p logs data/postgres data/redis test-results
	@echo "Setup completed! Please review .env file."

clean:
	@echo "Cleaning up containers and images..."
	docker-compose down -v --remove-orphans
	docker-compose -f docker-compose.test.yml down -v --remove-orphans
	docker-compose -f docker-compose.local.yml down -v --remove-orphans
	docker system prune -f

logs:
	@echo "Showing application logs..."
	docker-compose logs -f travel-search-service

shell:
	@echo "Opening shell in application container..."
	docker-compose exec travel-search-service /bin/bash

# Stop commands
stop:
	@echo "Stopping all services..."
	docker-compose down

stop-local:
	@echo "Stopping local services..."
	docker-compose -f docker-compose.local.yml down

# Status commands
status:
	@echo "Service status:"
	docker-compose ps

# Database commands
db-shell:
	@echo "Opening database shell..."
	docker-compose exec postgres psql -U flight_user -d flight_search_db

redis-cli:
	@echo "Opening Redis CLI..."
	docker-compose exec redis redis-cli
