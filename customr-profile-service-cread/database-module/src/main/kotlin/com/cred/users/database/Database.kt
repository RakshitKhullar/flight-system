package com.cred.users.database

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.runApplication
import java.sql.DriverManager

@SpringBootApplication(exclude = [
    HibernateJpaAutoConfiguration::class,
    DataSourceAutoConfiguration::class,
    JpaRepositoriesAutoConfiguration::class
])
open class Database

fun main(args: Array<String>) {
    println("🚀 Starting Customer Profile Service Database Module...")
    
    val ctx = runApplication<Database>(*args)
    
    try {
        println("Starting Liquibase migration for Customer Profile Service Database...")
        
        // Database connection properties
        val url = "jdbc:postgresql://localhost:5432/customer_profile_service_db"
        val username = "postgres"
        val password = "postgres"
        
        // Get database connection
        val connection = DriverManager.getConnection(url, username, password)
        val database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
        
        // Create Liquibase instance
        val liquibase = Liquibase(
            "db/changelog/db.changelog-master.xml",
            ClassLoaderResourceAccessor(),
            database
        )
        
        // Run migrations
        liquibase.update(Contexts(), LabelExpression())
        
        println("✅ Database migration completed successfully!")
        
        // Print migration status
        val unrunChangeSets = liquibase.listUnrunChangeSets(Contexts(), LabelExpression())
        if (unrunChangeSets.isEmpty()) {
            println("📊 Database is up to date - no pending migrations")
        } else {
            println("⚠️  Found ${unrunChangeSets.size} unrun changesets")
        }
        
        connection.close()
        
    } catch (e: Exception) {
        println("❌ Database migration failed: ${e.message}")
        e.printStackTrace()
    } finally {
        println("🏁 Database operations completed. Shutting down...")
        ctx.close()
    }
}
