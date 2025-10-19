# Database Module

This module contains database configurations, migrations, and database-related utilities for the User Service.

## Features

- **PostgreSQL** as the primary database
- **Liquibase** for database migrations using XML format
- **Kotlin Spring Boot Application** for running migrations
- **H2** for testing
- **TestContainers** for integration testing
- Proper indexing and constraints
- Automated migration execution with status reporting

## Database Schema

### Customer Table
The main table storing customer information with the following fields:
- `id` - Primary key (auto-increment)
- `user_id` - Unique UUID for the customer
- `user_name` - Unique username for login
- `user_email` - Unique email address
- `password` - Encrypted password
- `first_name`, `last_name` - Customer names
- `phone_number` - Contact number
- `address`, `city`, `country`, `postal_code` - Address information
- `is_verified` - Email verification status
- `is_active` - Account status (for soft delete)
- `verification_code` - Email verification code
- `verification_code_expiry` - Expiry time for verification code
- `created_at`, `updated_at` - Timestamps

## Migration Files

### V1__Create_customer_table.xml
- Creates the customer table with all required columns
- Adds indexes for performance optimization
- Creates PostgreSQL function and trigger for automatic `updated_at` updates
- Uses Liquibase XML format with proper changesets

### V2__Add_customer_constraints.xml
- Adds validation constraints using PostgreSQL regex patterns
- Email format validation
- Phone number format validation
- Name length validations
- Adds comprehensive table and column comments
- Includes rollback instructions for each changeset

## Usage

### Running Migrations

#### Option 1: Using the Kotlin Application (Recommended)
```bash
# From the database-module directory
mvn spring-boot:run

# Or compile and run the JAR
mvn clean package
java -jar target/database-module-0.0.1-SNAPSHOT.jar
```

#### Option 2: Using Liquibase Maven Plugin
```bash
# From the database-module directory
mvn liquibase:update

# To check migration status
mvn liquibase:status

# To validate migrations
mvn liquibase:validate

# To rollback migrations
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# To generate SQL for review (dry run)
mvn liquibase:updateSQL
```

### Database Configuration

Update the following properties in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/customer_profile_service_db
    username: your_username
    password: your_password
```

### Testing

The module uses H2 in-memory database for testing. Test configuration is in `application-test.yml`. Liquibase is disabled for tests to allow JPA DDL auto-generation.

### Docker Setup

To run PostgreSQL locally using Docker:

```bash
docker run --name postgres-customer-profile-service \
  -e POSTGRES_DB=customer_profile_service_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:15
```

## Best Practices

1. **Always use migrations** - Never modify the database schema directly
2. **Version your migrations** - Follow the V{version}__{description}.xml naming convention
3. **Test migrations** - Test both forward and rollback scenarios using Liquibase
4. **Use constraints** - Ensure data integrity at the database level
5. **Index appropriately** - Add indexes for frequently queried columns
6. **Include rollbacks** - Always provide rollback instructions in changesets
7. **Use contexts** - Leverage Liquibase contexts for environment-specific changes
8. **Atomic changesets** - Keep changesets focused and atomic

## Adding New Migrations

1. Create a new XML file in `src/main/resources/db/changelog/`
2. Follow naming convention: `V{next_version}__{description}.xml`
3. Use Liquibase XML format with proper changesets
4. Include rollback instructions for each changeset
5. Add the new file to `db.changelog-master.xml`
6. Test the migration locally before committing
7. Update this README if adding new tables or significant changes

### XML Migration Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="unique-id" author="your-name">
        <comment>Description of changes</comment>
        
        <!-- Your changes here -->
        
        <rollback>
            <!-- Rollback instructions -->
        </rollback>
    </changeSet>

</databaseChangeLog>
```
