#!/bin/bash

# Docker Build Script for Flight Search Service

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

# Function to build production image
build_production() {
    print_status "Building production Docker image..."
    
    docker build -t flight-search-service:latest .
    docker build -t flight-search-service:prod .
    
    print_success "Production image built successfully"
}

# Function to build development image
build_development() {
    print_status "Building development Docker image..."
    
    docker build -f Dockerfile.dev -t flight-search-service:dev .
    
    print_success "Development image built successfully"
}

# Function to build test image
build_test() {
    print_status "Building test Docker image..."
    
    docker build -f Dockerfile.test -t flight-search-service:test .
    
    print_success "Test image built successfully"
}

# Function to build all images
build_all() {
    print_status "Building all Docker images..."
    
    build_production
    build_development
    build_test
    
    print_success "All images built successfully"
}

# Function to run tests in container
run_tests() {
    print_status "Running tests in Docker container..."
    
    # Build test image if it doesn't exist
    if ! docker image inspect flight-search-service:test >/dev/null 2>&1; then
        build_test
    fi
    
    # Start test environment
    docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit test-runner
    
    print_success "Tests completed"
}

# Function to run integration tests
run_integration_tests() {
    print_status "Running integration tests in Docker container..."
    
    # Build test image if it doesn't exist
    if ! docker image inspect flight-search-service:test >/dev/null 2>&1; then
        build_test
    fi
    
    # Start integration test environment
    docker-compose -f docker-compose.test.yml --profile integration up --build --abort-on-container-exit integration-test-runner
    
    print_success "Integration tests completed"
}

# Function to push images to registry
push_images() {
    local registry=${1:-""}
    
    if [ -z "$registry" ]; then
        print_error "Registry URL is required for push operation"
        exit 1
    fi
    
    print_status "Pushing images to registry: $registry"
    
    # Tag images for registry
    docker tag flight-search-service:latest $registry/flight-search-service:latest
    docker tag flight-search-service:prod $registry/flight-search-service:prod
    docker tag flight-search-service:dev $registry/flight-search-service:dev
    
    # Push images
    docker push $registry/flight-search-service:latest
    docker push $registry/flight-search-service:prod
    docker push $registry/flight-search-service:dev
    
    print_success "Images pushed successfully"
}

# Function to clean up images
cleanup() {
    print_status "Cleaning up Docker images and containers..."
    
    # Remove containers
    docker-compose -f docker-compose.yml down --remove-orphans
    docker-compose -f docker-compose.test.yml down --remove-orphans
    
    # Remove images
    docker rmi flight-search-service:latest flight-search-service:prod flight-search-service:dev flight-search-service:test 2>/dev/null || true
    
    # Clean up dangling images
    docker image prune -f
    
    print_success "Cleanup completed"
}

# Function to show image information
show_images() {
    print_status "Docker images for Flight Search Service:"
    docker images | grep flight-search-service || echo "No flight-search-service images found"
}

# Function to show help
show_help() {
    echo "Flight Search Service Docker Build Script"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  prod                 Build production image"
    echo "  dev                  Build development image"
    echo "  test                 Build test image"
    echo "  all                  Build all images"
    echo "  run-tests            Run tests in container"
    echo "  run-integration      Run integration tests in container"
    echo "  push [REGISTRY]      Push images to registry"
    echo "  cleanup              Remove all images and containers"
    echo "  images               Show built images"
    echo "  help                 Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 all               # Build all images"
    echo "  $0 run-tests         # Run tests in container"
    echo "  $0 push registry.com # Push to registry"
    echo "  $0 cleanup           # Clean up everything"
}

# Main script logic
main() {
    case "${1:-help}" in
        "prod"|"production")
            build_production
            ;;
        "dev"|"development")
            build_development
            ;;
        "test")
            build_test
            ;;
        "all")
            build_all
            ;;
        "run-tests")
            run_tests
            ;;
        "run-integration")
            run_integration_tests
            ;;
        "push")
            push_images "$2"
            ;;
        "cleanup")
            cleanup
            ;;
        "images")
            show_images
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# Run main function with all arguments
main "$@"
