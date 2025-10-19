#!/bin/bash

echo "Setting up PostgreSQL database for reservation system..."

# Check if PostgreSQL is running
if ! pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
    echo "❌ PostgreSQL is not running. Please start PostgreSQL first:"
    echo "   brew services start postgresql"
    echo "   or"
    echo "   pg_ctl -D /usr/local/var/postgres start"
    exit 1
fi

echo "✅ PostgreSQL is running"

# Run the setup script
echo "Creating database and user..."
psql -U postgres -h localhost -p 5432 -f setup-database.sql

if [ $? -eq 0 ]; then
    echo "✅ Database setup completed successfully!"
    echo "Database: reservation_db"
    echo "User: reservation_user"
    echo "Password: reservation_pass"
else
    echo "❌ Database setup failed. Please check the error messages above."
    exit 1
fi
