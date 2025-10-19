#!/bin/bash

echo "🚀 Running Flight Search Service Tests"
echo "======================================"

# Set test profile
export SPRING_PROFILES_ACTIVE=test

# Run all tests with Maven
echo "📋 Running Unit Tests..."
mvn clean test -Dtest="**/*Test" -DfailIfNoTests=false

echo ""
echo "🔄 Running Integration Tests..."
mvn clean test -Dtest="**/*IntegrationTest" -DfailIfNoTests=false

echo ""
echo "📊 Generating Test Report..."
mvn surefire-report:report

echo ""
echo "✅ Test execution completed!"
echo "📄 Test reports available in: target/site/surefire-report.html"
