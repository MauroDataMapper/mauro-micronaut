package org.maurodata.service.semantic

import groovy.transform.CompileStatic

@CompileStatic
interface EmbeddingProvider {

    String id()

    boolean supports(EmbeddingProfile profile)

    List<float[]> embed(EmbeddingProfile profile, List<String> texts)
}
