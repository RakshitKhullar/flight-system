package com.cred.users.database.config

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.sql.DataSource
import org.slf4j.LoggerFactory

@Component
class DatabaseConfig(private val dataSource: DataSource) {
    
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)
    
    @EventListener(ApplicationReadyEvent::class)
    fun checkDatabaseConnection() {
        try {
            dataSource.connection.use { connection ->
                logger.info("✅ Database connection successful!")
                logger.info("Database URL: ${connection.metaData.url}")
                logger.info("Database Product: ${connection.metaData.databaseProductName}")
                logger.info("Database Version: ${connection.metaData.databaseProductVersion}")
            }
        } catch (e: Exception) {
            logger.error("❌ Database connection failed: ${e.message}")
            logger.error("Please ensure PostgreSQL is running and the database 'reservation_db' exists")
            logger.error("Connection details: jdbc:postgresql://localhost:5432/reservation_db")
        }
    }
}
