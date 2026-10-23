package org.maurodata.facet

import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.api.facet.DataFlowSourceCreateDTO
import org.maurodata.api.facet.DataComponentCreateDTO
import org.maurodata.api.facet.DataFlowNetworkApi
import org.maurodata.api.link.ItemLinkDTO
import org.maurodata.api.link.ItemLinkGraphApi
import org.maurodata.api.link.ItemLinkGraphDTO
import org.maurodata.api.link.ItemLinkKindDTO
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataComponent
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ItemLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.model.Path
import org.maurodata.export.ExportModel
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.HttpStatus
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.service.path.DataFlowNetworkService
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@ContainerizedTest
@Singleton
class DataFlowNetworkIntegrationSpec extends CommonDataSpec {
    @Shared @Inject DataFlowNetworkApi dataFlowNetworkApi
    @Shared @Inject DataFlowNetworkService dataFlowNetworkService
    @Shared @Inject ItemLinkGraphApi itemLinkGraphApi
    @Shared @Inject FacetCacheableRepository.ItemLinkCacheableRepository itemLinkRepository
    UUID folderId
    UUID dataModelId
    UUID targetDataClassId
    UUID sourceDataClassId
    UUID dataComponentId

    void setup() {
        folderId = folderApi.create(folder()).id
        dataModelId = dataModelApi.create(folderId, dataModelPayload()).id
        targetDataClassId = dataClassApi.create(dataModelId, dataClassPayload('DataFlow target')).id
        sourceDataClassId = dataClassApi.create(dataModelId, dataClassPayload('DataFlow source')).id
        dataComponentId = dataFlowNetworkApi.createDataFlowNetworkComponent(dataModelId,
            new DataComponentCreateDTO(label: 'Flow process')).id
    }

    void 'create list show and delete data flow path using typed DTOs'() {
        when:
        ItemLinkDTO created = dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: 'dc:DataFlow source'))

        then:
        created.id
        created.type == 'DataFlow'
        created.path == 'dc:DataFlow source'

        when:
        ListResponse<ItemLinkDTO> listed = dataFlowNetworkApi.list('DataComponent', dataComponentId)
        ItemLinkDTO shown = dataFlowNetworkApi.show('DataComponent', dataComponentId, created.id)

        then:
        listed.count == 1
        listed.items[0].path == created.path
        shown.id == created.id
        shown.path == created.path

        when:
        dataFlowNetworkApi.create('DataComponent', dataComponentId, new DataFlowSourceCreateDTO(path: created.path))

        then:
        HttpClientResponseException duplicate = thrown()
        duplicate.status == HttpStatus.CONFLICT

        when:
        dataFlowNetworkApi.delete('DataComponent', dataComponentId, created.id)

        then:
        dataFlowNetworkApi.list('DataComponent', dataComponentId).count == 0
    }

    void 'DataFlow API creates lists shows and deletes DataComponents'() {
        when:
        DataComponent created = dataFlowNetworkApi.createDataFlowNetworkComponent(dataModelId,
            new DataComponentCreateDTO(label: 'Empty process'))

        then:
        created.id
        created.type == DataComponent.DATA_FLOW_TYPE
        created.label == 'Empty process'
        created.path.lastPathNode().prefix == 'dcp'
        created.path.lastPathNode().identifier == 'DataFlow: Empty process'
        new DataComponent(type: 'OtherType', label: created.label).pathIdentifier != created.path.lastPathNode().identifier
        dataFlowNetworkApi.listDataFlowNetworkComponents(dataModelId).items*.id.contains(created.id)
        dataFlowNetworkApi.showDataFlowNetworkComponent(dataModelId, created.id).label == created.label

        when:
        dataFlowNetworkApi.createDataFlowNetworkComponent(dataModelId, new DataComponentCreateDTO(label: 'Empty process'))

        then:
        HttpClientResponseException duplicateComponent = thrown()
        duplicateComponent.status == HttpStatus.CONFLICT

        when:
        HttpResponse deleted = dataFlowNetworkApi.deleteDataFlowNetworkComponent(dataModelId, created.id)

        then:
        deleted.status == HttpStatus.NO_CONTENT
        !dataFlowNetworkApi.listDataFlowNetworkComponents(dataModelId).items*.id.contains(created.id)
    }

    void 'DataComponent creation wires consumes and produces as relative DataFlow paths'() {
        when:
        DataComponent flow = createDataFlowNetworkComponent(dataModelId, 'Practical process',
            ['dc:DataFlow source'], ['dc:DataFlow target'])
        List<ItemLinkDTO> inputs = dataFlowNetworkApi.list('DataComponent', flow.id).items
        List<ItemLinkDTO> outputs = dataFlowNetworkApi.list('DataClass', targetDataClassId).items
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataClass', targetDataClassId, ['DataFlow'])

        then:
        inputs*.path == ['dc:DataFlow source']
        outputs*.path == [flow.path.localPath().toString()]
        graph.items*.id == [targetDataClassId, flow.id, sourceDataClassId]
        graph.links*.sourceId == [flow.id, sourceDataClassId]
        graph.links*.targetId == [targetDataClassId, flow.id]
    }

    void 'one consumed DataElement can produce several target DataElements across DataModels'() {
        given:
        UUID sourceFolderId = folderApi.create(folder('People source database')).id
        DataModel sourceModel = dataModelApi.create(sourceFolderId, dataModelPayload('People source model'))
        DataClass people = dataClassApi.create(sourceModel.id, dataClassPayload('People'))
        DataType fullNameType = dataTypeApi.create(sourceModel.id, primitiveDataType('Full name type'))
        DataElement fullName = dataElementApi.create(sourceModel.id, people.id,
            new DataElement(label: 'fullName', dataType: new DataType(id: fullNameType.id)))
        dataModelApi.finalise(sourceModel.id, new FinaliseData(version: ModelVersion.from('1.0.0')))
        String fullNamePath = Path.fromPathRootedAtFirstModelIdentifier(
            dataElementApi.show(sourceModel.id, people.id, fullName.id).path.toString()).toString()

        DataClass person = dataClassApi.create(dataModelId, dataClassPayload('Person'))
        DataType namePartType = dataTypeApi.create(dataModelId, primitiveDataType('Name part type'))
        List<DataElement> nameParts = ['firstName', 'middleName', 'lastName'].collect { String label ->
            dataElementApi.create(dataModelId, person.id,
                new DataElement(label: label, dataType: new DataType(id: namePartType.id)))
        }
        List<String> producedPaths = nameParts.collect { DataElement element -> "dc:Person|de:${element.label}" }

        when:
        DataComponent flow = createDataFlowNetworkComponent(dataModelId, 'Split full name', [fullNamePath], producedPaths)
        List<ItemLinkDTO> consumes = dataFlowNetworkApi.list('DataComponent', flow.id).items
        List<List<ItemLinkDTO>> producedLinks = nameParts.collect { DataElement element ->
            dataFlowNetworkApi.list('DataElement', element.id).items
        }
        List<ItemLinkGraphDTO> outputGraphs = nameParts.collect { DataElement element ->
            itemLinkGraphApi.traverseIncomingLinks('DataElement', element.id, ['DataFlow'])
        }

        then:
        consumes*.path == [fullNamePath]
        producedLinks*.size() == [1, 1, 1]
        producedLinks.flatten()*.path == [flow.path.localPath().toString()] * 3
        outputGraphs.every { ItemLinkGraphDTO graph ->
            List<UUID> siblingIds = nameParts.findAll { DataElement element -> element.id != graph.items[0].id }
                .collect { DataElement element -> element.id }.sort()
            graph.items*.id == [graph.items[0].id, flow.id, fullName.id] + siblingIds
        }
        outputGraphs.every { ItemLinkGraphDTO graph ->
            List<UUID> siblingIds = nameParts.findAll { DataElement element -> element.id != graph.items[0].id }
                .collect { DataElement element -> element.id }.sort()
            graph.links*.sourceId == [flow.id, fullName.id] + ([flow.id] * siblingIds.size()) &&
                graph.links*.targetId == [graph.items[0].id, flow.id] + siblingIds
        }
    }

    void 'two separately finalised source DataModels can be consumed by one component'() {
        given:
        UUID sourceAlphaFolderId = folderApi.create(folder('Merge source alpha')).id
        DataModel sourceAlphaModel = dataModelApi.create(sourceAlphaFolderId, dataModelPayload('Alpha database'))
        DataClass sourceAlphaTable = dataClassApi.create(sourceAlphaModel.id, dataClassPayload('Alpha source'))
        DataType sourceAlphaType = dataTypeApi.create(sourceAlphaModel.id, primitiveDataType('Alpha key type'))
        DataElement sourceAlpha = dataElementApi.create(sourceAlphaModel.id, sourceAlphaTable.id,
            new DataElement(label: 'alphaKey', dataType: new DataType(id: sourceAlphaType.id)))
        dataModelApi.finalise(sourceAlphaModel.id, new FinaliseData(version: ModelVersion.from('1.0.0')))
        String sourceAlphaPath = Path.fromPathRootedAtFirstModelIdentifier(
            dataElementApi.show(sourceAlphaModel.id, sourceAlphaTable.id, sourceAlpha.id).path.toString()).toString()

        UUID sourceBetaFolderId = folderApi.create(folder('Merge source beta')).id
        DataModel sourceBetaModel = dataModelApi.create(sourceBetaFolderId, dataModelPayload('Beta database'))
        DataClass sourceBetaTable = dataClassApi.create(sourceBetaModel.id, dataClassPayload('Beta source'))
        DataType sourceBetaType = dataTypeApi.create(sourceBetaModel.id, primitiveDataType('Beta key type'))
        DataElement sourceBeta = dataElementApi.create(sourceBetaModel.id, sourceBetaTable.id,
            new DataElement(label: 'betaKey', dataType: new DataType(id: sourceBetaType.id)))
        dataModelApi.finalise(sourceBetaModel.id, new FinaliseData(version: ModelVersion.from('2.0.0')))
        String sourceBetaPath = Path.fromPathRootedAtFirstModelIdentifier(
            dataElementApi.show(sourceBetaModel.id, sourceBetaTable.id, sourceBeta.id).path.toString()).toString()

        DataType targetType = dataTypeApi.create(dataModelId, primitiveDataType('Merged key type'))
        DataElement target = dataElementApi.create(dataModelId, targetDataClassId,
            new DataElement(label: 'mergedKey', dataType: new DataType(id: targetType.id)))

        when:
        DataComponent flow = createDataFlowNetworkComponent(dataModelId, 'Merge sources',
            [sourceAlphaPath, sourceBetaPath], ['dc:DataFlow target|de:mergedKey'])
        List<ItemLinkDTO> consumes = dataFlowNetworkApi.list('DataComponent', flow.id).items
        List<ItemLinkDTO> producedLinks = dataFlowNetworkApi.list('DataElement', target.id).items
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataElement', target.id, ['DataFlow'])

        then:
        consumes.size() == 2
        consumes*.path as Set == ([sourceAlphaPath, sourceBetaPath] as Set)
        producedLinks.size() == 1
        producedLinks.first().path == flow.path.localPath().toString()
        graph.items.take(2)*.id == [target.id, flow.id]
        graph.items.drop(2)*.id as Set == ([sourceAlpha.id, sourceBeta.id] as Set)
        graph.links.size() == 3
        graph.links.first().sourceId == flow.id
        graph.links.first().targetId == target.id
        graph.links.first().path == absolutePath(flow.path)
        graph.links.drop(1)*.sourceId as Set == ([sourceAlpha.id, sourceBeta.id] as Set)
        graph.links.drop(1)*.targetId == [flow.id, flow.id]
        graph.links.drop(1)*.path as Set == ([sourceAlphaPath, sourceBetaPath] as Set)
    }

    void 'DataFlow rejects links between two DataComponents'() {
        when:
        createDataFlowNetworkComponent(dataModelId, 'Downstream process',
            ['dcp:DataFlow%: Flow process'], [])

        then:
        HttpClientResponseException resolvedComponentLink = thrown()
        resolvedComponentLink.status == HttpStatus.UNPROCESSABLE_ENTITY
        !dataFlowNetworkApi.listDataFlowNetworkComponents(dataModelId).items*.label.contains('Downstream process')

        when:
        dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: 'dcp:Missing component'))

        then:
        HttpClientResponseException unresolvedComponentLink = thrown()
        unresolvedComponentLink.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    void 'DataFlow rejects unsupported endpoints and links that bypass a DataComponent'() {
        when:
        dataFlowNetworkApi.create('DataClass', targetDataClassId, new DataFlowSourceCreateDTO(path: 'dc:DataFlow source'))

        then:
        HttpClientResponseException directClassLink = thrown()
        directClassLink.status == HttpStatus.UNPROCESSABLE_ENTITY

        when:
        dataFlowNetworkApi.create('DataComponent', dataComponentId, new DataFlowSourceCreateDTO(path: 'dt:Some type'))

        then:
        HttpClientResponseException unsupportedSource = thrown()
        unsupportedSource.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    void 'a DataComponent in a finalised DataModel can source a link to a different editable DataModel'() {
        given:
        Folder sourceVersionedFolder = versionedFolderApi.create(folder('External process'))
        DataModel sourceModel = dataModelApi.create(sourceVersionedFolder.id, dataModelPayload('External process model'))
        DataComponent source = dataFlowNetworkApi.createDataFlowNetworkComponent(sourceModel.id,
            new DataComponentCreateDTO(label: 'External process'))
        versionedFolderApi.finalise(sourceVersionedFolder.id,
            new FinaliseData(version: ModelVersion.from('4.0.0')))
        String sourcePath = dataFlowNetworkApi.showDataFlowNetworkComponent(sourceModel.id, source.id).path.toString()
        DataType targetType = dataTypeApi.create(dataModelId, primitiveDataType('External flow output type'))
        DataElement targetElement = dataElementApi.create(dataModelId, targetDataClassId,
            new DataElement(label: 'External flow output', dataType: new DataType(id: targetType.id)))

        when:
        ItemLinkDTO output = dataFlowNetworkApi.create('DataClass', targetDataClassId,
            new DataFlowSourceCreateDTO(path: sourcePath))
        ItemLinkDTO elementOutput = dataFlowNetworkApi.create('DataElement', targetElement.id,
            new DataFlowSourceCreateDTO(path: sourcePath))
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataClass', targetDataClassId, ['DataFlow'])
        ItemLinkGraphDTO elementGraph = itemLinkGraphApi.traverseIncomingLinks('DataElement', targetElement.id, ['DataFlow'])

        then:
        output.path == sourcePath
        elementOutput.path == sourcePath
        dataFlowNetworkApi.list('DataClass', targetDataClassId).items*.id == [output.id]
        dataFlowNetworkApi.list('DataElement', targetElement.id).items*.id == [elementOutput.id]
        graph.links.first().sourceId == source.id
        graph.links.first().targetId == targetDataClassId
        elementGraph.links.first().sourceId == source.id
        elementGraph.links.first().targetId == targetElement.id
    }

    void 'DataComponent creation rejects produced targets outside its DataModel atomically'() {
        given:
        UUID otherFolderId = folderApi.create(folder('Other output folder')).id
        DataModel otherModel = dataModelApi.create(otherFolderId, dataModelPayload('Other output model'))
        dataClassApi.create(otherModel.id, dataClassPayload('External output'))

        when:
        dataFlowNetworkApi.createDataFlowNetworkComponent(dataModelId, new DataComponentCreateDTO(
            label: 'Rejected process', produces: [new DataFlowSourceCreateDTO(path: 'dc:External output')]))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
        !dataFlowNetworkApi.listDataFlowNetworkComponents(dataModelId).items*.label.contains('Rejected process')
    }

    void 'DataComponent creation rejects another DataComponent as a produced target'() {
        when:
        dataFlowNetworkApi.createDataFlowNetworkComponent(dataModelId, new DataComponentCreateDTO(
            label: 'Rejected component output', produces: [new DataFlowSourceCreateDTO(path: 'dcp:DataFlow%: Flow process')]))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
        !dataFlowNetworkApi.listDataFlowNetworkComponents(dataModelId).items*.label.contains('Rejected component output')
    }

    void 'deleting a DataComponent removes local output links but preserves other links'() {
        given:
        DataType outputType = dataTypeApi.create(dataModelId, primitiveDataType('Output value type'))
        DataElement outputElement = dataElementApi.create(dataModelId, targetDataClassId,
            new DataElement(label: 'Output value', dataType: new DataType(id: outputType.id)))
        DataComponent component = createDataFlowNetworkComponent(dataModelId, 'Component to delete',
            ['dc:DataFlow source'], ['dc:DataFlow target', 'dc:DataFlow target|de:Output value'])
        DataComponent otherComponent = createDataFlowNetworkComponent(dataModelId, 'Component to retain',
            [], ['dc:DataFlow target'])
        ItemLinkDTO otherOutput = dataFlowNetworkApi.list('DataClass', targetDataClassId).items
            .find { ItemLinkDTO link -> link.path == otherComponent.path.localPath().toString() }

        expect:
        dataFlowNetworkApi.list('DataClass', targetDataClassId).count == 2
        dataFlowNetworkApi.list('DataElement', outputElement.id).count == 1
        otherOutput

        when:
        dataFlowNetworkApi.deleteDataFlowNetworkComponent(dataModelId, component.id)

        then:
        dataFlowNetworkApi.list('DataClass', targetDataClassId).items*.id == [otherOutput.id]
        dataFlowNetworkApi.list('DataElement', outputElement.id).count == 0
        dataFlowNetworkApi.listDataFlowNetworkComponents(dataModelId).items*.id.contains(otherComponent.id)
        !dataFlowNetworkApi.listDataFlowNetworkComponents(dataModelId).items*.id.contains(component.id)
    }

    void 'DataFlow API and graph ignore links of other types on the same item and path'() {
        given:
        ItemLinkDTO dataFlow = dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: 'dc:DataFlow source'))
        ItemLink otherTypeLink = itemLinkRepository.save(new ItemLink(
            type: 'SemanticLink',
            path: new Path(dataFlow.path),
            multiFacetAwareItemId: dataComponentId,
            multiFacetAwareItemDomainType: 'DataComponent'
        ))
        assert otherTypeLink.id
        assert itemLinkRepository.readById(otherTypeLink.id).type == 'SemanticLink'

        when:
        ListResponse<ItemLinkDTO> listed = dataFlowNetworkApi.list('DataComponent', dataComponentId)
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataComponent', dataComponentId, ['DataFlow'])

        then:
        listed.items*.id == [dataFlow.id]
        graph.links*.id == [dataFlow.id]
        graph.links*.type == ['DataFlow']

        when:
        ItemLinkDTO otherTypeShow = dataFlowNetworkApi.show('DataComponent', dataComponentId, otherTypeLink.id)

        then:
        !otherTypeShow

        when:
        dataFlowNetworkService.show(dataFlowNetworkApi.showDataFlowNetworkComponent(dataModelId, dataComponentId), otherTypeLink.id)

        then:
        HttpStatusException showException = thrown()
        showException.status == HttpStatus.NOT_FOUND

        when:
        HttpResponse deleteResponse = dataFlowNetworkApi.delete('DataComponent', dataComponentId, otherTypeLink.id)

        then:
        deleteResponse.status == HttpStatus.NOT_FOUND
        itemLinkRepository.readById(otherTypeLink.id).type == 'SemanticLink'
    }

    void 'service normalises and validates relative paths against the target model'() {
        given:
        DataComponent target = dataFlowNetworkApi.showDataFlowNetworkComponent(dataModelId, dataComponentId)

        when:
        Path normalised = dataFlowNetworkService.normaliseAndValidate('fo:Ignored folder|dc:DataFlow source', target)

        then:
        normalised.toString() == 'fo:Ignored folder|dc:DataFlow source'
        !normalised.isAbsolute()
    }

    void 'supported link kinds are reported and incoming link traversal is breadth first'() {
        given:
        DataClass source = dataClassApi.show(dataModelId, sourceDataClassId)
        DataClass furtherSource = dataClassApi.create(dataModelId,
            new DataClass(label: 'Further data flow source', description: 'Further source description'))
        DataComponent sourceFlow = createDataFlowNetworkComponent(dataModelId, 'Source flow',
            ['dc:Further data flow source'], ['dc:DataFlow source'])
        DataComponent targetFlow = createDataFlowNetworkComponent(dataModelId, 'Target flow',
            ['dc:DataFlow source'], ['dc:DataFlow target'])
        metadataApi.create('DataClass', furtherSource.id,
            new Metadata(namespace: 'org.example', key: 'meaning', value: 'meaningful source'))
        annotationApi.create('DataClass', sourceDataClassId, annotationPayload('Source context', 'Context description'))

        when:
        ListResponse<ItemLinkKindDTO> kinds = itemLinkGraphApi.listSupportedLinkKinds()
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataClass', targetDataClassId, ['DataFlow'])

        then:
        kinds.items*.type == ['DataFlow']
        graph.items*.id == [targetDataClassId, targetFlow.id, sourceDataClassId, sourceFlow.id, furtherSource.id]
        graph.items*.domainType == ['DataClass', 'DataComponent', 'DataClass', 'DataComponent', 'DataClass']
        graph.items[0].owningModel.id == dataModelId
        graph.items[0].owningModel.label == dataModelApi.show(dataModelId).label
        graph.items[0].owningModel.finalised == false
        graph.items[0].owningModel.branchName == 'main'
        graph.items[0].owningModel.modelVersion == null
        graph.items[0].owningModel.modelVersionTag == null
        graph.items[2].annotations*.label == ['Source context']
        graph.items[2].annotations[0].description == 'Context description'
        graph.items[4].metadata*.value == ['meaningful source']
        graph.links*.targetId == [targetDataClassId, targetFlow.id, sourceDataClassId, sourceFlow.id]
        graph.links*.sourceId == [targetFlow.id, sourceDataClassId, sourceFlow.id, furtherSource.id]
        graph.links*.path == [
            absolutePath(targetFlow.path),
            absolutePath(source.path),
            absolutePath(sourceFlow.path),
            absolutePath(furtherSource.path)
        ]
        graph.links*.type == ['DataFlow', 'DataFlow', 'DataFlow', 'DataFlow']
    }

    void 'graph item summaries include catalogue breadcrumbs through nested DataClasses to DataElements'() {
        given:
        DataClass nestedClass = dataClassApi.create(dataModelId, targetDataClassId,
            new DataClass(label: 'Nested graph class'))
        DataType outputType = dataTypeApi.create(dataModelId, primitiveDataType('Graph output type'))
        DataElement outputElement = dataElementApi.create(dataModelId, nestedClass.id,
            new DataElement(label: 'Graph output element', dataType: new DataType(id: outputType.id)))
        DataComponent flow = createDataFlowNetworkComponent(dataModelId, 'Graph breadcrumb process',
            ['dc:DataFlow source'], ['dc:DataFlow target|dc:Nested graph class|de:Graph output element'])

        when:
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataElement', outputElement.id, ['DataFlow'])

        then:
        graph.items*.id == [outputElement.id, flow.id, sourceDataClassId]
        graph.items[0].breadcrumbs*.id == [folderId, dataModelId, targetDataClassId, nestedClass.id, outputElement.id]
        graph.items[0].breadcrumbs*.domainType == ['Folder', 'DataModel', 'DataClass', 'DataClass', 'DataElement']
        graph.items[0].breadcrumbs*.label == [
            folderApi.show(folderId).label,
            dataModelApi.show(dataModelId).label,
            'DataFlow target', 'Nested graph class', 'Graph output element'
        ]
        graph.items[1].breadcrumbs*.id == [folderId, dataModelId, flow.id]
        graph.items[1].breadcrumbs*.domainType == ['Folder', 'DataModel', 'DataComponent']
        graph.items[2].breadcrumbs*.id == [folderId, dataModelId, sourceDataClassId]
    }

    void 'graph includes readable targets that reference the root without following their links'() {
        given:
        DataComponent referencingComponent = createDataFlowNetworkComponent(dataModelId, 'References graph root',
            ['dc:DataFlow target', 'dc:DataFlow source'], [])

        when:
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataClass', targetDataClassId, ['DataFlow'])

        then:
        graph.items*.id == [targetDataClassId, referencingComponent.id]
        graph.links.size() == 1
        graph.links[0].sourceId == targetDataClassId
        graph.links[0].sourceDomainType == 'DataClass'
        graph.links[0].targetId == referencingComponent.id
        graph.links[0].targetDomainType == 'DataComponent'
        graph.links[0].path == absolutePath(dataClassApi.show(dataModelId, targetDataClassId).path)
        !graph.items*.id.contains(sourceDataClassId)
    }

    void 'graph finds readable targets referring to every item in the source traversal without expanding them'() {
        given:
        DataClass additionalSource = dataClassApi.create(dataModelId,
            new DataClass(label: 'Additional unexpanded source'))
        DataComponent incomingFlow = createDataFlowNetworkComponent(dataModelId, 'Graph incoming flow',
            ['dc:DataFlow source'], ['dc:DataFlow target'])
        DataComponent referencingComponent = createDataFlowNetworkComponent(dataModelId, 'References reached source',
            ['dc:DataFlow source', 'dc:Additional unexpanded source'], [])

        when:
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataClass', targetDataClassId, ['DataFlow'])

        then:
        graph.items*.id == [targetDataClassId, incomingFlow.id, sourceDataClassId, referencingComponent.id]
        graph.links.size() == 3
        graph.links*.sourceId == [incomingFlow.id, sourceDataClassId, sourceDataClassId]
        graph.links*.targetId == [targetDataClassId, incomingFlow.id, referencingComponent.id]
        !graph.items*.id.contains(additionalSource.id)
        !graph.links*.sourceId.contains(additionalSource.id)
    }

    void 'incoming link traversal requires at least one supported kind'() {
        when:
        itemLinkGraphApi.traverseIncomingLinks('DataClass', targetDataClassId, [])

        then:
        HttpClientResponseException emptySelection = thrown()
        emptySelection.status == HttpStatus.BAD_REQUEST

        when:
        itemLinkGraphApi.traverseIncomingLinks('DataClass', targetDataClassId, ['UnknownLink'])

        then:
        HttpClientResponseException unsupported = thrown()
        unsupported.status == HttpStatus.BAD_REQUEST
    }

    void 'incoming link traversal retains unresolved paths without source identity'() {
        given:
        String unresolvedPath = 'dm:Missing link model$1.0.0|dc:Missing link source'
        dataFlowNetworkApi.create('DataComponent', dataComponentId, new DataFlowSourceCreateDTO(path: unresolvedPath))

        when:
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataComponent', dataComponentId, ['DataFlow'])

        then:
        graph.items*.id == [dataComponentId]
        graph.links.size() == 1
        graph.links[0].sourceId == null
        graph.links[0].sourceDomainType == null
        graph.links[0].targetId == dataComponentId
        graph.links[0].path == unresolvedPath
    }

    void 'incoming link traversal terminates cycles while retaining every link'() {
        given:
        DataComponent firstFlow = createDataFlowNetworkComponent(dataModelId, 'Cycle flow one',
            ['dc:DataFlow source'], ['dc:DataFlow target'])
        DataComponent secondFlow = createDataFlowNetworkComponent(dataModelId, 'Cycle flow two',
            ['dc:DataFlow target'], ['dc:DataFlow source'])

        when:
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataClass', targetDataClassId, ['DataFlow'])

        then:
        graph.items*.id == [targetDataClassId, firstFlow.id, secondFlow.id, sourceDataClassId]
        graph.links.size() == 4
        graph.links*.targetId == [targetDataClassId, secondFlow.id, firstFlow.id, sourceDataClassId]
        graph.links*.sourceId == [firstFlow.id, targetDataClassId, sourceDataClassId, secondFlow.id]
    }

    void 'incoming absolute links include finalized owning model version details'() {
        given:
        UUID sourceFolderId = folderApi.create(folder('Finalised data flow source')).id
        DataModel sourceModel = dataModelApi.create(sourceFolderId, dataModelPayload('Finalised source model'))
        DataClass source = dataClassApi.create(sourceModel.id, dataClassPayload('Finalised source class'))
        dataModelApi.finalise(sourceModel.id, new FinaliseData(version: ModelVersion.from('2.3.4')))
        source = dataClassApi.show(sourceModel.id, source.id)
        dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: source.path.toString()))

        when:
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataComponent', dataComponentId, ['DataFlow'])

        then:
        graph.items*.id == [dataComponentId, source.id]
        graph.items[1].owningModel.id == sourceModel.id
        graph.items[1].owningModel.label == 'Finalised source model'
        graph.items[1].owningModel.finalised
        graph.items[1].owningModel.modelVersion.toString() == '2.3.4'
        graph.links[0].path == absolutePath(source.path)
    }

    void 'relative graph paths resolve from the version owning versioned folder'() {
        given:
        Folder versionedFolder = versionedFolderApi.create(folder('Graph versioned folder'))
        DataModel sourceModel = dataModelApi.create(versionedFolder.id, dataModelPayload('Graph source database'))
        DataModel targetModel = dataModelApi.create(versionedFolder.id, dataModelPayload('Graph target database'))
        DataClass sourceTable = dataClassApi.create(sourceModel.id, dataClassPayload('Graph source table'))
        DataClass targetTable = dataClassApi.create(targetModel.id, dataClassPayload('Graph target table'))
        DataType sourceType = dataTypeApi.create(sourceModel.id, primitiveDataType('Graph source key type'))
        DataType targetType = dataTypeApi.create(targetModel.id, primitiveDataType('Graph target key type'))
        DataElement sourceColumn = dataElementApi.create(sourceModel.id, sourceTable.id,
            new DataElement(label: 'Graph source key', dataType: new DataType(id: sourceType.id)))
        DataElement targetColumn = dataElementApi.create(targetModel.id, targetTable.id,
            new DataElement(label: 'Graph target key', dataType: new DataType(id: targetType.id)))
        String relativePath = pathRelativeToVersionedFolder(sourceColumn.path.toString())
        DataComponent flow = createDataFlowNetworkComponent(targetModel.id, 'Cross-model flow',
            [relativePath], ['dc:Graph target table|de:Graph target key'])

        when:
        ItemLinkGraphDTO graph = itemLinkGraphApi.traverseIncomingLinks('DataElement', targetColumn.id, ['DataFlow'])

        then:
        graph.items*.id == [targetColumn.id, flow.id, sourceColumn.id]
        graph.items*.owningModel*.id == [versionedFolder.id, versionedFolder.id, versionedFolder.id]
        graph.links*.path == [absolutePath(flow.path), absolutePath(sourceColumn.path)]
        graph.links[0].sourceId == flow.id
        graph.links[0].targetId == targetColumn.id
    }

    void 'data data flow paths survive data model JSON export and import'() {
        given:
        DataComponent flow = createDataFlowNetworkComponent(dataModelId, 'Importable flow',
            ['dc:DataFlow source'], ['dc:DataFlow target'])
        HttpResponse<byte[]> exported = dataModelApi.exportModel(dataModelId,
            'org.maurodata.plugin.exporter.json', 'JsonDataModelExporterPlugin', '4.0.0')
        ExportModel exportModel = objectMapper.readValue(exported.body(), ExportModel)
        exportModel.dataModel.removeIds()
        MultipartBody request = MultipartBody.builder()
            .addPart('folderId', folderId.toString())
            .addPart('importFile', 'data-model.json', MediaType.APPLICATION_JSON_TYPE,
                objectMapper.writeValueAsBytes(exportModel))
            .build()

        when:
        ListResponse<DataModel> imported = dataModelApi.importModel(request,
            'org.maurodata.plugin.importer.json', 'JsonDataModelImporterPlugin', '4.0.0')
        DataModel importedModel = imported.items.first()
        DataClass importedTarget = dataClassApi.allDataClasses(importedModel.id).items
            .find { it.label == 'DataFlow target' }
        DataComponent importedFlow = dataFlowNetworkApi.listDataFlowNetworkComponents(importedModel.id).items
            .find { it.label == 'Importable flow' }

        then:
        imported.count == 1
        dataFlowNetworkApi.list('DataClass', importedTarget.id).items*.path == [flow.path.localPath().toString()]
        dataFlowNetworkApi.list('DataComponent', importedFlow.id).items*.path == ['dc:DataFlow source']
    }

    void 'same model absolute path is stored relative and survives a branch clone'() {
        given:
        DataClass source = dataClassApi.show(dataModelId, sourceDataClassId)
        DataClass nestedClass = dataClassApi.create(dataModelId, targetDataClassId,
            new DataClass(label: 'Nested lineage class', description: 'Nested class description'))
        DataType sourceDataType = dataTypeApi.create(dataModelId,
            new DataType(label: 'Lineage string type', description: 'String type description',
                dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))
        DataElement targetDataElement = dataElementApi.create(dataModelId, targetDataClassId,
            new DataElement(label: 'Nested lineage element', description: 'Element description',
                dataType: new DataType(id: sourceDataType.id)))

        when:
        DataComponent process = createDataFlowNetworkComponent(dataModelId, 'Cloneable process',
            [source.path.toString()], ['dc:DataFlow target', 'dc:DataFlow target|de:Nested lineage element'])
        ItemLinkDTO created = dataFlowNetworkApi.list('DataComponent', process.id).items.first()
        ItemLinkDTO elementLineage = dataFlowNetworkApi.list('DataElement', targetDataElement.id).items.first()

        then:
        created.path == 'dc:DataFlow source'
        elementLineage.path == process.path.localPath().toString()

        when:
        dataModelApi.finalise(dataModelId, new FinaliseData(version: ModelVersion.from('1.0.0')))
        DataModel branch = dataModelApi.createNewBranchModelVersion(dataModelId, new CreateNewVersionData(branchName: 'main'))
        List<DataClass> clonedClasses = dataClassApi.allDataClasses(branch.id).items
        DataClass clonedTarget = clonedClasses.find { it.label == 'DataFlow target' }
        DataClass clonedNestedClass = dataClassApi.list(branch.id, clonedTarget.id).items.find { it.label == nestedClass.label }
        List<ItemLinkDTO> clonedLineage = dataFlowNetworkApi.list('DataClass', clonedTarget.id).items
        DataComponent clonedProcess = dataFlowNetworkApi.listDataFlowNetworkComponents(branch.id).items
            .find { it.label == 'Cloneable process' }
        assert clonedProcess
        List<ItemLinkDTO> clonedProcessLineage = dataFlowNetworkApi.list('DataComponent', clonedProcess.id).items
        List<DataElement> clonedElements = dataElementApi.list(branch.id, clonedTarget.id).items
        DataElement clonedTargetElement = clonedElements.find { it.label == targetDataElement.label }
        List<ItemLinkDTO> clonedElementLineage = dataFlowNetworkApi.list('DataElement', clonedTargetElement.id).items
        DataType clonedDataType = dataTypeApi.list(branch.id).items.find { it.label == sourceDataType.label }

        then:
        branch.id != dataModelId
        clonedClasses*.label as Set == ['DataFlow target', 'DataFlow source', nestedClass.label] as Set
        clonedClasses*.id.toSet().disjoint([targetDataClassId, sourceDataClassId, nestedClass.id] as Set)
        clonedTarget.id != targetDataClassId
        clonedNestedClass.id != nestedClass.id
        clonedNestedClass.parentDataClass.id == clonedTarget.id
        clonedNestedClass.description == nestedClass.description
        clonedLineage.size() == 1
        clonedLineage[0].path == clonedProcess.path.localPath().toString()
        clonedProcess.id != process.id
        clonedProcessLineage.size() == 1
        clonedProcessLineage[0].id != created.id
        clonedProcessLineage[0].path == 'dc:DataFlow source'
        clonedElements.size() == 1
        clonedTargetElement.id != targetDataElement.id
        clonedTargetElement.description == targetDataElement.description
        clonedTargetElement.dataType.id == clonedDataType.id
        clonedDataType.id != sourceDataType.id
        clonedDataType.description == sourceDataType.description
        clonedElementLineage.size() == 1
        clonedElementLineage[0].id != elementLineage.id
        clonedElementLineage[0].path == clonedProcess.path.localPath().toString()
    }

    void 'unresolved absolute paths remain stored and direct self lineage is rejected'() {
        when:
        ItemLinkDTO unresolved = dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: 'fo:Ignored folder|dm:Missing model$1.0.0|dc:Missing source'))

        then:
        unresolved.path == 'dm:Missing model$1.0.0|dc:Missing source'
        dataFlowNetworkApi.list('DataComponent', dataComponentId).items*.id.contains(unresolved.id)

        when:
        DataComponent component = dataFlowNetworkApi.showDataFlowNetworkComponent(dataModelId, dataComponentId)
        dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: component.path.localPath().toString()))

        then:
        thrown(HttpClientResponseException)
    }

    void 'absolute path to a different unfinalised model is rejected'() {
        given:
        dataModelApi.create(folderId, dataModelPayload('Other draft model'))

        when:
        dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: 'dm:Other draft model$main|dc:Source'))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    void 'multiple visible finalised models matching an absolute path are rejected'() {
        given:
        UUID firstFolderId = folderApi.create(folder('First source folder')).id
        UUID secondFolderId = folderApi.create(folder('Second source folder')).id
        DataModel firstModel = dataModelApi.create(firstFolderId, dataModelPayload('Same source model'))
        DataModel secondModel = dataModelApi.create(secondFolderId, dataModelPayload('Same source model'))
        dataModelApi.finalise(firstModel.id, new FinaliseData(version: ModelVersion.from('1.0.0')))
        dataModelApi.finalise(secondModel.id, new FinaliseData(version: ModelVersion.from('1.0.0')))

        when:
        dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: 'dm:Same source model$1.0.0|dc:Source'))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    void 'finalised target model allows lineage reads but not creates or deletes'() {
        given:
        ItemLinkDTO existing = dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: 'dc:DataFlow source'))
        dataModelApi.finalise(dataModelId, new FinaliseData(version: ModelVersion.from('1.0.0')))

        when:
        ItemLinkDTO shown = dataFlowNetworkApi.show('DataComponent', dataComponentId, existing.id)
        ListResponse<ItemLinkDTO> listed = dataFlowNetworkApi.list('DataComponent', dataComponentId)

        then:
        shown.path == existing.path
        listed.items*.id == [existing.id]

        when:
        dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: 'dc:DataFlow source'))

        then:
        HttpClientResponseException createException = thrown()
        createException.status == HttpStatus.UNPROCESSABLE_ENTITY

        when:
        dataFlowNetworkApi.delete('DataComponent', dataComponentId, existing.id)

        then:
        HttpClientResponseException deleteException = thrown()
        deleteException.status == HttpStatus.UNPROCESSABLE_ENTITY
    }

    void 'service scopes a lineage facet to its owning item'() {
        given:
        ItemLinkDTO existing = dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: 'dc:DataFlow source'))
        UUID otherDataClassId = dataClassApi.create(dataModelId, dataClassPayload('Other class')).id
        assert otherDataClassId != targetDataClassId
        assert dataFlowNetworkApi.list('DataClass', otherDataClassId).items.empty
        DataClass otherDataClass = dataClassApi.show(dataModelId, otherDataClassId)

        when:
        dataFlowNetworkService.show(otherDataClass, existing.id)

        then:
        HttpStatusException serviceException = thrown()
        serviceException.status == HttpStatus.NOT_FOUND

        when:
        ItemLinkDTO shown = dataFlowNetworkApi.show('DataComponent', dataComponentId, existing.id)

        then:
        shown.id == existing.id
        dataFlowNetworkApi.list('DataComponent', dataComponentId).items*.id.contains(existing.id)
    }

    void 'finalised versioned folder is one path model candidate despite containing models'() {
        given:
        Folder sourceVersionedFolder = versionedFolderApi.create(folder('Versioned data flow source'))
        DataModel sourceModel = dataModelApi.create(sourceVersionedFolder.id, dataModelPayload('Versioned lineage model'))
        DataClass sourceClass = dataClassApi.create(sourceModel.id, dataClassPayload('Versioned data flow source class'))
        DataType sourceType = dataTypeApi.create(sourceModel.id, primitiveDataType('Versioned source type'))
        DataElement sourceElement = dataElementApi.create(sourceModel.id, sourceClass.id,
            new DataElement(label: 'Versioned source element', dataType: new DataType(id: sourceType.id)))
        versionedFolderApi.finalise(sourceVersionedFolder.id,
            new FinaliseData(version: ModelVersion.from('2.0.0')))
        String absoluteSourcePath = dataClassApi.show(sourceModel.id, sourceClass.id).path.toString()
        String absoluteSourceElementPath = dataElementApi.show(sourceModel.id, sourceClass.id, sourceElement.id).path.toString()

        when:
        ItemLinkDTO created = dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: absoluteSourcePath))
        ItemLinkDTO createdElementSource = dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: absoluteSourceElementPath))

        then:
        created.path == absoluteSourcePath
        createdElementSource.path == absoluteSourceElementPath
        dataFlowNetworkApi.list('DataComponent', dataComponentId).items*.path as Set ==
            ([absoluteSourcePath, absoluteSourceElementPath] as Set)
    }

    void 'relative lineage resolves between data elements in different data models in a versioned folder'() {
        given:
        Folder versionedFolder = versionedFolderApi.create(folder('Database lineage'))
        DataModel sourceModel = dataModelApi.create(versionedFolder.id, dataModelPayload('Source database'))
        DataModel targetModel = dataModelApi.create(versionedFolder.id, dataModelPayload('Target database'))
        DataClass sourceTable = dataClassApi.create(sourceModel.id, dataClassPayload('Source table'))
        DataClass targetTable = dataClassApi.create(targetModel.id, dataClassPayload('Target table'))
        DataType sourceType = dataTypeApi.create(sourceModel.id, primitiveDataType('Source key type'))
        DataType targetType = dataTypeApi.create(targetModel.id, primitiveDataType('Target key type'))
        DataElement sourceColumn = dataElementApi.create(sourceModel.id, sourceTable.id,
            new DataElement(label: 'Source key', dataType: new DataType(id: sourceType.id)))
        DataElement targetColumn = dataElementApi.create(targetModel.id, targetTable.id,
            new DataElement(label: 'Target foreign key', dataType: new DataType(id: targetType.id)))
        String relativeSourcePath = pathRelativeToVersionedFolder(sourceColumn.path.toString())

        when:
        DataComponent flow = createDataFlowNetworkComponent(targetModel.id, 'Database column flow',
            [relativeSourcePath], ['dc:Target table|de:Target foreign key'])
        ItemLinkDTO lineage = dataFlowNetworkApi.list('DataElement', targetColumn.id).items.first()
        ItemLinkDTO flowInput = dataFlowNetworkApi.list('DataComponent', flow.id).items.first()
        DataComponent resolvedFlow = (DataComponent) pathApi.getResourceByPathFromResource(
            'DataElement', targetColumn.id, lineage.path)
        DataElement resolvedSource = (DataElement) pathApi.getResourceByPathFromResource(
            'DataComponent', flow.id, flowInput.path)

        then:
        flowInput.path == relativeSourcePath
        !flowInput.path.contains('$')
        resolvedFlow.id == flow.id
        resolvedSource.id == sourceColumn.id
        sourceColumn.path.toString().startsWith("vf:${versionedFolder.label}\$")
    }

    void 'relative lineage resolves between data elements in different data classes in one versioned model'() {
        given:
        Folder versionedFolder = versionedFolderApi.create(folder('Joined table lineage'))
        DataModel model = dataModelApi.create(versionedFolder.id, dataModelPayload('Joined database'))
        DataClass sourceTable = dataClassApi.create(model.id, dataClassPayload('Key values table'))
        DataClass targetTable = dataClassApi.create(model.id, dataClassPayload('Fact table'))
        DataType sourceType = dataTypeApi.create(model.id, primitiveDataType('Key value type'))
        DataType targetType = dataTypeApi.create(model.id, primitiveDataType('Foreign key type'))
        DataElement sourceColumn = dataElementApi.create(model.id, sourceTable.id,
            new DataElement(label: 'Key', dataType: new DataType(id: sourceType.id)))
        DataElement targetColumn = dataElementApi.create(model.id, targetTable.id,
            new DataElement(label: 'Key reference', dataType: new DataType(id: targetType.id)))
        String relativeSourcePath = pathRelativeToVersionedFolder(sourceColumn.path.toString())

        when:
        DataComponent flow = createDataFlowNetworkComponent(model.id, 'Joined table flow',
            [relativeSourcePath], ['dc:Fact table|de:Key reference'])
        ItemLinkDTO lineage = dataFlowNetworkApi.list('DataElement', targetColumn.id).items.first()
        ItemLinkDTO flowInput = dataFlowNetworkApi.list('DataComponent', flow.id).items.first()
        DataElement resolvedSource = (DataElement) pathApi.getResourceByPathFromResource(
            'DataComponent', flow.id, flowInput.path)
        DataComponent resolvedFlow = (DataComponent) pathApi.getResourceByPathFromResource(
            'DataElement', targetColumn.id, lineage.path)

        then:
        flowInput.path == relativeSourcePath
        !flowInput.path.contains('$')
        resolvedSource.id == sourceColumn.id
        resolvedFlow.id == flow.id
        sourceColumn.path.toString() != targetColumn.path.toString()
        sourceColumn.path.toString().startsWith("vf:${versionedFolder.label}\$")
    }

    private static DataType primitiveDataType(String label) {
        new DataType(label: label, dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)
    }

    private DataComponent createDataFlowNetworkComponent(UUID modelId, String label, List<String> consumes, List<String> produces) {
        dataFlowNetworkApi.createDataFlowNetworkComponent(modelId, new DataComponentCreateDTO(
            label: label,
            consumes: consumes.collect {String path -> new DataFlowSourceCreateDTO(path: path)},
            produces: produces.collect {String path -> new DataFlowSourceCreateDTO(path: path)}
        ))
    }

    private static String absolutePath(Path path) {
        new Path([path.modelRootNode] + path.localPath().nodes).toString()
    }

    private static String pathRelativeToVersionedFolder(String path) {
        Path absolutePath = new Path(path)
        assert absolutePath.isAbsolute()
        assert absolutePath.nodes.first().prefix == 'vf'
        new Path(absolutePath.nodes.drop(1)).toString()
    }

    void 'cyclic lineage definitions are allowed'() {
        when:
        DataComponent targetToSource = createDataFlowNetworkComponent(dataModelId, 'Target to source',
            ['dc:DataFlow target'], ['dc:DataFlow source'])
        DataComponent sourceToTarget = createDataFlowNetworkComponent(dataModelId, 'Source to target',
            ['dc:DataFlow source'], ['dc:DataFlow target'])

        then:
        targetToSource.id
        sourceToTarget.id
        dataFlowNetworkApi.list('DataClass', targetDataClassId).count == 1
        dataFlowNetworkApi.list('DataClass', sourceDataClassId).count == 1
    }

    void 'absolute model version and another model branch with the same identifier are ambiguous'() {
        given:
        UUID sourceFolderId = folderApi.create(folder()).id
        DataModel sourceModel = dataModelApi.create(sourceFolderId, dataModelPayload('External source model'))
        dataModelApi.finalise(sourceModel.id, new FinaliseData(version: ModelVersion.from('1.0.0')))
        String absolutePath = 'dm:External source model$1.0.0|dc:Source'

        when:
        ItemLinkDTO stored = dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: absolutePath))
        DataModel draftBranch = dataModelApi.createNewBranchModelVersion(sourceModel.id,
            new CreateNewVersionData(branchName: '1.0.0'))

        then:
        stored.path == absolutePath
        draftBranch.branchName == '1.0.0'

        when:
        dataFlowNetworkApi.create('DataComponent', dataComponentId,
            new DataFlowSourceCreateDTO(path: absolutePath))

        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY
        dataFlowNetworkApi.list('DataComponent', dataComponentId).items*.path.contains(absolutePath)
    }
}
