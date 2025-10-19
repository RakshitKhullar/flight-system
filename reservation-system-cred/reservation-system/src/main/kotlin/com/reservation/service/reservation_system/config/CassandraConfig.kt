package com.reservation.service.reservation_system.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption

@Configuration
class CassandraConfig : AbstractCassandraConfiguration() {

    @Value("\${spring.cassandra.keyspace-name:reservation_system}")
    private lateinit var keyspaceName: String

    @Value("\${spring.cassandra.contact-points:127.0.0.1}")
    private lateinit var contactPoints: String

    @Value("\${spring.cassandra.port:9042}")
    private var port: Int = 9042

    @Value("\${spring.cassandra.local-datacenter:datacenter1}")
    private lateinit var localDatacenter: String

    override fun getKeyspaceName(): String = keyspaceName

    override fun getContactPoints(): String = contactPoints

    override fun getPort(): Int = port

    override fun getLocalDataCenter(): String = localDatacenter

    override fun getSchemaAction(): SchemaAction = SchemaAction.CREATE_IF_NOT_EXISTS

    override fun getKeyspaceCreations(): List<CreateKeyspaceSpecification> {
        return listOf(
            CreateKeyspaceSpecification.createKeyspace(keyspaceName)
                .ifNotExists()
                .with(KeyspaceOption.DURABLE_WRITES, true)
                .withSimpleReplication(1)
        )
    }

    override fun getEntityBasePackages(): Array<String> {
        return arrayOf("com.reservation.service.reservation_system.entity.cassandra")
    }
}
