-- Setup script for PostgreSQL database
-- Run this script as postgres user: psql -U postgres -f setup-database.sql

-- Create database
CREATE DATABASE reservation_db;

-- Create user
CREATE USER reservation_user WITH PASSWORD 'reservation_pass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE reservation_db TO reservation_user;

-- Connect to the new database and grant schema privileges
\c reservation_db;
GRANT ALL ON SCHEMA public TO reservation_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO reservation_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO reservation_user;

-- Grant default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO reservation_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO reservation_user;
