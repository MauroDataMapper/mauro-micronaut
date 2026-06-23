package org.maurodata.persistence.search

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.ApplicationEventPublisher
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

    final long advisoryLockKey = 123454321L

    private volatile long lastSeenVersion = -1
    private volatile boolean refreshInProgress = false

    @Scheduled(fixedDelay = '${mauro.search.rebuild.poll-interval:10s}')
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
            // Debounce passed and dbVersion > processedVersion -> attempt to run rebuild
            boolean locked = tryAcquireAdvisoryLock(advisoryLockKey)
            if (!locked) {
                log.info("Could not acquire advisory lock; another instance may be rebuilding. Will retry later.")
                return
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
                releaseAdvisoryLock(advisoryLockKey)
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
    @Transactional
    boolean tryAcquireAdvisoryLock(long key) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT pg_try_advisory_lock(?)")
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
    @Transactional
    void releaseAdvisoryLock(long key) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT pg_advisory_unlock(?)")
            ps.setLong(1, key)
            ps.executeQuery()
        }
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
