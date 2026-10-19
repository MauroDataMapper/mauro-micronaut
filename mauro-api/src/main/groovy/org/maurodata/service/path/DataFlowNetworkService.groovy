package org.maurodata.service.path

import org.maurodata.api.facet.DataFlowSourceCreateDTO

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.Path
import org.maurodata.domain.facet.ItemLink
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataComponent
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.api.facet.DataComponentCreateDTO
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.domain.security.Role
import org.maurodata.security.AccessControlService
import io.micronaut.transaction.annotation.Transactional

@CompileStatic
@Singleton
class DataFlowNetworkService {
    private static final String DATA_FLOW_LINK_TYPE = DataComponent.DATA_FLOW_TYPE

    @Inject PathRepository pathRepository
    @Inject AccessControlService accessControlService
    @Inject VisiblePathModelResolver visiblePathModelResolver
    @Inject FacetCacheableRepository.ItemLinkCacheableRepository itemLinkRepository
    @Inject AdministeredItemCacheableRepository.DataComponentCacheableRepository dataComponentRepository
    @Inject ModelCacheableRepository.DataModelCacheableRepository dataModelRepository
    @Inject PathPrefixTypeLookup pathPrefixTypeLookup
    @Inject ContentsService contentsService

    List<ItemLink> list(AdministeredItem target) {
        itemLinkRepository.readAllByMultiFacetAwareItemIdAndType(target.id, DATA_FLOW_LINK_TYPE) as List<ItemLink>
    }

    ItemLink create(AdministeredItem target, String pathString) {
        Path path = normaliseAndValidate(pathString, target)
        if (list(target).any { it.path?.toString() == path.toString() }) {
            throw new HttpStatusException(HttpStatus.CONFLICT, 'This data flow path already exists on the item')
        }
        itemLinkRepository.save(new ItemLink(type: DATA_FLOW_LINK_TYPE, path: path).tap {
            multiFacetAwareItem = target
            multiFacetAwareItemId = target.id
            multiFacetAwareItemDomainType = target.domainType
            updateCreationProperties()
            catalogueUser = accessControlService.getUser()
        })
    }

    List<DataComponent> listDataFlowNetworkComponents(UUID dataModelId) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        if (!dataModel) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'DataModel not found')
        accessControlService.checkRole(Role.READER, dataModel)
        List<DataComponent> components = (dataComponentRepository.readAllByDataModel(dataModel) ?: [])
            .findAll { DataComponent component -> component.type == DataComponent.DATA_FLOW_TYPE }
        components.each { DataComponent component ->
            pathRepository.readParentItems(component)
            component.updatePath()
            component.updateBreadcrumbs()
        }
        components
    }

    @Transactional
    DataComponent createDataFlowNetworkComponent(UUID dataModelId, DataComponentCreateDTO request) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        if (!dataModel) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'DataModel not found')
        accessControlService.checkRole(Role.EDITOR, dataModel)
        if (!request?.label?.trim()) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'A DataFlow component label is required')
        }

        pathRepository.readParentItems(dataModel)
        dataModel.updatePath()
        Model owningModel = dataModel.owner
        if (!owningModel || owningModel.finalised) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'The DataModel must have an editable version-owning Model')
        }

        DataComponent candidate = new DataComponent(type: DataComponent.DATA_FLOW_TYPE, label: request.label.trim())
        if (dataComponentRepository.findAllByParentAndPathIdentifier(dataModel.id, candidate.pathIdentifier)) {
            throw new HttpStatusException(HttpStatus.CONFLICT, 'A DataFlow component with this type and label already exists in the DataModel')
        }

        DataComponent component = dataComponentRepository.save(candidate.tap {
            parent = dataModel
            updateCreationProperties()
            catalogueUser = accessControlService.getUser()
        })
        pathRepository.readParentItems(component)
        component.updatePath()

        (request.consumes ?: [] as List<DataFlowSourceCreateDTO>).each {consume ->
            if (!consume?.path?.trim()) {
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Each consumes entry requires a path')
            }
            create(component, consume.path)
        }

        (request.produces ?: [] as List<DataFlowSourceCreateDTO>).each { produce ->
            AdministeredItem outputTarget = resolveProducedTarget(dataModel, produce?.path)
            accessControlService.checkRole(Role.EDITOR, outputTarget)
            create(outputTarget, component.path.localPath().toString())
        }
        component
    }

    DataComponent showDataFlowNetworkComponent(UUID dataModelId, UUID id) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        if (!dataModel) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'DataModel not found')
        DataComponent component = dataComponentRepository.readById(id)
        ensureComponentOwner(component, dataModel)
        accessControlService.checkRole(Role.READER, component)
        pathRepository.readParentItems(component)
        component.updatePath()
        component
    }

    @Transactional
    boolean deleteDataFlowNetworkComponent(UUID dataModelId, UUID id) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        if (!dataModel) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'DataModel not found')
        DataComponent component = dataComponentRepository.loadWithContent(id)
        ensureComponentOwner(component, dataModel)
        pathRepository.readParentItems(component)
        accessControlService.checkRole(Role.EDITOR, component)
        ensureTargetModelEditable(component)
        contentsService.deleteWithContent(component)
    }

    ItemLink show(AdministeredItem target, UUID id) {
        ItemLink itemLink = itemLinkRepository.readById(id)
        ensureOwner(itemLink, target)
        itemLink
    }

    boolean delete(AdministeredItem target, UUID id) {
        ItemLink itemLink = itemLinkRepository.readById(id)
        ensureOwner(itemLink, target)
        ensureTargetModelEditable(target)
        itemLinkRepository.delete(itemLink) > 0
    }

    Path normaliseAndValidate(String pathString, AdministeredItem target) {
        if (pathString == null || pathString.trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'A data flow path is required')
        }

        Path path
        try {
            path = Path.fromPathRootedAtFirstModelIdentifier(pathString)
        } catch (RuntimeException e) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid data flow path: ${e.message}")
        }
        boolean absolute = path.isAbsolute()

        Model owningModel = ensureTargetModelEditable(target)
        Path.PathNode targetRoot = owningModel.path?.modelRootNode

        if (absolute && targetRoot && path.modelRootNode.isSameModelRoot(targetRoot)) {
            path = new Path(path.nodes.drop(1))
        } else if (absolute) {
            validateAbsoluteRoot(path.nodes[0])
        }

        if (!path.nodes) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'A data flow path must identify a source path')
        }

        String sourceDomainType = pathPrefixTypeLookup.getDomainType(path.lastPathNode()?.prefix)
        if (!(sourceDomainType in ['DataClass', 'DataElement', 'DataComponent'])) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'DataFlow sources must be DataClasses, DataElements or DataFlow components')
        }
        if (!(target.domainType in ['DataClass', 'DataElement', 'DataComponent'])) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'DataFlow targets must be DataClasses, DataElements or DataFlow components')
        }
        if (target instanceof DataComponent && target.type != DataComponent.DATA_FLOW_TYPE) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'The DataComponent target must have type DataFlow')
        }
        if (!(target instanceof DataComponent) && sourceDomainType != 'DataComponent') {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'A DataFlow link must have a DataFlow component at one end')
        }

        // Reject direct self-lineage when the relative or absolute path currently resolves.
        AdministeredItem resolved = resolveFromModel(owningModel, path, !absolute)
        if (resolved && !(resolved instanceof DataClass || resolved instanceof DataElement || resolved instanceof DataComponent)) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'DataFlow sources must be DataClasses, DataElements or DataFlow components')
        }
        if (resolved instanceof DataComponent) {
            if (resolved.type != DataComponent.DATA_FLOW_TYPE) {
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'The DataComponent source must have type DataFlow')
            }
            DataModel sourceDataModel = owningDataModel(resolved)
            DataModel targetDataModel = owningDataModel(target)
            if (sourceDataModel && targetDataModel && sourceDataModel.id != targetDataModel.id) {
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    'A DataFlow component source and its DataFlow target must belong to the same DataModel')
            }
        }
        if (resolved?.id == target.id && resolved.domainType == target.domainType) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'An item cannot be a source of its own lineage')
        }
        path
    }

    private AdministeredItem resolveProducedTarget(DataModel dataModel, String pathString) {
        if (!pathString?.trim()) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Each produces entry requires a relative target path')
        }
        Path relativePath
        try {
            relativePath = Path.fromPathRootedAtFirstModelIdentifier(pathString)
        } catch (RuntimeException e) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid produces path: ${e.message}")
        }
        if (relativePath.isAbsolute() || !relativePath.nodes) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Produces paths must be relative to the component DataModel')
        }
        String targetDomainType = pathPrefixTypeLookup.getDomainType(relativePath.lastPathNode()?.prefix)
        if (!(targetDomainType in ['DataClass', 'DataElement', 'DataComponent'])) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'DataFlow targets must be DataClasses, DataElements or DataFlow components')
        }
        pathRepository.readParentItems(dataModel)
        dataModel.updatePath()
        Path basePath = dataModel.path
        Path.PathNode dataModelNode = basePath?.lastPathNode()
        if (!dataModelNode) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'The DataModel has no path')
        Path fullPath = new Path(([dataModelNode] + relativePath.nodes) as List<Path.PathNode>)
        AdministeredItem resolved = pathRepository.findResourcesByPathFromRootResource(dataModel, fullPath)
        if (!resolved) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'A produces target path did not resolve in the component DataModel')
        if (resolved instanceof DataComponent && resolved.type != DataComponent.DATA_FLOW_TYPE) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'A produces DataComponent target must have type DataFlow')
        }
        pathRepository.readParentItems(resolved)
        if (owningDataModel(resolved)?.id != dataModel.id) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Produces targets must belong to the component DataModel')
        }
        resolved
    }

    private static DataModel owningDataModel(AdministeredItem item) {
        if (item instanceof DataClass) return ((DataClass) item).dataModel
        if (item instanceof DataElement) return ((DataElement) item).dataModel
        if (item instanceof DataComponent) return ((DataComponent) item).dataModel
        null
    }

    private static void ensureComponentOwner(DataComponent component, DataModel dataModel) {
        if (!component || component.type != DataComponent.DATA_FLOW_TYPE || component.dataModel?.id != dataModel.id) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'DataComponent not found in the supplied DataModel')
        }
    }

    private void validateAbsoluteRoot(Path.PathNode root) {
        PathModelCandidates candidates = visiblePathModelResolver.findVisibleModelCandidatesForPathRoot(root)
        if (candidates.draftModels) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'An absolute data flow path cannot identify an unfinalised Model')
        }
        if (candidates.finalisedModels.size() > 1) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'The absolute data flow path identifies more than one visible finalised Model')
        }
        // A path to a missing source is allowed; if a matching Model is visible, it must be finalised.
    }

    private AdministeredItem resolveFromModel(Model model, Path path, boolean relative) {
        Path resolvedPath = path
        if (relative) {
            pathRepository.readParentItems(model)
            model.updatePath()
            Path.PathNode root = model.path?.modelRootNode
            if (!root) return null
            resolvedPath = new Path(([root] + path.nodes) as List<Path.PathNode>)
        } else {
            PathModelCandidates candidates = visiblePathModelResolver.findVisibleModelCandidatesForPathRoot(path.nodes[0])
            if (candidates.finalisedModels.size() != 1) return null
            model = candidates.finalisedModels[0]
        }
        pathRepository.findResourcesByPathFromRootResource(model, resolvedPath)
    }

    private static void ensureOwner(ItemLink itemLink, AdministeredItem target) {
        if (!itemLink || itemLink.type != DATA_FLOW_LINK_TYPE || itemLink.multiFacetAwareItemId != target.id ||
            !itemLink.multiFacetAwareItemDomainType.equalsIgnoreCase(target.domainType)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'DataFlow not found for the supplied item')
        }
    }

    private Model ensureTargetModelEditable(AdministeredItem target) {
        pathRepository.readParentItems(target)
        target.updatePath()
        Model owningModel = target.owner
        if (!owningModel) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'The target item has no version-owning Model')
        }
        pathRepository.readParentItems(owningModel)
        owningModel.updatePath()
        if (owningModel.finalised) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'The target model must not be finalised')
        }
        owningModel
    }
}
