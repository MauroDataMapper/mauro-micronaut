package org.maurodata.plugin.chat.semantic

import groovy.transform.CompileStatic

@CompileStatic
class SetSemanticCandidate {
    UUID sourceSetId
    String sourceSetDomainType
    String sourceVectorFamily
    String sourceCentroidKind
    Integer sourceRegionOrdinal
    UUID targetSetId
    String targetSetDomainType
    String targetSetLabel
    UUID targetMauroModelId
    String targetVectorFamily
    String targetCentroidKind
    Integer targetRegionOrdinal
    Integer targetMemberCount
    Integer targetSourceMemberCount
    Map<String, Object> targetMetadata = [:]
    Double distance
    Double similarity
}
