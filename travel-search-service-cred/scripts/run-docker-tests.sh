#!/bin/bash

# Docker Test Runner Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Function to run unit tests
run_unit_tests() {
    print_status "Running unit tests in Docker..."
    
    # Start test dependencies
    docker-compose -f docker-compose.test.yml up -d postgres-test redis-test
    
    # Wait for services to be ready
    print_status "Waiting for test services to be ready..."
    sleep 15
    
    # Run tests
    docker-compose -f docker-compose.test.yml up --build test-runner
    
    # Get exit code
    local exit_code=$?
    
    # Cleanup
    docker-compose -f docker-compose.test.yml down
    
    if [ $exit_code -eq 0 ]; then
        print_success "Unit tests passed!"
    else
        print_error "Unit tests failed!"
        exit $exit_code
    fi
}

# Function to run integration tests
run_integration_tests() {
    print_status "Running integration tests in Docker..."
    
    # Start test dependencies
    docker-compose -f docker-compose.test.yml up -d postgres-test redis-test
    
    # Wait for services to be ready
    print_status "Waiting for test services to be ready..."
    sleep 15
    
    # Run integration tests
    docker-compose -f docker-compose.test.yml --profile integration up --build integration-test-runner
    
    # Get exit code
    local exit_code=$?
    
    # Cleanup
    docker-compose -f docker-compose.test.yml down
    
    if [ $exit_code -eq 0 ]; then
        print_success "Integration tests passed!"
    else
        print_error "Integration tests failed!"
        exit $exit_code
    fi
}

# Function to run all tests
run_all_tests() {
    print_status "Running all tests in Docker..."
    
    run_unit_tests
    run_integration_tests
    
    print_success "All tests completed successfully!"
}

# Function to run tests with coverage
run_tests_with_coverage() {
    print_status "Running tests with coverage in Docker..."
    
    # Create coverage directory
    mkdir -p target/site/jacoco
    
    # Start test dependencies
    docker-compose -f docker-compose.test.yml up -d postgres-test redis-test
    
    # Wait for services to be ready
    sleep 15
    
    # Run tests with coverage
    docker-compose -f docker-compose.test.yml run --rm test-runner \
        ./mvnw test jacoco:report -B
    
    # Cleanup
    docker-compose -f docker-compose.test.yml down
    
    print_success "Tests with coverage completed!"
    print_status "Coverage report available at: target/site/jacoco/index.html"
}

# Function to show test results
show_test_results() {
    print_status "Test Results Summary:"
    
    if [ -d "target/surefire-reports" ]; then
        echo "Unit Test Reports:"
        find target/surefire-reports -name "*.xml" | wc -l | xargs echo "  Test files:"
        
        if [ -f "target/surefire-reports/TEST-*.xml" ]; then
            grep -h "tests=" target/surefire-reports/TEST-*.xml | head -1
        fi
    fi
    
    if [ -d "target/site/jacoco" ]; then
        echo "Coverage Report: target/site/jacoco/index.html"
    fi
}

# Function to clean test artifacts
clean_test_artifacts() {
    print_status "Cleaning test artifacts..."
    
    # Stop and remove test containers
    docker-compose -f docker-compose.test.yml down -v --remove-orphans
    
    # Remove test images
    docker rmi flight-search-service:test 2>/dev/null || true
    
    # Clean local test artifacts
    rm -rf target/surefire-reports
    rm -rf target/site/jacoco
    rm -rf test-results
    
    print_success "Test artifacts cleaned!"
}

# Function to show help
show_help() {
    echo "Docker Test Runner for Flight Search Service"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  unit                 Run unit tests only"
    echo "  integration          Run integration tests only"
    echo "  all                  Run all tests"
    echo "  coverage             Run tests with coverage report"
    echo "  results              Show test results summary"
    echo "  clean                Clean test artifacts"
    echo "  help                 Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 unit              # Run unit tests"
    echo "  $0 all               # Run all tests"
    echo "  $0 coverage          # Run with coverage"
}

# Main script logic
main() {
    case "${1:-help}" in
        "unit")
            run_unit_tests
            ;;
        "integration")
            run_integration_tests
            ;;
        "all")
            run_all_tests
            ;;
        "coverage")
            run_tests_with_coverage
            ;;
        "results")
            show_test_results
            ;;
        "clean")
            clean_test_artifacts
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# Run main function
main "$@"
