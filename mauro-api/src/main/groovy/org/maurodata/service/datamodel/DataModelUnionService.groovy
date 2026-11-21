package org.maurodata.service.datamodel

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import org.maurodata.api.datamodel.DataModelUnionRequestDTO
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.Path
import org.maurodata.domain.model.ModelPathMatch
import org.maurodata.domain.security.Role
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.security.AccessControlService

/** Builds a new model containing only the selected models' structure and labels. */
@CompileStatic
@Singleton
class DataModelUnionService {
    private final RepositoryService repositoryService
    private final ContentsService contentsService
    private final PathRepository pathRepository
    private final DataModelCacheableRepository dataModelRepository
    private final FolderCacheableRepository folderRepository
    private final AccessControlService accessControlService

    DataModelUnionService(RepositoryService repositoryService, ContentsService contentsService,
                          PathRepository pathRepository, DataModelCacheableRepository dataModelRepository,
                          FolderCacheableRepository folderRepository, AccessControlService accessControlService) {
        this.repositoryService = repositoryService
        this.contentsService = contentsService
        this.pathRepository = pathRepository
        this.dataModelRepository = dataModelRepository
        this.folderRepository = folderRepository
        this.accessControlService = accessControlService
    }

    DataModel build(DataModelUnionRequestDTO request) {
        Selection selection = select(request)
        DataModel union = new DataModel(label: request.label?.trim() ?: 'DataModel union')
        Map<String, DataClass> classByStructure = [:]
        IdentityHashMap<DataElement, Set<String>> sourceTypeLabelsByUnionElement = new IdentityHashMap<>()
        selection.models.each { DataModel source ->
            DataModel withContent = contentsService.loadWithContent(source)
            withContent.setAssociations()
            withContent.dataClasses?.each { DataClass dataClass ->
                addClass(dataClass, null, union, classByStructure, sourceTypeLabelsByUnionElement)
            }
        }
        applyStubDataTypes(union, sourceTypeLabelsByUnionElement)
        union
    }

    List<Map<String, String>> membership(DataModelUnionRequestDTO request) {
        select(request).models.collect { DataModel model -> identity(model) }
            .sort { Map<String, String> left, Map<String, String> right ->
                (left.label <=> right.label) ?: ((left.modelVersion ?: left.branchName) <=> (right.modelVersion ?: right.branchName))
            }
    }

    private Selection select(DataModelUnionRequestDTO request) {
        if (!request?.rootDomainType || !request.rootId) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'rootDomainType and rootId are required')
        }
        AdministeredItem root = repositoryService.getAdministeredItemRepository(request.rootDomainType)?.readById(request.rootId) as AdministeredItem
        if (!root) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Selection root not found')
        accessControlService.checkRole(Role.READER, root)
        pathRepository.readParentItems(root)
        root.updatePath()

        List<DataModel> candidates = modelsWithin(root)
        List<DataModel> selected = candidates.findAll { DataModel model ->
            if (!accessControlService.canDoRole(Role.READER, model)) return false
            pathRepository.readParentItems(model)
            model.updatePath()
            ModelPathMatch match = request.modelPathMatch
            !match?.path?.nodes || model.path.matchesRelativeTo(root.path, match.path, true,
                match.matchPathSuffix, match.matchFirstNodeIdentifierSuffix)
        }
        if (!selected) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'No visible DataModels matched the union selection')
        if (selected.size() > 1 && request.modelPathMatch && !request.modelPathMatch.allowMultipleModels) {
            throw new HttpStatusException(HttpStatus.CONFLICT, 'DataModel selection matched multiple models; allowMultipleModels must be enabled')
        }
        if (request.folderId) {
            Folder target = folderRepository.readById(request.folderId)
            if (!target) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Destination Folder not found')
            accessControlService.checkRole(Role.EDITOR, target)
        }
        new Selection(root: root, models: selected)
    }

    private List<DataModel> modelsWithin(AdministeredItem root) {
        if (root instanceof DataModel) return [(DataModel) root]
        if (!(root instanceof Folder)) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Selection root must be a Folder, VersionedFolder, or DataModel')
        }
        Set<UUID> folderIds = [root.id] as Set<UUID>
        Set<UUID> frontier = [root.id] as Set<UUID>
        while (frontier) {
            List<Folder> children = folderRepository.readAllByFolderIdIn(frontier)
            frontier = children*.id.findAll { UUID childId -> folderIds.add(childId) } as Set<UUID>
        }
        dataModelRepository.readAllByFolderIdIn(folderIds)
    }

    private static void addClass(DataClass source, DataClass parent, DataModel target,
                                 Map<String, DataClass> classByStructure,
                                 IdentityHashMap<DataElement, Set<String>> sourceTypeLabelsByUnionElement) {
        String key = parent ? parentKey(parent) + '|' + source.label : source.label
        DataClass destination = classByStructure[key]
        if (!destination) {
            destination = new DataClass(label: source.label, dataModel: target, parentDataClass: parent,
                dataClasses: [], dataElements: [])
            classByStructure.put(key, destination)
            if (parent) parent.dataClasses.add(destination)
            else target.dataClasses.add(destination)
        }
        source.dataElements?.each { DataElement element ->
            DataElement unionElement = destination.dataElements.find { DataElement existing -> existing.label == element.label }
            if (!unionElement) {
                unionElement = new DataElement(label: element.label, dataModel: target, dataClass: destination)
                destination.dataElements.add(unionElement)
            }
            if (!element.dataType?.label) {
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "DataElement '${element.label}' in DataClass '${source.label}' has no labelled DataType")
            }
            Set<String> labels = sourceTypeLabelsByUnionElement.get(unionElement)
            if (!labels) {
                labels = new TreeSet<String>()
                sourceTypeLabelsByUnionElement.put(unionElement, labels)
            }
            labels.add(element.dataType.label)
        }
        source.dataClasses?.each { DataClass child ->
            addClass(child, destination, target, classByStructure, sourceTypeLabelsByUnionElement)
        }
    }

    private static void applyStubDataTypes(DataModel target,
                                           IdentityHashMap<DataElement, Set<String>> sourceTypeLabelsByUnionElement) {
        sourceTypeLabelsByUnionElement.each { DataElement element, Set<String> sourceLabels ->
            String label = sourceLabels.join(' / ')
            DataType stub = target.dataTypes.find { DataType existing -> existing.label == label }
            if (!stub) {
                stub = new DataType(label: label, dataModel: target,
                    dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE)
                target.dataTypes.add(stub)
            }
            element.dataType = stub
        }
    }

    private static String parentKey(DataClass dataClass) {
        List<String> labels = []
        DataClass current = dataClass
        while (current) {
            labels.add(0, current.label)
            current = current.parentDataClass
        }
        labels.join('|')
    }

    private static Map<String, String> identity(DataModel model) {
        Map<String, String> result = [label: model.label]
        if (model.modelVersion != null) result.modelVersion = model.modelVersion.toString()
        else result.branchName = model.branchName ?: Model.DEFAULT_BRANCH_NAME
        result
    }

    private static class Selection {
        AdministeredItem root
        List<DataModel> models
    }
}
