package org.maurodata.plugin.chat.semantic

import org.maurodata.domain.search.dto.SearchRequestDTO

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class SetSemanticSearchRequest extends SearchRequestDTO {
    SetSemanticSearchRequest() { max = 20 }
    String embeddingProfile
    Boolean diagnostics = false
    Integer candidateLimit = 100
    String query
    Boolean includeIdentifiers = false
}
