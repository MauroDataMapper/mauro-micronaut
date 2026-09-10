package org.maurodata.plugin.chat.semantic

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.web.ListResponse

@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@CompileStatic
@Introspected
class SetSemanticSearchResponse extends ListResponse<Map<String, Object>> {
    String emptyReason
    String countScope = 'retrieved-shortlist'
    List<Map<String, Object>> dimensions
    Map<String, Object> source
    Integer sourceVectorCount
    Integer centroidHitsPerVector
    Boolean candidateBudgetReached
    String embeddingProfile
}
