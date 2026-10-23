package org.maurodata.persistence.facet

import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.ItemLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Path
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.datamodel.DataModelRepository
import jakarta.inject.Inject
import spock.lang.Specification

@ContainerizedTest
class ItemLinkRepositorySpec extends Specification {
    @Inject ContentsService contentsService
    @Inject ModelCacheableRepository.FolderCacheableRepository folderRepository
    @Inject DataModelRepository dataModelRepository
    @Inject ItemLinkRepository itemLinkRepository
    @Inject FacetCacheableRepository.ItemLinkCacheableRepository itemLinkCacheableRepository

    UUID firstOwnerId
    UUID secondOwnerId

    void setup() {
        Folder myFolder = folderRepository.save(new Folder(label: "ItemLink repository folder ${UUID.randomUUID()}"))
        DataModel model = DataModel.build {
            label "ItemLink repository model ${UUID.randomUUID()}"
            folder myFolder
            branchName 'main'
            dataClass { label 'First owner' }
            dataClass { label 'Second owner' }
        }
        model = contentsService.saveWithContent(model) as DataModel
        DataModel loadedModel = dataModelRepository.loadWithContent(model.id)
        firstOwnerId = loadedModel.dataClasses.find { it.label == 'First owner' }.id
        secondOwnerId = loadedModel.dataClasses.find { it.label == 'Second owner' }.id
    }

    void 'repository persists typed paths and scopes reads and deletes to the owning item'() {
        given:
        ItemLink firstPath = saveItemLink(firstOwnerId, 'DataFlow', 'dc:Source one')
        ItemLink secondPath = saveItemLink(firstOwnerId, 'DataFlow', 'dm:External$1.0.0|dc:Source two')
        ItemLink otherTypeSamePath = saveItemLink(firstOwnerId, 'SemanticLink', 'dc:Source one')
        ItemLink otherOwnerPath = saveItemLink(secondOwnerId, 'DataFlow', 'dc:Source one')

        when:
        Set<ItemLink> firstOwnerPaths = itemLinkRepository.readAllByMultiFacetAwareItemId(firstOwnerId)
        Set<ItemLink> firstOwnerDataFlowPaths = itemLinkRepository.readAllByMultiFacetAwareItemIdAndType(firstOwnerId, 'DataFlow')
        Set<ItemLink> bothOwnersPaths = itemLinkRepository.readAllByMultiFacetAwareItemIdIn([firstOwnerId, secondOwnerId])
        Set<ItemLink> cacheablePaths = itemLinkCacheableRepository.readAllByMultiFacetAwareItemId(firstOwnerId)
        Set<ItemLink> cacheableDataFlowPaths = itemLinkCacheableRepository.readAllByMultiFacetAwareItemIdAndType(firstOwnerId, 'DataFlow')

        then:
        firstOwnerPaths*.id as Set == [firstPath.id, secondPath.id, otherTypeSamePath.id] as Set
        firstOwnerPaths.collect { it.path.toString() } as Set == ['dc:Source one', 'dm:External$1.0.0|dc:Source two'] as Set
        firstOwnerPaths.find { it.type == 'SemanticLink' && it.path.toString() == 'dc:Source one' }
        firstOwnerDataFlowPaths*.id as Set == [firstPath.id, secondPath.id] as Set
        bothOwnersPaths*.id as Set == [firstPath.id, secondPath.id, otherTypeSamePath.id, otherOwnerPath.id] as Set
        cacheablePaths*.id as Set == [firstPath.id, secondPath.id, otherTypeSamePath.id] as Set
        cacheableDataFlowPaths*.id as Set == [firstPath.id, secondPath.id] as Set
        itemLinkRepository.handles(ItemLink)
        itemLinkRepository.handles('ItemLink')
        itemLinkRepository.handlesPathPrefix('IL')

        when:
        Long deleted = itemLinkRepository.deleteByMultiFacetAwareItemIdIn([firstOwnerId])

        then:
        deleted == 3
        itemLinkRepository.readAllByMultiFacetAwareItemId(firstOwnerId).empty
        (itemLinkRepository.readAllByMultiFacetAwareItemId(secondOwnerId)*.id as Set) == [otherOwnerPath.id] as Set
    }

    void 'repository finds links by type and any candidate source path across potential target owners'() {
        given:
        ItemLink matchingPath = saveItemLink(firstOwnerId, 'DataFlow', 'dc:Known source')
        ItemLink matchingPathOtherOwner = saveItemLink(secondOwnerId, 'DataFlow', 'dc:Known source')
        ItemLink matchingAlternatePath = saveItemLink(firstOwnerId, 'DataFlow', 'dc:Known source two')
        saveItemLink(firstOwnerId, 'SemanticLink', 'dc:Known source')
        saveItemLink(firstOwnerId, 'DataFlow', 'dc:Other source')

        when:
        Set<ItemLink> matches = itemLinkRepository.readAllByTypeAndPathIn('DataFlow',
            ['dc:Known source', 'dc:Known source two'])

        then:
        matches*.id as Set == [matchingPath.id, matchingPathOtherOwner.id, matchingAlternatePath.id] as Set
    }

    private ItemLink saveItemLink(UUID ownerId, String type, String path) {
        itemLinkRepository.save(new ItemLink(
            multiFacetAwareItemDomainType: 'DataClass',
            multiFacetAwareItemId: ownerId,
            type: type,
            path: new Path(path)
        ))
    }
}
