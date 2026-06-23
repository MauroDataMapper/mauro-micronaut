package org.maurodata.persistence

import groovy.transform.CompileStatic
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.JdbcDatabaseContainerProvider
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@CompileStatic
class PgVectorContainerProvider extends JdbcDatabaseContainerProvider {

    static final String DATABASE_TYPE = 'pgvector'
    static final String IMAGE = 'pgvector/pgvector'
    static final String DEFAULT_TAG = 'pg16'

    @Override
    boolean supports(String databaseType) {
        DATABASE_TYPE == databaseType
    }

    @Override
    JdbcDatabaseContainer newInstance() {
        newInstance(DEFAULT_TAG)
    }

    @Override
    JdbcDatabaseContainer newInstance(String tag) {
        new PostgreSQLContainer(DockerImageName.parse(IMAGE).withTag(tag ?: DEFAULT_TAG))
    }
}
