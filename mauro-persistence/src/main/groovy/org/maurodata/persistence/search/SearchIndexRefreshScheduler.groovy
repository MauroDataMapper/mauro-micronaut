package org.maurodata.persistence.search

import io.micronaut.scheduling.annotation.Scheduled
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.inject.Singleton

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource

@Singleton
class SearchIndexRefreshScheduler {

    @Inject
    DataSource dataSource

    private volatile long lastSeenVersion = -1
    private volatile boolean refreshInProgress = false

    @Scheduled(fixedDelay = "30s")
    void checkAndRefresh() {
        if (refreshInProgress) {
            return // already running
        }

        long currentVersion = fetchVersion()

        if (currentVersion > lastSeenVersion) {
            runRefreshLoop()
        }
    }

    private synchronized void runRefreshLoop() {
        if (refreshInProgress) return

        refreshInProgress = true

        try {
            while (true) {
                long before = fetchVersion()

                refreshMaterializedViews()

                lastSeenVersion = before

                long after = fetchVersion()

                if (after == before) {
                    // nothing changed during refresh
                    break
                }

                // else loop again — coalesced into single extra run
            }
        } finally {
            refreshInProgress = false
        }
    }

    @Transactional
    long fetchVersion() {
        try (Connection conn = dataSource.getConnection()
             Statement st = conn.createStatement()
             ResultSet rs = st.executeQuery(
                 "SELECT version FROM search.search_domains_dirty_flag LIMIT 1")) {

            if (!rs.next()) {
                // no row present — return 0 (Perhaps no rows after initialisation)
                return 0L
            }

            return rs.getLong(1)

        } catch (SQLException e) {
            throw new RuntimeException(e)
        }
    }

    @Transactional
    void refreshMaterializedViews() {
        try (Connection conn = dataSource.getConnection()
             Statement st1 = conn.createStatement()
             Statement st2 = conn.createStatement()) {

            st1.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY search.metadata_agg")
            st2.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY search.search_domains")


        } catch (SQLException e) {
            throw new RuntimeException(e)
        }
    }
}