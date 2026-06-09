package org.maurodata.service.chat.semantic

import groovy.transform.CompileStatic

@CompileStatic
interface RelatedTermsService {
    RelatedTermsResult lookup(String inputText, String context, String locale, String embeddingProvider, String embeddingModel, String generationModel, int max, boolean generateWhenMissing, boolean includePrompts)
}
