package org.maurodata.plugin.chat.semantic

import org.maurodata.service.semantic.SemanticIndexAdministrationService
import org.maurodata.domain.search.dto.SemanticIndexJobDTO

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Singleton

@Slf4j
@CompileStatic
@Singleton
class SemanticIndexJobRecoveryListener implements ApplicationEventListener<StartupEvent> {

    private final SemanticIndexAdministrationService semanticIndexAdministrationService

    SemanticIndexJobRecoveryListener(SemanticIndexAdministrationService semanticIndexAdministrationService) {
        this.semanticIndexAdministrationService = semanticIndexAdministrationService
    }

    @Override
    void onApplicationEvent(StartupEvent event) {
        try {
            List<SemanticIndexJobDTO> recovered = semanticIndexAdministrationService.recoverInterruptedJobs()
            if (!recovered.isEmpty()) {
                log.warn('Recovered {} interrupted semantic indexing jobs: {}', Integer.valueOf(recovered.size()), recovered)
            }
        } catch (Exception e) {
            log.warn('Could not recover interrupted semantic indexing jobs during startup', e)
        }
    }
}
