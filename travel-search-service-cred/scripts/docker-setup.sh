#!/bin/bash

# Flight Search Service Docker Setup Script

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

# Function to check if Docker is installed and running
check_docker() {
    print_status "Checking Docker installation..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    print_success "Docker is installed and running"
}

# Function to check if Docker Compose is installed
check_docker_compose() {
    print_status "Checking Docker Compose installation..."
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    print_success "Docker Compose is available"
}

# Function to create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    
    mkdir -p logs
    mkdir -p data/postgres
    mkdir -p data/redis
    mkdir -p nginx/ssl
    
    print_success "Directories created"
}

# Function to copy environment file
setup_environment() {
    print_status "Setting up environment variables..."
    
    if [ ! -f .env ]; then
        if [ -f .env.example ]; then
            cp .env.example .env
            print_warning "Created .env file from .env.example. Please review and update the values."
        else
            print_warning ".env.example not found. Please create .env file manually."
        fi
    else
        print_status ".env file already exists"
    fi
}

# Function to build the application
build_application() {
    print_status "Building the application..."
    
    cd travel-search-service-cred
    
    if [ -f mvnw ]; then
        ./mvnw clean package -DskipTests
    elif command -v mvn &> /dev/null; then
        mvn clean package -DskipTests
    else
        print_error "Maven is not available. Please install Maven or use the Maven wrapper."
        exit 1
    fi
    
    cd ..
    print_success "Application built successfully"
}

# Function to start services
start_services() {
    local environment=${1:-"dev"}
    
    print_status "Starting services in $environment mode..."
    
    case $environment in
        "dev"|"development")
            docker-compose up -d postgres redis
            print_status "Started development services (postgres, redis)"
            ;;
        "prod"|"production")
            docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
            print_status "Started production services"
            ;;
        "monitoring")
            docker-compose --profile monitoring up -d
            print_status "Started services with monitoring"
            ;;
        "all")
            docker-compose --profile monitoring --profile search up -d
            print_status "Started all services"
            ;;
        *)
            docker-compose up -d
            print_status "Started default services"
            ;;
    esac
    
    print_success "Services started successfully"
}

# Function to stop services
stop_services() {
    print_status "Stopping all services..."
    docker-compose down
    print_success "Services stopped"
}

# Function to show service status
show_status() {
    print_status "Service status:"
    docker-compose ps
}

# Function to show logs
show_logs() {
    local service=${1:-""}
    
    if [ -z "$service" ]; then
        docker-compose logs -f
    else
        docker-compose logs -f "$service"
    fi
}

# Function to clean up
cleanup() {
    print_status "Cleaning up..."
    
    print_warning "This will remove all containers, volumes, and networks. Are you sure? (y/N)"
    read -r response
    
    if [[ "$response" =~ ^[Yy]$ ]]; then
        docker-compose down -v --remove-orphans
        docker system prune -f
        print_success "Cleanup completed"
    else
        print_status "Cleanup cancelled"
    fi
}

# Function to run tests
run_tests() {
    print_status "Running tests..."
    
    # Start test dependencies
    docker-compose up -d postgres redis
    
    # Wait for services to be ready
    sleep 10
    
    cd travel-search-service-cred
    
    if [ -f mvnw ]; then
        ./mvnw test
    else
        mvn test
    fi
    
    cd ..
    print_success "Tests completed"
}

# Function to show help
show_help() {
    echo "Flight Search Service Docker Setup"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  setup                 Initial setup (check dependencies, create directories, build)"
    echo "  build                 Build the application"
    echo "  start [ENV]          Start services (ENV: dev, prod, monitoring, all)"
    echo "  stop                 Stop all services"
    echo "  restart [ENV]        Restart services"
    echo "  status               Show service status"
    echo "  logs [SERVICE]       Show logs (optionally for specific service)"
    echo "  test                 Run tests"
    echo "  cleanup              Remove all containers, volumes, and networks"
    echo "  help                 Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 setup             # Initial setup"
    echo "  $0 start dev         # Start development services"
    echo "  $0 start prod        # Start production services"
    echo "  $0 logs postgres     # Show PostgreSQL logs"
    echo "  $0 cleanup           # Clean up everything"
}

# Main script logic
main() {
    case "${1:-help}" in
        "setup")
            check_docker
            check_docker_compose
            create_directories
            setup_environment
            build_application
            print_success "Setup completed! Run '$0 start dev' to start development services."
            ;;
        "build")
            build_application
            ;;
        "start")
            start_services "${2:-dev}"
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            stop_services
            start_services "${2:-dev}"
            ;;
        "status")
            show_status
            ;;
        "logs")
            show_logs "$2"
            ;;
        "test")
            run_tests
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# Run main function with all arguments
main "$@"
