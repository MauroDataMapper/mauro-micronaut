package org.maurodata.controller.datamodel

import io.swagger.v3.oas.annotations.Operation
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
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
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

import org.maurodata.persistence.search.SearchRepository
import org.maurodata.plugin.datatype.DefaultDataTypeProviderPlugin
import org.maurodata.plugin.exporter.DataModelExporterPlugin
import org.maurodata.plugin.importer.DataModelImporterPlugin
import org.maurodata.service.plugin.PluginService
import org.maurodata.shredder.ShreddedContent
import org.maurodata.web.ListResponse

@Slf4j
@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class DataModelController extends ModelController<DataModel> implements DataModelApi {

    DataModelCacheableRepository dataModelRepository

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
    DataElementRepository dataElementRepository

    DataModelController(DataModelCacheableRepository dataModelRepository, FolderCacheableRepository folderRepository,
                        DataModelService dataModelService) {
        super(DataModel, dataModelRepository, folderRepository, dataModelService)
        this.dataModelRepository = dataModelRepository
        this.dataModelService = dataModelService
    }

    @Audit
    @Operation(operationId = 'showDataModel', summary = "Get a data model", description = "Returns a data model.")
    @Get(Paths.DATA_MODEL_ID_ROUTE)
    DataModel show(UUID id) {
        super.show(id)
    }

    @Audit
    @Transactional
    @Operation(operationId = 'createDataModel', summary = "Create a data model", description = "Creates a data model.")
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
    @Operation(operationId = 'updateDataModel', summary = "Update a data model", description = "Updates a data model.")
    @Put(Paths.DATA_MODEL_ID_ROUTE)
    @Transactional
    DataModel update(UUID id, @Body @NonNull DataModel dataModel) {
        super.update(id, dataModel) as DataModel
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteDataModel', summary = "Delete a data model", description = "Deletes a data model.")
    @Delete(Paths.DATA_MODEL_ID_ROUTE)
    HttpResponse delete(UUID id, @Body @Nullable DataModel dataModel, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, dataModel, permanent)
    }


    @Audit
    @Operation(summary = "List the data models", description = "Returns the data models. You must have read privileges on the item in question.")
    @Get(Paths.DATA_MODEL_SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(UUID id, @Parameter @Nullable SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        DataModel dataModel = dataModelRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        ListResponse.from(searchRepository.search(requestDTO))
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "List the data models", description = "Returns the data models. You must have read privileges on the item in question.")
    @Post(Paths.DATA_MODEL_SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(UUID id, @Body SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        DataModel dataModel = dataModelRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        ListResponse.from(searchRepository.search(requestDTO))
    }


    @Audit
    @Operation(summary = "List the data models", description = "Returns the data models.")
    @Get(Paths.FOLDER_LIST_DATA_MODEL)
    ListResponse<DataModel> list(UUID folderId) {
        super.list(folderId)
    }

    @Audit
    @Operation(operationId = 'listAllDataModel', summary = "List the data models", description = "Returns the data models.")
    @Get(Paths.DATA_MODEL_ROUTE)
    ListResponse<DataModel> listAll() {
        super.listAll()
    }

    @Audit(title = EditType.FINALISE, description = "Finalise data model")
    @Transactional
    @Operation(operationId = 'finaliseDataModel', summary = "Update a data model", description = "Updates a data model.")
    @Put(Paths.DATA_MODEL_ID_FINALISE)
    DataModel finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Transactional
    @Audit(title = EditType.COPY, description = "New version of data model")
    @Operation(summary = "Update a data model", description = "Updates a data model.")
    @Put(Paths.DATA_MODEL_BRANCH_MODEL_VERSION)
    DataModel createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Audit(title = EditType.EXPORT, description = 'Export data model')
    @Operation(operationId = 'exportModelDataModel', summary = "Get a data model", description = "Returns a data model.")
    @Get(Paths.DATA_MODEL_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModels(namespace, name, version, [id])
    }

    @Audit(title = EditType.EXPORT, description = 'Export data models')
    @Operation(summary = "Export the data model", description = "Exports the data model.")
    @Post(value = Paths.DATA_MODEL_EXPORT_MANY, produces = MediaType.ALL)
    HttpResponse<byte[]> exportModels(@Nullable String namespace, @Nullable String name, @Nullable String version, @Body List<UUID> ids) {
        super.exportModels(namespace, name, version, ids)
    }


    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Audit(title = EditType.IMPORT, description = "Import data model")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(operationId = 'importModelDataModel', summary = "Import the data model", description = "Imports the data model.")
    @Post(Paths.DATA_MODEL_IMPORT)
    ListResponse<DataModel> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)
    }

    @Audit
    @Operation(summary = "Get a data model", description = "Returns a data model. You must have read privileges on the item in question.")
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

    @Operation(summary = "List the data models", description = "Returns the data models.")
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
    @Operation(summary = "Update a data model", description = "Updates a data model. You must have read or edit privileges on the item in question, depending on the action.")
    @Put(Paths.DATA_MODEL_SUBSET)
    DataModel subset(UUID id, UUID otherId, @Body SubsetData subsetData) {
        DataModel dataModel = dataModelRepository.readById(id)
        // source i.e. rootDataModel
        accessControlService.canDoRole(Role.READER, dataModel)
        DataModel otherDataModel = dataModelRepository.loadWithContent(otherId)
        // target i.e. request model
        accessControlService.canDoRole(Role.EDITOR, otherDataModel)

        if(subsetData.additions) {
            List<DataElement> additionDataElements = dataElementCacheableRepository.findAllByIdIn(subsetData.additions)

            ShreddedContent shreddedContent = new ShreddedContent()
            additionDataElements?.each {DataElement dataElement ->
                log.debug "subset: processing data element addition for id [$dataElement.id], label [$dataElement.label]"
                // Do an initial check that we're not trying to add DataElements from a different DataModel
                List<AdministeredItem> parents = pathRepository.readParentItems(dataElement)
                if (dataElement.owner.id != dataModel.id) {
                    throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Subset DataElements for Addition must be within the source DataModel")
                }
                dataElement.updatePath()
                dataElement.dataType = dataTypeCacheableRepository.loadWithContent(dataElement.dataType.id)
                otherDataModel.addDataElementAtPath(dataElement, dataElement.path, shreddedContent)
            }
            shreddedContent.unsetIdentifiers()
            contentsService.saveShreddedContent(shreddedContent)
        }
        if(subsetData.deletions) {
            // process DataElements for deletion
            List<DataElement> deletionDataElements = subsetData.deletions.collect {dataElementCacheableRepository.findById(it)}
            deletionDataElements.each {DataElement dataElement ->
                pathRepository.readParentItems(dataElement)
                if (dataElement.owner.id != dataModel.id) throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Subset DataElements for Deletion must be within the source DataModel")
            }

            otherDataModel = dataModelRepository.loadWithContent(otherId)

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
        }

        return (DataModel) dataModelRepository.findById(otherId)
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
    @Operation(summary = "Create a data model", description = "Creates a data model. You must have read privileges on the item in question.")
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
    @Operation(summary = "Update a data model", description = "Updates a data model.")
    @Put(Paths.DATA_MODEL_READ_BY_AUTHENTICATED)
    @Transactional
    DataModel allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as DataModel
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a data model", description = "Deletes a data model.")
    @Delete(Paths.DATA_MODEL_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Operation(summary = "Update a data model", description = "Updates a data model.")
    @Put(Paths.DATA_MODEL_READ_BY_EVERYONE)
    @Transactional
    DataModel allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as DataModel
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a data model", description = "Deletes a data model.")
    @Delete(Paths.DATA_MODEL_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Override
    @Operation(summary = "List the data models", description = "Returns the data models.")
    @Get(Paths.DATA_MODEL_SIMPLE_MODEL_VERSION_TREE)
    List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly) {
        super.simpleModelVersionTree(id, branchesOnly)
    }

    @Override
    @Operation(summary = "List the data models", description = "Returns the data models.")
    @Get(Paths.DATA_MODEL_MODEL_VERSION_TREE)
    List<ModelVersionedWithTargetsRefDTO> modelVersionTree(UUID id) {
        super.modelVersionTree(id)
    }

    @Override
    @Operation(summary = "Get a data model", description = "Returns a data model.")
    @Get(Paths.DATA_MODEL_CURRENT_MAIN_BRANCH)
    DataModel currentMainBranch(UUID id) {
        super.currentMainBranch(id)
    }

    @Override
    @Operation(summary = "Get a data model", description = "Returns a data model.")
    @Get(Paths.DATA_MODEL_LATEST_MODEL_VERSION)
    ModelVersionDTO latestModelVersion(UUID id) {
        super.latestModelVersion(id)
    }

    @Override
    @Operation(summary = "Get a data model", description = "Returns a data model.")
    @Get(Paths.DATA_MODEL_LATEST_FINALISED_MODEL)
    ModelVersionedRefDTO latestFinalisedModel(UUID id) {
        super.latestFinalisedModel(id)
    }

    @Operation(summary = "Get a data model", description = "Returns a data model.")
    @Get(Paths.DATA_MODEL_COMMON_ANCESTOR)
    DataModel commonAncestor(UUID id, UUID other_model_id) {
        super.commonAncestor(id,other_model_id)
    }

    @Operation(summary = "Get a data model", description = "Returns a data model.")
    @Get(Paths.DATA_MODEL_MERGE_DIFF)
    MergeDiffDTO mergeDiff(@NonNull UUID id, @NonNull UUID otherId) {
        super.mergeDiff(id,otherId)
    }

    @Audit
    @Transactional
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Operation(summary = "Update a data model", description = "Updates a data model.")
    @Put(Paths.DATA_MODEL_MERGE_INTO)
    DataModel mergeInto(@NonNull UUID id, @NonNull UUID otherId, @Body @Nullable MergeIntoDTO mergeIntoDTO){
        super.mergeInto(id,otherId,mergeIntoDTO)
    }

    @Operation(summary = "List the data models", description = "Returns the data models.")
    @Get(Paths.DATA_MODEL_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }

    @Operation(summary = "List the data models", description = "Returns the data models.")
    @Get(Paths.DATA_MODEL_EXPORTERS)
    List<DataModelExporterPlugin> dataModelExporters() {
        mauroPluginService.listPlugins(DataModelExporterPlugin)
    }

    @Operation(summary = "Get a data model", description = "Returns a data model.")
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

    @Audit(description = 'Move folder')
    @Transactional
    @Put(Paths.DATA_MODEL_MOVE)
    DataModel moveFolder(UUID id, String destination) {
        super.moveFolder(id, destination)
    }

}
