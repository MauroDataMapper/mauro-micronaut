package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.event.ApplicationEventListener
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton
import org.maurodata.persistence.search.SearchDomainsRefreshedEvent
import org.maurodata.domain.search.dto.SemanticIndexJobDTO

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Slf4j
@CompileStatic
@Singleton
class SemanticIndexRefreshListener implements ApplicationEventListener<SearchDomainsRefreshedEvent> {

    private final SemanticIndexAdministrationService semanticIndexAdministrationService
    private final ExecutorService executorService = Executors.newSingleThreadExecutor({Runnable runnable ->
        Thread thread = new Thread(runnable, 'semantic-index-reconcile-worker')
        thread.daemon = true
        thread
    })
    private volatile boolean reconcileInProgress = false
    private volatile long lastProcessedVersion = -1L

    SemanticIndexRefreshListener(SemanticIndexAdministrationService semanticIndexAdministrationService) {
        this.semanticIndexAdministrationService = semanticIndexAdministrationService
    }

    @Override
    synchronized void onApplicationEvent(SearchDomainsRefreshedEvent event) {
        if (!semanticIndexAdministrationService.autoReconcileEnabled()) {
            log.debug('Semantic index auto reconcile disabled; ignoring search domains refresh version {}', Long.valueOf(event.version()))
            return
        }
        if (reconcileInProgress) {
            log.info('Semantic index reconcile already in progress; search domains refresh version {} will be picked up later if needed', Long.valueOf(event.version()))
            return
        }
        if (event.version() <= lastProcessedVersion) {
            log.debug('Semantic index reconcile already processed search domains version {}', Long.valueOf(event.version()))
            return
        }

        reconcileInProgress = true
        long version = event.version()
        executorService.submit({
            runReconcile(version)
        } as Runnable)
    }

    private void runReconcile(long version) {
        try {
            log.info('Semantic index reconcile triggered by search domains refresh version {}', Long.valueOf(version))
            List<SemanticIndexJobDTO> results = semanticIndexAdministrationService.reconcileDeclaredIndexes()
            lastProcessedVersion = version
            log.info('Semantic index reconcile completed for search domains version {} with {} index results', Long.valueOf(version), Integer.valueOf(results.size()))
        } catch (Exception e) {
            log.error('Semantic index reconcile failed for search domains version {}', Long.valueOf(version), e)
        } finally {
            reconcileInProgress = false
        }
    }

    @PreDestroy
    void shutdownExecutor() {
        executorService.shutdownNow()
    }
}
