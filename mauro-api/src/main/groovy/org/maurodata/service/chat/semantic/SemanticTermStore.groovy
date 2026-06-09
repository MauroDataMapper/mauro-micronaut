package org.maurodata.service.chat.semantic

import groovy.transform.CompileStatic

@CompileStatic
interface SemanticTermStore {
    void saveTerms(String inputText, String context, List<RelatedTerm> terms)
    List<RelatedTerm> findSimilar(String inputText, String context, String embeddingProvider, String embeddingModel, int max)
}
