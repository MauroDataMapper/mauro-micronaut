package org.maurodata.plugin.chat.semantic

import groovy.transform.CompileStatic

@CompileStatic
class SetSemanticMemberEmbedding {
    UUID setId
    String setDomainType
    String setLabel
    UUID mauroModelId
    String vectorFamily
    UUID memberId
    String memberDomainType
    String memberLabel
    String memberText
    String contentHash
    float[] embedding
}
