#!/bin/bash

echo "🛑 Stopping PostgreSQL and Cassandra..."

# Stop the databases
docker-compose down

echo "✅ Databases stopped successfully!"
echo ""
echo "💡 To remove all data volumes as well, run:"
echo "   docker-compose down -v"
