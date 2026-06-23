package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

@CompileStatic
@Singleton
@Requires(missingBeans = SemanticIndexAdministrationService)
class NoOpSemanticIndexAdministrationService implements SemanticIndexAdministrationService {

    static final String REASON = 'semantic index implementation is not installed'

    @Override
    Map<String, Object> rebuildCatalogueIndex(String indexName,
                                              String corpusName,
                                              List<String> domainTypes,
                                              UUID mauroModelId,
                                              Integer maxRows,
                                              Integer batchSize,
                                              boolean force) {
        [
            indexName: indexName ?: 'catalogue-items-default',
            corpusName: corpusName ?: 'catalogue-items',
            status: 'unavailable',
            reason: REASON
        ] as Map<String, Object>
    }

    @Override
    List<Map<String, Object>> reconcileDeclaredIndexes() {
        Collections.emptyList()
    }

    @Override
    boolean hasEmbeddings(String indexName) {
        false
    }
}
