package org.maurodata.persistence

import groovy.transform.CompileStatic

import java.sql.Connection
import java.sql.Statement

@CompileStatic
class PgVectorTestDatabase {

    static void createVectorExtension(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute('CREATE EXTENSION IF NOT EXISTS vector')
        }
    }
}
