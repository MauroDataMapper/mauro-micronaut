package org.maurodata.plugin.chat.semantic

import groovy.transform.CompileStatic
import java.sql.Connection
import java.sql.Savepoint

/** A read-only retrieval scope; preserves the caller's connection and transaction settings. */
@CompileStatic
class SetCandidatePlanScope implements AutoCloseable {
    private final Connection connection
    private final String previousMode
    private final Savepoint savepoint

    SetCandidatePlanScope(Connection connection) {
        this.connection = connection
        try (def statement = connection.createStatement(); def rs = statement.executeQuery('SHOW plan_cache_mode')) {
            rs.next()
            previousMode = rs.getString(1)
        }
        savepoint = connection.autoCommit ? null : connection.setSavepoint()
        try {
            setMode('force_custom_plan', savepoint != null)
        } catch (Exception failure) {
            if (savepoint != null) {
                connection.rollback(savepoint)
                connection.releaseSavepoint(savepoint)
            }
            throw failure
        }
    }

    @Override
    void close() {
        if (savepoint != null) {
            // Retrieval only reads. Rolling back the scope also recovers an aborted query
            // and restores SET LOCAL, without rolling back the caller's transaction.
            connection.rollback(savepoint)
            connection.releaseSavepoint(savepoint)
        } else {
            setMode(previousMode, false)
        }
    }

    private void setMode(String mode, boolean local) {
        try (def statement = connection.prepareStatement("SELECT set_config('plan_cache_mode', ?, ?)")) {
            statement.setString(1, mode)
            statement.setBoolean(2, local)
            try (def rs = statement.executeQuery()) { rs.next() }
        }
    }
}
