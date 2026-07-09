package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.event.ApplicationEventListener
import jakarta.inject.Singleton
import org.maurodata.persistence.search.SearchDomainsRefreshedEvent
import org.maurodata.domain.search.dto.SemanticIndexJobDTO

@Slf4j
@CompileStatic
@Singleton
class SemanticIndexRefreshListener implements ApplicationEventListener<SearchDomainsRefreshedEvent> {

    private final SemanticIndexAdministrationService semanticIndexAdministrationService
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
        try {
            log.info('Semantic index reconcile triggered by search domains refresh version {}', Long.valueOf(event.version()))
            List<SemanticIndexJobDTO> results = semanticIndexAdministrationService.reconcileDeclaredIndexes()
            lastProcessedVersion = event.version()
            log.info('Semantic index reconcile completed for search domains version {} with {} index results', Long.valueOf(event.version()), Integer.valueOf(results.size()))
        } catch (Exception e) {
            log.error('Semantic index reconcile failed for search domains version {}', Long.valueOf(event.version()), e)
        } finally {
            reconcileInProgress = false
        }
    }
}
