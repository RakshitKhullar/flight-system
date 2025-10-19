#!/bin/bash

echo "🚀 Starting PostgreSQL and Cassandra for Reservation System..."

# Start the databases
docker-compose up -d

echo "⏳ Waiting for databases to be ready..."

# Wait for PostgreSQL
echo "Checking PostgreSQL..."
until docker exec reservation-postgres pg_isready -U reservation_user -d reservation_db > /dev/null 2>&1; do
    echo "PostgreSQL is starting up..."
    sleep 2
done
echo "✅ PostgreSQL is ready!"

# Wait for Cassandra
echo "Checking Cassandra..."
until docker exec reservation-cassandra cqlsh -e "describe cluster" > /dev/null 2>&1; do
    echo "Cassandra is starting up..."
    sleep 5
done
echo "✅ Cassandra is ready!"

echo ""
echo "🎉 All databases are ready!"
echo ""
echo "📊 Database Connection Details:"
echo "PostgreSQL:"
echo "  Host: localhost"
echo "  Port: 5432"
echo "  Database: reservation_db"
echo "  Username: reservation_user"
echo "  Password: reservation_pass"
echo ""
echo "Cassandra:"
echo "  Host: localhost"
echo "  Port: 9042"
echo "  Cluster: ReservationCluster"
echo ""
echo "🔧 Useful Commands:"
echo "  Stop databases: docker-compose down"
echo "  View logs: docker-compose logs -f"
echo "  Connect to PostgreSQL: docker exec -it reservation-postgres psql -U reservation_user -d reservation_db"
echo "  Connect to Cassandra: docker exec -it reservation-cassandra cqlsh"
