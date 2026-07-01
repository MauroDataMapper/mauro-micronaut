package org.maurodata.persistence.search

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.data.connection.annotation.Connectable
import io.micronaut.scheduling.annotation.Scheduled
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.inject.Singleton

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

@Singleton
@Slf4j
class SearchIndexRefreshScheduler {

    @Inject
    DataSource dataSource

    @Inject
    ApplicationEventPublisher<SearchDomainsRefreshedEvent> eventPublisher

    @Value('${mauro.search.rebuild.poll-inactivity-period:30s}')
    Duration debounce

    @Value('${mauro.search.rebuild.single-instance:true}')
    boolean singleInstance

    final long advisoryLockKey = 123454321L

    private volatile long lastSeenVersion = -1
    private volatile boolean refreshInProgress = false

    @Scheduled(fixedDelay = '${mauro.search.rebuild.poll-interval:10s}')
    @Connectable
    synchronized void scheduledCheck() {
        log.trace("Scheduled Check...")
        if (refreshInProgress) {
            return
        }
        refreshInProgress = true

        try {
            Map response = readVersionAndTimestamp()
            long version = response.version
            Instant lastUpdated = response.lastUpdated

            // If nothing new, quick exit
            if (version <= lastSeenVersion) {
                return
            }
            // If the last change is recent, wait for inactivity window
            Duration sinceLast = Duration.between(lastUpdated, Instant.now())
            if (sinceLast < debounce) {
                log.debug("Change observed but debounce window not passed ({}s left)",
                          (debounce.minus(sinceLast).toMillis() / 1000.0).round(1))
                return
            }
            // Debounce passed and dbVersion > processedVersion -> attempt to run rebuild.
            // PostgreSQL advisory locks are session-scoped, so acquire and release must use
            // the same JDBC connection. Returning a pooled connection does not close the
            // database session, and can otherwise leave the lock held by the pool.
            try (Connection advisoryLockConnection = dataSource.getConnection()) {
                boolean locked = tryAcquireAdvisoryLock(advisoryLockConnection, advisoryLockKey)
                if (!locked) {
                    if (singleInstance) {
                        int terminatedLockHolders = terminateAdvisoryLockHolders(advisoryLockConnection, advisoryLockKey)
                        if (terminatedLockHolders > 0) {
                            log.warn(
                                "Terminated {} stale advisory lock holder(s) for search index rebuild in single-instance mode; retrying lock acquisition.",
                                Integer.valueOf(terminatedLockHolders)
                            )
                            locked = tryAcquireAdvisoryLock(advisoryLockConnection, advisoryLockKey)
                        }
                    }
                    if (!locked) {
                        log.info("Could not acquire advisory lock; another instance may be rebuilding. Will retry later.")
                        return
                    }
                }

                try {
                    // Re-check DB after acquiring lock to avoid races
                    Map freshResponse = readVersionAndTimestamp()
                    long freshVersion = freshResponse.version
                    if (freshVersion <= lastSeenVersion) {
                        log.debug("No new version after acquiring lock (freshVersion=${freshVersion} processed=${lastSeenVersion}).")
                        return
                    }

                    log.info("Debounce passed. Running rebuild for version ${freshVersion} (processed=${lastSeenVersion})")
                    refreshMaterializedViews()   // implement your refresh/indexing logic here
                    lastSeenVersion = freshVersion
                    eventPublisher.publishEvent(new SearchDomainsRefreshedEvent(freshVersion, Instant.now()))
                    log.info("Rebuild finished. processedVersion set to ${lastSeenVersion}")
                } finally {
                    releaseAdvisoryLock(advisoryLockConnection, advisoryLockKey)
                }
            }

        } catch (Exception e) {
            log.error("Error in scheduledCheck", e)
            // keep processedVersion unchanged so we retry later
        } finally {
            refreshInProgress = false
        }
    }

    @Transactional
    void refreshMaterializedViews() {
        try (Connection conn = dataSource.getConnection()
            Statement st1 = conn.createStatement()
            Statement st2 = conn.createStatement()) {

            log.debug("Starting search index refresh")
            st1.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY search.metadata_agg")
            st2.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY search.search_domains")
            log.debug("Completed search index refresh")


        } catch (SQLException e) {
            throw new RuntimeException(e)
        }
    }

    @Transactional
    void markIndexDirty() {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE search.search_domains_dirty_flag " +
                                                         "SET version = version + 1, " +
                                                         "last_updated = now() " +
                                                         "WHERE id = TRUE")
            ps.executeUpdate()
        }
    }

    /**
     * Try to get Postgres advisory lock for a 64-bit key.
     * Returns true if lock acquired.
     */
    boolean tryAcquireAdvisoryLock(Connection conn, long key) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            ps.setLong(1, key)
            try (ResultSet rs = ps.executeQuery()) {
                rs.next()
                return rs.getBoolean(1)
            }
        }
    }

    /**
     * Release the advisory lock for the key.
     */
    void releaseAdvisoryLock(Connection conn, long key) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            ps.setLong(1, key)
            ps.executeQuery()
        }
    }

    /**
     * In single-instance deployments a held scheduler advisory lock is assumed stale if
     * it belongs to another backend. PostgreSQL advisory locks cannot be unlocked from a
     * different session, so the recovery action is terminating the backend that owns it.
     */
    int terminateAdvisoryLockHolders(Connection conn, long key) {
        long highKey = key >> 32
        long lowKey = key & 0xffffffffL
        List<Integer> lockHolderPids = []

        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT pid FROM pg_locks " +
            "WHERE locktype = 'advisory' " +
            "AND granted " +
            "AND objsubid = 1 " +
            "AND classid = (?::bigint)::oid " +
            "AND objid = (?::bigint)::oid " +
            "AND pid <> pg_backend_pid()")) {
            ps.setLong(1, highKey)
            ps.setLong(2, lowKey)
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lockHolderPids.add(Integer.valueOf(rs.getInt(1)))
                }
            }
        }

        int terminated = 0
        for (Integer pid : lockHolderPids) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT pg_terminate_backend(?)")) {
                ps.setInt(1, pid.intValue())
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getBoolean(1)) {
                        terminated++
                    }
                }
            } catch (SQLException e) {
                log.warn("Could not terminate stale advisory lock holder pid={}", pid, e)
            }
        }
        return terminated
    }

    /**
     * Read current (version, last_updated_at).
     * Returns a Map: [version: long, lastUpdated: Instant]
     */
    @Transactional
    Map readVersionAndTimestamp() {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT version, last_updated FROM search.search_domains_dirty_flag WHERE id = TRUE")
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long v = rs.getLong(1)
                    Instant t = rs.getTimestamp(2).toInstant()
                    return [version: v, lastUpdated: t]
                } else {
                    ps = conn.prepareStatement("INSERT INTO search.search_domains_dirty_flag (id, version, last_updated) values (true, 0, now())")
                    ps.executeUpdate()
                    return [version: 0, lastUpdated: Instant.now()]
                }
            }
        }
    }
}
