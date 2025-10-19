#!/bin/bash

# Flight Booking System - Docker Build Script
set -e

echo "🚀 Building Flight Booking System Docker Images..."

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

# Build the application
print_status "Building Reservation System Docker image..."
docker build -t reservation-system:latest .

if [ $? -eq 0 ]; then
    print_success "Docker image built successfully!"
else
    print_error "Failed to build Docker image"
    exit 1
fi

# Optional: Tag with version
if [ ! -z "$1" ]; then
    print_status "Tagging image with version: $1"
    docker tag reservation-system:latest reservation-system:$1
    print_success "Image tagged as reservation-system:$1"
fi

# Show image info
print_status "Docker image information:"
docker images reservation-system

print_success "Build completed successfully! 🎉"
print_status "To run the complete system, use: docker-compose up -d"
