package org.maurodata.service.semantic

import groovy.transform.CompileStatic

@CompileStatic
interface SemanticIndexAdministrationService {

    Map<String, Object> rebuildCatalogueIndex(String indexName,
                                              String corpusName,
                                              List<String> domainTypes,
                                              UUID mauroModelId,
                                              Integer maxRows,
                                              Integer batchSize,
                                              boolean force)

    List<Map<String, Object>> reconcileDeclaredIndexes()

    boolean hasEmbeddings(String indexName)
}
