package org.maurodata.overlay

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.api.overlay.OverlayApi
import org.maurodata.api.overlay.OverlayApplicationDTO
import org.maurodata.api.overlay.OverlayApplicationResponseDTO
import org.maurodata.api.overlay.OverlayMatchRequestDTO
import org.maurodata.api.overlay.OverlayMatchResultDTO
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Path
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.model.version.VersionChangeType
import org.maurodata.domain.overlay.Overlay
import org.maurodata.domain.overlay.OverlayAction
import org.maurodata.domain.model.ModelPathMatch
import org.maurodata.domain.overlay.OverlayRule
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-overlay.sql", "classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class OverlayIntegrationSpec extends CommonDataSpec {

    @Shared @Inject OverlayApi overlayApi

    void 'Overlay API supports creating, reading, updating, listing, versioning, permissions, moving and deleting'() {
        given:
        Folder sourceFolder = folderApi.create(new Folder(label: 'Overlay API source folder'))
        Folder destinationFolder = folderApi.create(new Folder(label: 'Overlay API destination folder'))
        Overlay created = overlayApi.create(sourceFolder.id, overlay('Customer descriptions'))

        expect:
        created.id
        created.label == 'Customer descriptions'
        created.path.toString() == 'fo:Overlay API source folder|ov:Customer descriptions$main'
        overlayApi.show(created.id).overlayRules[0].actions[0].value == 'Customer information'

        when: 'the overlay is updated and listed from its folder and the catalogue'
        Overlay updated = overlayApi.update(created.id, new Overlay(label: 'Updated customer descriptions', description: 'Maintained by the API test'))
        ListResponse<Overlay> folderOverlays = overlayApi.list(sourceFolder.id)
        ListResponse<Overlay> pagedFolderOverlays = overlayApi.list(sourceFolder.id, new PaginationParams(max: 1))
        ListResponse<Overlay> allOverlays = overlayApi.listAll()
        ListResponse<Overlay> pagedAllOverlays = overlayApi.listAll(new PaginationParams(max: 1))

        then:
        updated.label == 'Updated customer descriptions'
        updated.description == 'Maintained by the API test'
        folderOverlays.items*.id.contains(created.id)
        pagedFolderOverlays.count >= 1
        pagedFolderOverlays.items*.id.contains(created.id)
        allOverlays.items*.id.contains(created.id)
        pagedAllOverlays.count >= 1
        pagedAllOverlays.items.size() <= 1

        when: 'versioning and sharing endpoints are used'
        Overlay finalised = overlayApi.finalise(created.id, new FinaliseData(version: ModelVersion.from('1.0.0')))
        Overlay branch = overlayApi.createNewBranchModelVersion(finalised.id, new CreateNewVersionData(branchName: 'review'))
        overlayApi.allowReadByAuthenticated(branch.id)
        boolean readableByAuthenticated = overlayApi.permissions(branch.id).readableByAuthenticated
        overlayApi.revokeReadByAuthenticated(branch.id)
        overlayApi.allowReadByEveryone(branch.id)
        boolean readableByEveryone = overlayApi.permissions(branch.id).readableByEveryone
        overlayApi.revokeReadByEveryone(branch.id)

        then:
        finalised.finalised
        finalised.modelVersion.toString() == '1.0.0'
        branch.branchName == 'review'
        readableByAuthenticated
        readableByEveryone
        !overlayApi.permissions(branch.id).readableByEveryone

        when: 'the overlay is moved, then soft-deleted and permanently deleted'
        Overlay moved = overlayApi.moveFolder(branch.id, destinationFolder.id.toString())
        HttpResponse softDelete = overlayApi.delete(moved.id, moved, false)
        HttpResponse permanentDelete = overlayApi.delete(created.id, created, true)

        then:
        moved.path.toString().startsWith('fo:Overlay API destination folder|')
        softDelete.status == HttpStatus.NO_CONTENT
        permanentDelete.status == HttpStatus.NO_CONTENT
    }

    void 'Overlay API matches and applies value and metadata actions to model items'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Overlay application folder'))
        DataModel model = dataModelApi.create(folder.id, new DataModel(label: 'Customer model'))
        DataClass dataClass = dataClassApi.create(model.id, new DataClass(label: 'Customer'))
        DataType dataType = dataTypeApi.create(model.id, dataTypesPayload())
        DataElement dataElement = dataElementApi.create(model.id, dataClass.id, dataElementPayload('Name', dataType))
        Overlay overlay = overlayApi.create(folder.id, new Overlay(label: 'Customer enrichment', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Customer')),
                actions: [
                    new OverlayAction(type: 'value', field: 'description', value: 'Customer information', source: 'business-glossary'),
                    new OverlayAction(type: 'metadata', namespace: 'test.overlay', key: 'reviewed', value: 'true', source: 'business-glossary'),
                    new OverlayAction(type: 'annotation', label: 'Glossary note', description: 'Reviewed against the customer glossary.', source: 'business-glossary')
                ]),
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Customer|de:Name')),
                actions: [new OverlayAction(type: 'annotation', label: 'Column note', description: 'The customer name column.')])
        ]))

        when:
        List<OverlayMatchResultDTO> matches = overlayApi.matches(new OverlayMatchRequestDTO(
            rootDomainType: 'DataModel', rootId: model.id, itemDomainType: 'DataClass', itemId: dataClass.id,
            overlayIds: [overlay.id]))
        OverlayApplicationResponseDTO applied = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'DataModel', rootId: model.id, overlayIds: [overlay.id]))
        OverlayApplicationResponseDTO reapplied = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'DataModel', rootId: model.id, overlayIds: [overlay.id]))
        DataClass enriched = dataClassApi.show(model.id, dataClass.id)
        ListResponse<Annotation> annotations = annotationApi.list('DataClass', dataClass.id)
        ListResponse<Annotation> columnAnnotations = annotationApi.list('DataElement', dataElement.id)

        then:
        matches.size() == 1
        matches[0].overlayId == overlay.id
        matches[0].overlayRuleId == overlay.overlayRules[0].id
        matches[0].overlayActionIds == overlay.overlayRules[0].actions*.id
        applied.rootId == model.id
        applied.overlayIds == [overlay.id]
        applied.matchedActions == 4
        applied.appliedActions == 4
        applied.sampleMatch.overlayId == overlay.id
        applied.sampleMatch.overlayRuleId == overlay.overlayRules[0].id
        applied.sampleMatch.itemId == dataClass.id
        applied.sampleMatch.itemPath.endsWith('dc:Customer')
        applied.sampleMatch.selectorPath == 'dc:Customer'
        applied.sampleMatch.actionIds == overlay.overlayRules[0].actions*.id
        applied.sampleMatch.actions.find { it.field == 'description' }.valuePreview == 'Customer information'
        applied.sampleMatch.actions.find { it.key == 'reviewed' }.valuePreview == 'true'
        applied.sampleMatch.actions.find { it.type == 'annotation' }.label == 'Glossary note'
        applied.sampleMatch.actions.find { it.type == 'annotation' }.descriptionPreview == 'Reviewed against the customer glossary.'
        applied.annotationActionResults.size() == 2
        applied.annotationActionResults.find { it.label == 'Glossary note' }.matchedCount == 1
        applied.annotationActionResults.find { it.label == 'Glossary note' }.appliedCount == 1
        applied.annotationActionResults.find { it.label == 'Glossary note' }.matchedPaths == [dataClass.path.toString()]
        applied.annotationActionResults.find { it.label == 'Column note' }.matchedCount == 1
        applied.annotationActionResults.find { it.label == 'Column note' }.matchedPaths == [dataElement.path.toString()]
        reapplied.appliedActions == 4
        reapplied.annotationActionResults.every { it.matchedCount == 1 && it.appliedCount == 1 }
        enriched.description == 'Customer information'
        enriched.metadata.any { Metadata metadata -> metadata.namespace == 'test.overlay' && metadata.key == 'reviewed' && metadata.value == 'true' }
        enriched.metadata.count { Metadata metadata -> metadata.namespace == 'org.maurodata.overlay' && metadata.key == 'source' && metadata.value == 'business-glossary' } == 1
        annotations.items.count { Annotation annotation -> annotation.label == 'Glossary note' && annotation.description == 'Reviewed against the customer glossary.' } == 1
        columnAnnotations.items.count { Annotation annotation -> annotation.label == 'Column note' && annotation.description == 'The customer name column.' } == 1
    }

    void 'OverlayRules are paged by the database and can be moved before another rule'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Overlay rule endpoint folder'))
        Overlay overlay = overlayApi.create(folder.id, structuralOverlayPayload())
        ListResponse<OverlayRule> firstPage = overlayApi.listRules(overlay.id, new PaginationParams(max: 1))
        List<OverlayRule> allRules = overlayApi.listRules(overlay.id, new PaginationParams(max: 0)).items

        when:
        OverlayRule moved = overlayApi.moveRule(overlay.id, allRules[1].id,
            new org.maurodata.api.overlay.OverlayRuleMoveDTO(beforeRuleId: allRules[0].id))
        List<OverlayRule> reordered = overlayApi.listRules(overlay.id, new PaginationParams(max: 0)).items

        then:
        firstPage.count == 2
        firstPage.items.size() == 1
        firstPage.items[0].id == allRules[0].id
        firstPage.items[0].order < allRules[1].order
        moved.id == allRules[1].id
        reordered*.id == [allRules[1].id, allRules[0].id]
        reordered*.order == [1000L, 2000L]
    }

    void 'a UI can update a matching OverlayRule or add an OverlayRule for a known item path'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Overlay editing folder'))
        DataModel model = dataModelApi.create(folder.id, new DataModel(label: 'Overlay editing model'))
        DataClass customer = dataClassApi.create(model.id, new DataClass(label: 'Customer'))
        DataClass supplier = dataClassApi.create(model.id, new DataClass(label: 'Supplier'))
        Overlay created = overlayApi.create(folder.id, new Overlay(label: 'Business descriptions', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Customer')),
                actions: [new OverlayAction(type: 'value', field: 'description', value: 'Initial customer description', source: 'business-glossary')])
        ]))

        when: 'the UI looks up the Overlay, finds the OverlayRule matching Customer, and updates its description action'
        Overlay overlay = overlayApi.show(created.id)
        List<OverlayMatchResultDTO> customerMatches = overlayApi.matches(new OverlayMatchRequestDTO(
            rootDomainType: 'DataModel', rootId: model.id, itemDomainType: 'DataClass', itemId: customer.id,
            overlayIds: [overlay.id]))
        OverlayRule customerOverlayRule = overlay.overlayRules.find { OverlayRule overlayRule -> overlayRule.id == customerMatches[0].overlayRuleId }
        customerOverlayRule.actions[0].value = 'Approved customer description'
        Overlay overlayAfterCustomerDescriptionUpdate = overlayApi.update(overlay.id, new Overlay(
            label: overlay.label, description: overlay.description, overlayRules: overlay.overlayRules))
        OverlayApplicationResponseDTO customerApplied = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'DataModel', rootId: model.id, overlayIds: [overlay.id]))

        then:
        customerMatches.size() == 1
        customerMatches[0].overlayId == overlay.id
        customerMatches[0].overlayActionIds == customerOverlayRule.actions*.id
        overlayAfterCustomerDescriptionUpdate.overlayRules.find { OverlayRule overlayRule -> overlayRule.id == customerOverlayRule.id }.actions[0].value == 'Approved customer description'
        customerApplied.matchedActions == 1
        customerApplied.appliedActions == 1
        dataClassApi.show(model.id, customer.id).description == 'Approved customer description'

        when: 'the UI finds no match for Supplier and adds an OverlayRule using the path it knows'
        Overlay overlayBeforeAddingRule = overlayApi.show(overlay.id)
        List<OverlayMatchResultDTO> supplierMatches = overlayApi.matches(new OverlayMatchRequestDTO(
            rootDomainType: 'DataModel', rootId: model.id, itemDomainType: 'DataClass', itemId: supplier.id,
            overlayIds: [overlay.id]))
        OverlayRule supplierOverlayRule = new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Supplier')),
            actions: [new OverlayAction(type: 'value', field: 'description', value: 'Approved supplier description', source: 'business-glossary')])
        Overlay overlayAfterSupplierOverlayRuleAddition = overlayApi.update(overlay.id, new Overlay(
            label: overlayBeforeAddingRule.label, description: overlayBeforeAddingRule.description,
            overlayRules: overlayBeforeAddingRule.overlayRules + supplierOverlayRule))
        List<OverlayMatchResultDTO> supplierMatchesAfterUpdate = overlayApi.matches(new OverlayMatchRequestDTO(
            rootDomainType: 'DataModel', rootId: model.id, itemDomainType: 'DataClass', itemId: supplier.id,
            overlayIds: [overlay.id]))
        OverlayApplicationResponseDTO bothApplied = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'DataModel', rootId: model.id, overlayIds: [overlay.id]))

        then:
        supplierMatches.empty
        supplierMatchesAfterUpdate.size() == 1
        supplierMatchesAfterUpdate[0].overlayId == overlay.id
        supplierMatchesAfterUpdate[0].overlayRuleId == supplierOverlayRule.id
        overlayAfterSupplierOverlayRuleAddition.overlayRules*.id.containsAll([customerOverlayRule.id, supplierOverlayRule.id])
        bothApplied.matchedActions == 2
        bothApplied.appliedActions == 2
        dataClassApi.show(model.id, customer.id).description == 'Approved customer description'
        dataClassApi.show(model.id, supplier.id).description == 'Approved supplier description'
    }

    void 'finalising an Overlay preserves its structure and branching deep clones it with fresh IDs'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Overlay clone folder'))
        Overlay created = overlayApi.create(folder.id, new Overlay(label: 'Structural overlay', description: 'All the OverlayRules'))
        Overlay populated = overlayApi.update(created.id, structuralOverlayPayload())
        Metadata originalMetadata = metadataApi.create('Overlay', populated.id,
            new Metadata(namespace: 'test.overlay', key: 'catalogue', value: 'business glossary'))
        Overlay beforeFinalising = overlayApi.show(populated.id)

        when:
        Overlay finalised = overlayApi.finalise(populated.id, new FinaliseData(version: ModelVersion.from('2.1.0'), versionTag: 'catalogue release'))

        then: 'finalisation changes version tags but leaves all Overlay structure and item IDs intact'
        finalised.finalised
        finalised.modelVersion.toString() == '2.1.0'
        finalised.modelVersionTag == 'catalogue release'
        metadataApi.list('Overlay', populated.id).items*.id.contains(originalMetadata.id)
        overlayStructure(finalised) == overlayStructure(beforeFinalising)
        finalised.overlayRules*.id == beforeFinalising.overlayRules*.id
        finalised.overlayRules.collectMany { it.actions }*.id == beforeFinalising.overlayRules.collectMany { it.actions }*.id

        when: 'a new branch is created from the finalised Overlay'
        Overlay branch = overlayApi.createNewBranchModelVersion(finalised.id, new CreateNewVersionData(branchName: 'review'))
        Overlay retrievedBranch = overlayApi.show(branch.id)
        List<Metadata> branchMetadata = metadataApi.list('Overlay', branch.id).items

        then: 'the branch retains every OverlayRule and action value while all copied entities have fresh IDs'
        branch.id != finalised.id
        branch.branchName == 'review'
        overlayStructure(retrievedBranch) == overlayStructure(finalised)
        (retrievedBranch.overlayRules*.id as Set).disjoint(finalised.overlayRules*.id as Set)
        (retrievedBranch.overlayRules.collectMany { it.actions }*.id as Set).disjoint(finalised.overlayRules.collectMany { it.actions }*.id as Set)
        branchMetadata.size() == 1
        branchMetadata[0].id != originalMetadata.id
        branchMetadata[0].namespace == originalMetadata.namespace
        branchMetadata[0].key == originalMetadata.key
        branchMetadata[0].value == originalMetadata.value

        when: 'a nested action is changed on the branch'
        retrievedBranch.overlayRules[0].actions[0].value = 'Changed only on review'
        overlayApi.update(branch.id, new Overlay(label: retrievedBranch.label, description: retrievedBranch.description,
            overlayRules: retrievedBranch.overlayRules))

        then: 'the finalised source remains unchanged'
        overlayApi.show(finalised.id).overlayRules[0].actions[0].value == 'Original description'
        overlayApi.show(branch.id).overlayRules[0].actions[0].value == 'Changed only on review'
    }

    void 'Overlay contents inherit versioning from a VersionedFolder and are cloned with it'() {
        given:
        Folder parentFolder = folderApi.create(new Folder(label: 'Versioned Overlay parent'))
        Folder versionedFolder = versionedFolderApi.create(parentFolder.id, new Folder(label: 'Versioned Overlay catalogue'))
        Overlay original = overlayApi.create(versionedFolder.id, structuralOverlayPayload())
        Metadata originalMetadata = metadataApi.create('Overlay', original.id,
            new Metadata(namespace: 'test.overlay', key: 'catalogue', value: 'business glossary'))

        expect: 'the contained Overlay inherits its version from the VersionedFolder'
        metadataApi.list('Overlay', original.id).items*.id.contains(originalMetadata.id)
        original.branchName == null
        original.modelVersion == null
        original.path.toString().contains('vf:Versioned Overlay catalogue$main|ov:')

        when:
        Folder finalisedFolder = versionedFolderApi.finalise(versionedFolder.id,
            new FinaliseData(version: ModelVersion.from('1.0.0'), versionChangeType: VersionChangeType.MAJOR))
        Folder branchFolder = versionedFolderApi.createNewBranchModelVersion(finalisedFolder.id,
            new CreateNewVersionData(branchName: 'review'))
        ListResponse<Overlay> branchOverlays = overlayApi.list(branchFolder.id)
        Overlay branchOverlay = branchOverlays.items[0]
        branchOverlay.overlayRules = overlayApi.listRules(branchOverlay.id, new PaginationParams(max: 0)).items

        then: 'the folder owns the new version and its Overlay contents are independently cloned'
        finalisedFolder.finalised
        finalisedFolder.modelVersion.toString() == '1.0.0'
        branchFolder.branchName == 'review'
        branchOverlays.items.size() == 1
        branchOverlay.id != original.id
        branchOverlay.branchName == null
        branchOverlay.modelVersion == null
        branchOverlay.path.toString().contains('vf:Versioned Overlay catalogue$review|ov:')
        overlayStructure(branchOverlay) == overlayStructure(original)
        (branchOverlay.overlayRules*.id as Set).disjoint(original.overlayRules*.id as Set)
        (branchOverlay.overlayRules.collectMany { it.actions }*.id as Set).disjoint(original.overlayRules.collectMany { it.actions }*.id as Set)
        metadataApi.list('Overlay', branchOverlay.id).items.size() == 1
        metadataApi.list('Overlay', branchOverlay.id).items[0].id != originalMetadata.id
    }

    void 'Overlays apply in request order and merge provenance and additive metadata'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Sequential Overlay folder'))
        DataModel model = dataModelApi.create(folder.id, new DataModel(label: 'Sequential model'))
        DataClass dataClass = dataClassApi.create(model.id, new DataClass(label: 'Customer'))
        Overlay first = overlayApi.create(folder.id, new Overlay(label: 'Glossary overlay', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Customer@description')),
                actions: [
                    new OverlayAction(type: 'value', value: 'Glossary description', source: 'business-glossary'),
                    new OverlayAction(type: 'metadata', namespace: 'test.overlay', key: 'terms', value: 'customer,person', policy: 'add', source: 'business-glossary')
                ])
        ]))
        Overlay second = overlayApi.create(folder.id, new Overlay(label: 'Curation overlay', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Customer')),
                actions: [
                    new OverlayAction(type: 'value', field: 'description', value: 'Curated description', source: 'curation-team'),
                    new OverlayAction(type: 'metadata', namespace: 'test.overlay', key: 'terms', value: 'person,client', policy: 'add', source: 'curation-team')
                ])
        ]))

        when:
        OverlayApplicationResponseDTO firstPass = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'DataModel', rootId: model.id, overlayIds: [first.id, second.id]))
        DataClass afterFirstPass = dataClassApi.show(model.id, dataClass.id)
        OverlayApplicationResponseDTO secondPass = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'DataModel', rootId: model.id, overlayIds: [second.id, first.id]))
        DataClass afterSecondPass = dataClassApi.show(model.id, dataClass.id)

        then:
        firstPass.matchedActions == 4
        firstPass.appliedActions == 4
        afterFirstPass.description == 'Curated description'
        afterFirstPass.metadata.find { it.namespace == 'test.overlay' && it.key == 'terms' }.value == 'customer,person,client'
        afterFirstPass.metadata.find { it.namespace == 'org.maurodata.overlay' && it.key == 'source' }.value == 'business-glossary,curation-team'
        secondPass.matchedActions == 4
        secondPass.appliedActions == 4
        afterSecondPass.description == 'Glossary description'
        afterSecondPass.metadata.find { it.namespace == 'test.overlay' && it.key == 'terms' }.value == 'customer,person,client'
        afterSecondPass.metadata.find { it.namespace == 'org.maurodata.overlay' && it.key == 'source' }.value == 'business-glossary,curation-team'
    }

    void 'empty values and overwrite-if-empty do not write or add provenance while zero is populated'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Overlay policy folder'))
        DataModel model = dataModelApi.create(folder.id, new DataModel(label: 'Overlay policy model'))
        DataClass dataClass = dataClassApi.create(model.id, new DataClass(label: 'Customer', description: 'Existing description', minMultiplicity: 0))
        metadataApi.create('DataClass', dataClass.id, new Metadata(namespace: 'test.overlay', key: 'status', value: 'reviewed'))
        Overlay overlay = overlayApi.create(folder.id, new Overlay(label: 'Conditional overlay', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Customer')),
                actions: [
                    new OverlayAction(type: 'value', field: 'description', value: 'Replacement description', policy: 'overwriteIfEmpty', source: 'description-source'),
                    new OverlayAction(type: 'value', field: 'minMultiplicity', value: 5, policy: 'overwriteIfEmpty', source: 'multiplicity-source'),
                    new OverlayAction(type: 'metadata', namespace: 'test.overlay', key: 'status', value: 'revised', policy: 'overwriteIfEmpty', source: 'metadata-source'),
                    new OverlayAction(type: 'value', field: 'description', value: '', source: 'empty-source')
                ])
        ]))

        when:
        OverlayApplicationResponseDTO response = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'DataModel', rootId: model.id, overlayIds: [overlay.id]))
        DataClass unchanged = dataClassApi.show(model.id, dataClass.id)

        then:
        response.matchedActions == 4
        response.appliedActions == 0
        unchanged.description == 'Existing description'
        unchanged.minMultiplicity == 0
        unchanged.metadata.find { it.namespace == 'test.overlay' && it.key == 'status' }.value == 'reviewed'
        !unchanged.metadata.any { it.namespace == 'org.maurodata.overlay' && it.key == 'source' }
    }

    void 'a path without model identifiers errors across multiple Models unless explicitly allowed'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Overlay ambiguity folder'))
        DataModel mainModel = dataModelApi.create(folder.id, new DataModel(label: 'Ambiguous model one'))
        DataClass mainClass = dataClassApi.create(mainModel.id, new DataClass(label: 'Customer'))
        DataModel secondModel = dataModelApi.create(folder.id, new DataModel(label: 'Ambiguous model two'))
        DataClass secondClass = dataClassApi.create(secondModel.id, new DataClass(label: 'Customer'))
        Overlay overlay = overlayApi.create(folder.id, ambiguityOverlay(false))

        when:
        overlayApi.apply(new OverlayApplicationDTO(rootDomainType: 'Folder', rootId: folder.id, overlayIds: [overlay.id]))

        then: 'matching both models is rejected before any value is changed'
        HttpClientResponseException ambiguity = thrown()
        ambiguity.status == HttpStatus.CONFLICT
        dataClassApi.show(mainModel.id, mainClass.id).description == null
        dataClassApi.show(secondModel.id, secondClass.id).description == null

        when: 'the match explicitly permits every matching Model'
        overlayApi.update(overlay.id, ambiguityOverlay(true))
        OverlayApplicationResponseDTO applied = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'Folder', rootId: folder.id, overlayIds: [overlay.id]))

        then:
        applied.matchedActions == 2
        applied.appliedActions == 2
        dataClassApi.show(mainModel.id, mainClass.id).description == 'Applied to every version'
        dataClassApi.show(secondModel.id, secondClass.id).description == 'Applied to every version'
    }

    void 'suffix selectors apply table and column descriptions across different models in a Folder'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Cross-model Overlay folder'))
        DataModel ouhModel = dataModelApi.create(folder.id, new DataModel(label: 'Ouh_Cerner'))
        DataModel rbhModel = dataModelApi.create(folder.id, new DataModel(label: 'Rbhft_Cerner'))
        DataClass ouhTable = dataClassApi.create(ouhModel.id, new DataClass(label: 'Ouh_Cerner_Order_Text'))
        DataClass rbhTable = dataClassApi.create(rbhModel.id, new DataClass(label: 'Rbhft_Cerner_Order_Text'))
        DataType ouhType = dataTypeApi.create(ouhModel.id, dataTypesPayload())
        DataType rbhType = dataTypeApi.create(rbhModel.id, dataTypesPayload())
        DataElement ouhColumn = dataElementApi.create(ouhModel.id, ouhTable.id, dataElementPayload('Catalog_Cd', ouhType))
        DataElement rbhColumn = dataElementApi.create(rbhModel.id, rbhTable.id, dataElementPayload('Catalog_Cd', rbhType))
        Overlay overlay = overlayApi.create(folder.id, new Overlay(label: 'Model-independent descriptions', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Order_Text@description'),
                matchPathSuffix: true, matchFirstNodeIdentifierSuffix: true, allowMultipleModels: true),
                actions: [new OverlayAction(type: 'value', field: 'description', value: 'Shared table description')]),
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Order_Text|de:Catalog_Cd'),
                matchPathSuffix: true, matchFirstNodeIdentifierSuffix: true, allowMultipleModels: true),
                actions: [new OverlayAction(type: 'value', field: 'description', value: 'Shared column description')])
        ]))

        when:
        OverlayApplicationResponseDTO applied = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'Folder', rootId: folder.id, overlayIds: [overlay.id]))

        then:
        applied.matchedActions == 4
        applied.appliedActions == 4
        dataClassApi.show(ouhModel.id, ouhTable.id).description == 'Shared table description'
        dataClassApi.show(rbhModel.id, rbhTable.id).description == 'Shared table description'
        dataElementApi.show(ouhModel.id, ouhTable.id, ouhColumn.id).description == 'Shared column description'
        dataElementApi.show(rbhModel.id, rbhTable.id, rbhColumn.id).description == 'Shared column description'
    }

    void 'selectors outside the model-item SQL fast path use the generic matcher'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Generic Overlay matcher folder'))
        DataModel model = dataModelApi.create(folder.id, new DataModel(label: 'Generic matcher model'))
        DataType dataType = dataTypeApi.create(model.id, dataTypesPayload())
        Overlay overlay = overlayApi.create(folder.id, new Overlay(label: 'Generic matcher Overlay', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path("dt:${dataType.label}")),
                actions: [new OverlayAction(type: 'value', field: 'description', value: 'Generic matcher result')])
        ]))

        when:
        OverlayApplicationResponseDTO response = overlayApi.apply(new OverlayApplicationDTO(
            rootDomainType: 'DataModel', rootId: model.id, overlayIds: [overlay.id]))

        then:
        response.matchedActions == 1
        response.appliedActions == 1
        dataTypeApi.show(model.id, dataType.id).description == 'Generic matcher result'
    }

    void 'applying an Overlay to a finalised VersionedFolder is denied'() {
        given:
        Folder parentFolder = folderApi.create(new Folder(label: 'Finalised Overlay root parent'))
        Folder draftVersionedFolder = versionedFolderApi.create(parentFolder.id,
            new Folder(label: 'Finalised Overlay root'))
        DataModel model = dataModelApi.create(draftVersionedFolder.id, new DataModel(label: 'Finalised Overlay model'))
        DataClass dataClass = dataClassApi.create(model.id, new DataClass(label: 'Customer'))
        Overlay overlay = overlayApi.create(parentFolder.id, new Overlay(label: 'Finalised root test', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dm:Finalised Overlay model|dc:Customer')),
                actions: [new OverlayAction(type: 'value', field: 'description', value: 'Must not be applied')])
        ]))
        Folder finalisedFolder = versionedFolderApi.finalise(draftVersionedFolder.id,
            new FinaliseData(version: ModelVersion.from('1.0.0'), versionChangeType: VersionChangeType.MAJOR))
        DataModel finalisedModel = dataModelApi.list(finalisedFolder.id).items.find { it.label == model.label }
        DataClass finalisedClass = dataClassApi.list(finalisedModel.id).items.find { it.label == dataClass.label }

        when:
        overlayApi.apply(new OverlayApplicationDTO(rootDomainType: 'VersionedFolder', rootId: finalisedFolder.id,
            overlayIds: [overlay.id]))

        then:
        HttpClientResponseException denied = thrown()
        denied.status == HttpStatus.UNPROCESSABLE_ENTITY
        dataClassApi.show(finalisedModel.id, finalisedClass.id).description == null
    }

    private static Overlay overlay(String label) {
        new Overlay(label: label, overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('')),
                actions: [new OverlayAction(type: 'value', field: 'description', value: 'Customer information', source: 'customer-glossary')])
        ])
    }

    private static Overlay structuralOverlayPayload() {
        new Overlay(label: 'Structural overlay', description: 'All the OverlayRules', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Customer@description'), allowMultipleModels: true),
                actions: [
                    new OverlayAction(type: 'value', value: 'Original description', source: 'glossary', policy: 'overwriteIfEmpty'),
                    new OverlayAction(type: 'metadata', namespace: 'org.example', key: 'purpose', value: 'business terms', source: 'glossary', policy: 'add')
                ]),
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Customer')),
                actions: [new OverlayAction(type: 'metadata', namespace: 'org.example', key: 'status', value: 'reviewed', source: 'review-board')])
        ])
    }

    private static List<Map> overlayStructure(Overlay overlay) {
        overlay.overlayRules.collect { OverlayRule overlayRule ->
            [path: overlayRule.match.path.toString(), allowMultipleModels: overlayRule.match.allowMultipleModels,
             actions: overlayRule.actions.collect { OverlayAction action ->
                 [type: action.type, field: action.field, namespace: action.namespace, key: action.key,
                  value: action.value, policy: action.policy, source: action.source]
             }]
        }
    }

    private static Overlay ambiguityOverlay(boolean allowMultipleModels) {
        new Overlay(label: 'Ambiguous selector', overlayRules: [
            new OverlayRule(match: new ModelPathMatch(path: new Path('dc:Customer'), allowMultipleModels: allowMultipleModels,
                matchPathSuffix: true),
                actions: [new OverlayAction(type: 'value', field: 'description', value: 'Applied to every version', source: 'test-glossary')])
        ])
    }
}
