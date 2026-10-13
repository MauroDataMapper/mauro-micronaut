package org.maurodata.persistence.search

import io.micronaut.context.annotation.Requires
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Inject
import jakarta.inject.Singleton

import java.time.Instant

/** Invokes the refresh policy on a schedule; the policy itself remains callable deterministically in tests. */
@Singleton
@Requires(property = 'mauro.search.rebuild.scheduled.enabled', value = 'true', defaultValue = 'true')
class SearchIndexRefreshTrigger {

    @Inject
    SearchIndexRefreshScheduler refreshScheduler

    @Scheduled(fixedDelay = '${mauro.search.rebuild.poll-interval:10s}')
    void scheduledCheck() {
        refreshScheduler.checkAt(Instant.now())
    }
}
