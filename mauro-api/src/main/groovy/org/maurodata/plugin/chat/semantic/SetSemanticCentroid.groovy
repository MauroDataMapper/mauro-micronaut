package org.maurodata.plugin.chat.semantic

import groovy.transform.CompileStatic

@CompileStatic
class SetSemanticCentroid {
    UUID setId
    String setDomainType
    String setLabel
    UUID mauroModelId
    String vectorFamily
    String centroidKind
    Integer regionOrdinal = 0
    Integer memberCount = 0
    Integer sourceMemberCount = 0
    Double residualRadius
    Double localRadius
    String sourceFingerprint
    float[] embedding
    Map<String, Object> metadata = [:]
}
