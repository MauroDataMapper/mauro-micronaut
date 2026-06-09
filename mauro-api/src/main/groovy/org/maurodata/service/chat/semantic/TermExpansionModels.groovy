package org.maurodata.service.chat.semantic

import groovy.transform.CompileStatic

@CompileStatic
class TermExpansionPrompt {
    String category
    String prompt
}

@CompileStatic
class RelatedTerm {
    String text
    String normalizedText
    String relation
    String source
    String sourceModel
    BigDecimal score
    Map<String, Object> metadata = [:]
}

@CompileStatic
class RelatedTermsResult {
    String inputText
    String normalizedInputText
    String context
    String locale
    String lookupStrategy
    String embeddingProvider
    String embeddingModel
    List<RelatedTerm> terms = []
    List<TermExpansionPrompt> prompts = []
}
