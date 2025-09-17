package org.maurodata.controller.security.tracking

import jakarta.inject.Singleton

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.*

@Singleton
class SessionTracker {

    private final Map<String, TrackedSession> active = new ConcurrentHashMap<>()

    private static final long INACTIVITY_LIMIT_MINUTES = 30

    void updateSession(String sessionId, String username, String url) {

        TrackedSession ts = active.get(sessionId)
        if (ts == null) {
            ts = new TrackedSession(sessionId)
            active.put(sessionId, ts)
        }
        ts.userEmailAddress = username
        ts.lastAccessedUrl = url
        ts.lastAccessedDateTime = Instant.now()
    }

    Collection<TrackedSession> getActiveSessions() {
        Instant cutoff = Instant.now().minus(INACTIVITY_LIMIT_MINUTES, ChronoUnit.MINUTES)

        active.values().removeIf {TrackedSession trackedSession ->
            trackedSession.lastAccessedDateTime.isBefore(cutoff)
        }

        return active.values()
    }
}
