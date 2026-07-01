package org.maurodata.plugin.chat.semantic

import spock.lang.Specification

class SemanticSearchExecutionServiceSpec extends Specification {

    void 'chunk kind weights prefer labels over summaries and descriptions'() {
        expect:
        SemanticSearchExecutionService.chunkKindWeight('label') > SemanticSearchExecutionService.chunkKindWeight('summary')
        SemanticSearchExecutionService.chunkKindWeight('summary') > SemanticSearchExecutionService.chunkKindWeight('description-section')
        SemanticSearchExecutionService.chunkKindWeight('description-section') > SemanticSearchExecutionService.chunkKindWeight('unknown')
    }

    void 'domain type weights prefer broader model artefacts'() {
        expect:
        SemanticSearchExecutionService.domainTypeWeight('DataModel') > SemanticSearchExecutionService.domainTypeWeight('DataClass')
        SemanticSearchExecutionService.domainTypeWeight('DataClass') > SemanticSearchExecutionService.domainTypeWeight('DataElement')
        SemanticSearchExecutionService.domainTypeWeight('DataElement') > SemanticSearchExecutionService.domainTypeWeight('DataType')
        SemanticSearchExecutionService.domainTypeWeight('DataType') > SemanticSearchExecutionService.domainTypeWeight('EnumerationValue')
    }

    void 'relation distance weights prefer direct matches over deeper projected matches'() {
        expect:
        SemanticSearchExecutionService.relationDistanceWeight(0) > SemanticSearchExecutionService.relationDistanceWeight(1)
        SemanticSearchExecutionService.relationDistanceWeight(1) > SemanticSearchExecutionService.relationDistanceWeight(4)
    }

    void 'projected support scores increase with source diversity and repeated evidence'() {
        given:
        UUID target = UUID.randomUUID()
        UUID repeatedSource = UUID.randomUUID()
        List<SemanticCandidate> oneSource = [
            projectedCandidate(target, repeatedSource),
            projectedCandidate(target, repeatedSource)
        ]
        List<SemanticCandidate> threeSources = [
            projectedCandidate(target, UUID.randomUUID()),
            projectedCandidate(target, UUID.randomUUID()),
            projectedCandidate(target, UUID.randomUUID())
        ]

        expect:
        SemanticSearchExecutionService.projectedSourceSupportScore(threeSources) >
            SemanticSearchExecutionService.projectedSourceSupportScore(oneSource)
        SemanticSearchExecutionService.repeatedEvidenceScore(threeSources) >
            SemanticSearchExecutionService.repeatedEvidenceScore([threeSources.first()])
    }

    void 'direct target evidence gets a direct support score'() {
        given:
        UUID target = UUID.randomUUID()
        SemanticCandidate direct = new SemanticCandidate(
            sourceId: target,
            sourceDomainType: 'DataModel',
            targetId: target,
            targetDomainType: 'DataModel',
            chunkKind: 'label',
            similarity: 0.8D,
            relationDistance: 0
        )
        SemanticCandidate projected = projectedCandidate(target, UUID.randomUUID())

        expect:
        SemanticSearchExecutionService.directTargetScore([direct]) > 0D
        SemanticSearchExecutionService.directTargetScore([projected]) == 0D
    }

    private static SemanticCandidate projectedCandidate(UUID target, UUID source) {
        new SemanticCandidate(
            sourceId: source,
            sourceDomainType: 'EnumerationValue',
            targetId: target,
            targetDomainType: 'DataModel',
            chunkKind: 'label',
            similarity: 0.8D,
            relationDistance: 4
        )
    }
}
