package org.maurodata.controller.datamodel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Parameter
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.datamodel.DataModelApi
import org.maurodata.api.model.MergeDiffDTO
import org.maurodata.api.model.MergeIntoDTO
import org.maurodata.api.model.ModelVersionDTO
import org.maurodata.api.model.ModelVersionedRefDTO
import org.maurodata.api.model.ModelVersionedWithTargetsRefDTO
import org.maurodata.api.model.PermissionsDTO
import org.maurodata.api.model.VersionLinkDTO
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ModelController
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataModelService
import org.maurodata.domain.datamodel.DataModelType
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.IntersectsData
import org.maurodata.domain.datamodel.IntersectsManyData
import org.maurodata.domain.datamodel.SubsetData
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.datamodel.DataElementRepository
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.persistence.datamodel.DataTypeContentRepository
import org.maurodata.persistence.search.SearchRepository
import org.maurodata.plugin.datatype.DefaultDataTypeProviderPlugin
import org.maurodata.plugin.exporter.DataModelExporterPlugin
import org.maurodata.plugin.importer.DataModelImporterPlugin
import org.maurodata.service.plugin.PluginService
import org.maurodata.web.ListResponse

@Slf4j
@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class DataModelController extends ModelController<DataModel> implements DataModelApi {
    DataModelCacheableRepository dataModelRepository

    DataModelContentRepository dataModelContentRepository

    @Inject
    SearchRepository searchRepository

    @Inject
    DataModelService dataModelService

    @Inject
    DataClassCacheableRepository dataClassRepository

    @Inject
    DataElementCacheableRepository dataElementCacheableRepository

    @Inject
    DataTypeCacheableRepository dataTypeCacheableRepository

    @Inject
    DataTypeContentRepository dataTypeContentRepository

    @Inject
    DataElementRepository dataElementRepository

    DataModelController(DataModelCacheableRepository dataModelRepository, FolderCacheableRepository folderRepository, DataModelContentRepository dataModelContentRepository,
                        DataModelService dataModelService) {
        super(DataModel, dataModelRepository, folderRepository, dataModelContentRepository, dataModelService)
        this.dataModelRepository = dataModelRepository
        this.dataModelContentRepository = dataModelContentRepository
        this.dataModelService = dataModelService
    }

    @Audit
    @Get(Paths.DATA_MODEL_ID_ROUTE)
    DataModel show(UUID id) {
        super.show(id)
    }

    @Audit
    @Transactional
    @Post(Paths.FOLDER_LIST_DATA_MODEL)
    DataModel create(UUID folderId, @Body @NonNull DataModel dataModel, @Nullable @QueryValue String defaultDataTypeProvider = null) {
        // First try and get the default datatypes if applicable
        List<DataType> importedDataTypes = []
        if(defaultDataTypeProvider) {
            DefaultDataTypeProviderPlugin defaultDataTypeProviderPlugin = mauroPluginService.getPlugin(DefaultDataTypeProviderPlugin, defaultDataTypeProvider)
            PluginService.handlePluginNotFound(defaultDataTypeProviderPlugin, DefaultDataTypeProviderPlugin, defaultDataTypeProvider)
            importedDataTypes.addAll(defaultDataTypeProviderPlugin.dataTypes)
        }
        DataModel newDataModel = super.create(folderId, dataModel) as DataModel
        // If we previously got datatypes, now save them into the model
        importedDataTypes.each {
            it.dataModel = newDataModel
        }
        dataTypeCacheableRepository.saveAll(importedDataTypes)

        return newDataModel
    }

    @Audit
    @Put(Paths.DATA_MODEL_ID_ROUTE)
    @Transactional
    DataModel update(UUID id, @Body @NonNull DataModel dataModel) {
        super.update(id, dataModel) as DataModel
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Transactional
    @Delete(Paths.DATA_MODEL_ID_ROUTE)
    HttpResponse delete(UUID id, @Body @Nullable DataModel dataModel, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, dataModel, permanent)
    }


    @Audit
    @Get(Paths.DATA_MODEL_SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(UUID id, @Parameter @Nullable SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        DataModel dataModel = dataModelRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        ListResponse.from(searchRepository.search(requestDTO))
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.DATA_MODEL_SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(UUID id, @Body SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        DataModel dataModel = dataModelRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        ListResponse.from(searchRepository.search(requestDTO))
    }


    @Audit
    @Get(Paths.FOLDER_LIST_DATA_MODEL)
    ListResponse<DataModel> list(UUID folderId) {
        super.list(folderId)
    }

    @Audit
    @Get(Paths.DATA_MODEL_ROUTE)
    ListResponse<DataModel> listAll() {
        super.listAll()
    }

    @Audit(title = EditType.FINALISE, description = "Finalise data model")
    @Transactional
    @Put(Paths.DATA_MODEL_ID_FINALISE)
    DataModel finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Transactional
    @Audit(title = EditType.COPY, description = "New version of data model")
    @Put(Paths.DATA_MODEL_BRANCH_MODEL_VERSION)
    DataModel createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Audit
    @Get(Paths.DATA_MODEL_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModel(id, namespace, name, version)
    }

    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Audit(title = EditType.IMPORT, description = "Import data model")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.DATA_MODEL_IMPORT)
    ListResponse<DataModel> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)
    }

    @Audit
    @Get(Paths.DATA_MODEL_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId) {
        DataModel dataModel = (DataModel) contentsService.loadWithContent(modelRepository.readById(id))
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataModel, "item with $id not found")
        DataModel otherDataModel = (DataModel) contentsService.loadWithContent(modelRepository.readById(otherId))
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, otherDataModel, "item with $otherId not found")

        accessControlService.checkRole(Role.READER, dataModel)
        accessControlService.checkRole(Role.READER, otherDataModel)

        pathRepository.readParentItems(dataModel)
        dataModel.updatePath()

        pathRepository.readParentItems(otherDataModel)
        otherDataModel.updatePath()

        dataModel.diff(otherDataModel)
    }

    @Get(Paths.DATA_MODEL_IMPORTERS)
    List<DataModelImporterPlugin> dataModelImporters() {
        mauroPluginService.listPlugins(DataModelImporterPlugin)
    }

    /**
     * Copy a subset of DataElements and their DataType and containing DataClasses from a DataModel to a target DataModel.
     * @param id of the source DataModel
     * @param otherId of the target DataModel
     * @param subsetData a list of source DataElement IDs to be copied
     * @return the IDs of the new DataElements in the target DataModel
     */
    @Audit(title = EditType.UPDATE, description = "Subset data model")
    @Put(Paths.DATA_MODEL_SUBSET)
    DataModel subset(UUID id, UUID otherId, @Body SubsetData subsetData) {
        DataModel dataModel = dataModelRepository.readById(id)
        // source i.e. rootDataModel
        accessControlService.canDoRole(Role.READER, dataModel)
        DataModel otherDataModel = dataModelContentRepository.findWithContentById(otherId)
        // target i.e. request model
        accessControlService.canDoRole(Role.EDITOR, otherDataModel)

        List<DataElement> additionDataElements = subsetData.additions?.collect {dataElementCacheableRepository.findById(it)}
        additionDataElements?.each {DataElement dataElement ->
            pathRepository.readParentItems(dataElement)
            if (dataElement.owner.id != dataModel.id) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Subset DataElements for Addition must be within the source DataModel")
        }

        DataModel additionSubset = new DataModel()

        // copy missing DataElements and intermediate DataClasses into additionSubset
        additionDataElements?.each {DataElement dataElement ->
            log.debug "subset: processing data element addition for id [$dataElement.id], label [$dataElement.label]"
            List<AdministeredItem> parents = pathRepository.readParentItems(dataElement)
            List<DataClass> dataClassParents = parents.takeWhile {it !instanceof Model}.tail().reverse() as List<DataClass>
            DataClass currentOtherModelOrAdditionParent = new DataClass(dataClasses: otherDataModel.dataClasses)
            // maintain as the copy of the parent of `DataClass child` in the otherDataModel
            dataClassParents.each {DataClass child ->
                DataClass otherModelOrAdditionChild =
                    currentOtherModelOrAdditionParent?.dataClasses?.find {it.label == child.label} ?: additionSubset.dataClasses.find {it.id == child.id}
                if (otherModelOrAdditionChild) {
                    currentOtherModelOrAdditionParent = otherModelOrAdditionChild
                } else {
                    child.dataModel = otherDataModel
                    child.parent = currentOtherModelOrAdditionParent
                    additionSubset.dataClasses.add(child)
                    currentOtherModelOrAdditionParent = child
                }
            }

            if (!currentOtherModelOrAdditionParent.dataElements.label.contains(dataElement.label)) {
                dataElement.dataModel = otherDataModel
                dataElement.parent = currentOtherModelOrAdditionParent
                additionSubset.dataElements.add(dataElement)
            }
        }

        // copy missing DataTypes into additionSubset
        List<DataType> dataTypes = dataTypeContentRepository.findAllWithContentByParent(dataModel)
        Set<String> additionDataTypeLabels = (additionSubset.dataElements.dataType.label - otherDataModel.dataTypes.label) as Set
        dataTypes.findAll {additionDataTypeLabels.contains(it.label)}.each {DataType dataType ->
            dataType.dataModel = otherDataModel
            additionSubset.dataTypes.add(dataType)
        }
        Map<String, DataType> dataTypeMap = (additionSubset.dataTypes + otherDataModel.dataTypes).collectEntries {[it.label, it]}
        additionSubset.dataElements.each {DataElement dataElement ->
            dataElement.dataType = dataTypeMap[dataElement.dataType.label]
        }
        additionSubset.dataTypes = additionSubset.dataElements.dataType.unique()
        additionSubset.dataTypes.each {
            it.dataModel = otherDataModel
        }

        additionSubset.id = otherDataModel.id
        additionSubset.setAssociations()

        log.debug "subset: saving additions to datamodel id [$additionSubset.id]"
        contentsService.saveContentOnly(additionSubset)

        // process DataElements for deletion
        List<DataElement> deletionDataElements = subsetData.deletions?.collect {dataElementCacheableRepository.findById(it)}
        deletionDataElements?.each {DataElement dataElement ->
            pathRepository.readParentItems(dataElement)
            if (dataElement.owner.id != dataModel.id) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Subset DataElements for Deletion must be within the source DataModel")
        }

        otherDataModel = dataModelContentRepository.findWithContentById(otherId)

        deletionDataElements?.each {DataElement dataElement ->
            log.debug "subset: processing data element deletion for id [$dataElement.id], label [$dataElement.label]"
            List<AdministeredItem> parents = pathRepository.readParentItems(dataElement)
            List<DataClass> dataClassParents = parents.takeWhile {it !instanceof Model}.tail().reverse() as List<DataClass>
            DataClass currentOtherModelParent = new DataClass(dataClasses: otherDataModel.dataClasses)
            dataClassParents.each {DataClass child ->
                currentOtherModelParent = currentOtherModelParent?.dataClasses?.find {it.label == child.label}
            }

            DataElement targetDataElement = currentOtherModelParent.dataElements.find {it.label == dataElement.label}
            if (targetDataElement) {
                dataElementCacheableRepository.delete(targetDataElement)
            }
        }

        dataModelRepository.findById(otherId)
    }

    /**
     * Given a source DataModel, a list of source DataElement IDs and many target DataModels, give the intersection
     * (by path) of DataElements in the source and each target DataModel.
     * @param id of the source DataModel
     * @param intersectsManyData contains the source DataElement IDs and source DataModel IDs
     * @return {@link ListResponse} of {@link IntersectsData} containing the list of source DataElement IDs that intersected the target
     * DataModel
     */
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.DATA_MODEL_INTERSECTS_MANY)
    ListResponse<IntersectsData> intersectsMany(UUID id, @Body IntersectsManyData intersectsManyData) {
        DataModel sourceDataModel = dataModelRepository.readById(id)
        accessControlService.canDoRole(Role.READER, sourceDataModel)
        List<DataModel> targetDataModels = intersectsManyData.targetDataModelIds.collect {dataModelRepository.readById(it)}
        targetDataModels.each {DataModel dataModel -> accessControlService.canDoRole(Role.READER, dataModel)}
        List<DataElement> dataElements = intersectsManyData.dataElementIds.collect {dataElementCacheableRepository.readById(it)}
        dataElements.each {DataElement dataElement ->
            pathRepository.readParentItems(dataElement)
            if (dataElement.owner.id != sourceDataModel.id) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                                                                          "Intersection DataElements must be within the source DataModel")
        }

        Map<UUID, List<DataElement>> targetDataModelsDataElementsMap = targetDataModels.collectEntries {[it.id, dataElementRepository.readAllByDataModelId(it.id)]}

        Map<UUID, List<DataElement>> potentialTargetDataModelsIntersects =
            targetDataModelsDataElementsMap.collectEntries {UUID targetDataModelId, List<DataElement> targetDataElements ->
                [targetDataModelId, targetDataElements.findAll {dataElements.label.contains(it.label)}]
            }

        Map<UUID, List<DataElement>> dataElementsIntersects =
            potentialTargetDataModelsIntersects.collectEntries {UUID targetDataModelId, List<DataElement> targetDataElements ->
                List<DataElement> potentialIntersects = dataElements.findAll {targetDataElements.label.contains(it.label)}
                List<DataElement> potentialTargetIntersects = targetDataElements.findAll {potentialIntersects.label.contains(it.label)}

                potentialTargetIntersects.each {DataElement dataElement ->
                    pathRepository.readParentItems(dataElement)
                    dataElement.updateBreadcrumbs()
                }

                potentialIntersects.each {DataElement dataElement ->
                    if (!dataElement.breadcrumbs) {
                        pathRepository.readParentItems(dataElement)
                        dataElement.updateBreadcrumbs()
                    }
                }

                [targetDataModelId, potentialIntersects.findAll {DataElement intersect ->
                    potentialTargetIntersects
                        .find {intersect.breadcrumbs.tail().collect {new Tuple2(it.domainType, it.label)} == it.breadcrumbs.tail().collect {new Tuple2(it.domainType, it.
                            label)}}
                }]
            }

        ListResponse.from(dataElementsIntersects.collect {UUID targetDataModelId, List<DataElement> intersects ->
            new IntersectsData(sourceDataModelId: sourceDataModel.id, targetDataModelId: targetDataModelId, intersects: intersects.id)
        })
    }

    @Audit
    @Put(Paths.DATA_MODEL_READ_BY_AUTHENTICATED)
    @Transactional
    DataModel allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as DataModel
    }

    @Audit
    @Transactional
    @Delete(Paths.DATA_MODEL_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Put(Paths.DATA_MODEL_READ_BY_EVERYONE)
    @Transactional
    DataModel allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as DataModel
    }

    @Audit
    @Transactional
    @Delete(Paths.DATA_MODEL_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Override
    @Get(Paths.DATA_MODEL_VERSION_LINKS)
    ListResponse<VersionLinkDTO> listVersionLinks(UUID id) {

        final Model model = super.show(id)
        if (model == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found")
        }

        final List<VersionLinkDTO> versionList = new ArrayList<>()

        for (VersionLink versionLink : model.versionLinks) {
            versionList.add(super.constructVersionLinkDTO(model, versionLink))
        }

        return ListResponse.from(versionList)
    }

    @Override
    @Get(Paths.DATA_MODEL_SIMPLE_MODEL_VERSION_TREE)
    List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly) {
        super.simpleModelVersionTree(id, branchesOnly)
    }

    @Override
    @Get(Paths.DATA_MODEL_MODEL_VERSION_TREE)
    List<ModelVersionedWithTargetsRefDTO> modelVersionTree(UUID id) {
        super.modelVersionTree(id)
    }

    @Override
    @Get(Paths.DATA_MODEL_CURRENT_MAIN_BRANCH)
    DataModel currentMainBranch(UUID id) {
        super.currentMainBranch(id)
    }

    @Override
    @Get(Paths.DATA_MODEL_LATEST_MODEL_VERSION)
    ModelVersionDTO latestModelVersion(UUID id) {
        super.latestModelVersion(id)
    }

    @Override
    @Get(Paths.DATA_MODEL_LATEST_FINALISED_MODEL)
    ModelVersionedRefDTO latestFinalisedModel(UUID id) {
        super.latestFinalisedModel(id)
    }

    @Get(Paths.DATA_MODEL_COMMON_ANCESTOR)
    DataModel commonAncestor(UUID id, UUID other_model_id) {
        super.commonAncestor(id,other_model_id)
    }

    @Get(Paths.DATA_MODEL_MERGE_DIFF)
    MergeDiffDTO mergeDiff(@NonNull UUID id, @NonNull UUID otherId) {
        super.mergeDiff(id,otherId)
    }

    @Audit
    @Transactional
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Put(Paths.DATA_MODEL_MERGE_INTO)
    DataModel mergeInto(@NonNull UUID id, @NonNull UUID otherId, @Body @Nullable MergeIntoDTO mergeIntoDTO){
        super.mergeInto(id,otherId,mergeIntoDTO)
    }

    @Get(Paths.DATA_MODEL_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }

    @Get(Paths.DATA_MODEL_EXPORTERS)
    List<DataModelExporterPlugin> dataModelExporters() {
        mauroPluginService.listPlugins(DataModelExporterPlugin)
    }

    @Get(Paths.DATA_MODEL_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }

    @Override
    List<DefaultDataTypeProviderPlugin> defaultDataTypeProviders() {
        return mauroPluginService.listPlugins(DefaultDataTypeProviderPlugin)
    }

    @Override
    List<String> dataModelTypes() {
        return DataModelType.labels()
    }


    DataModel saveContentOnly(@NonNull DataModel model) {
        //List<Collection<AdministeredItem>> associations = model.getAllAssociations() as List<Collection<AdministeredItem>>

        model.allDataClasses.each {
            contentsService.saveWithContent(it)
        }
        model.dataTypes.each {
            contentsService.saveWithContent(it)
        }
        model.dataElements.each {
            contentsService.saveWithContent(it)
        }

/*        associations.each {association ->
            if (association) {
                //Collection<AdministeredItem> savedAssociation = getRepository(association.first()).saveAll((Collection<AdministeredItem>) association)
                association.each {
                }
            }
        }

        if(model.allDataClasses) {
            dataClassRepository.updateAll(model.allDataClasses.findAll { it.parentDataClass})
        }
        if(model.dataElements) {
            dataElementRepository.updateAll(model.dataElements)
        }
 */
        model
    }


}
