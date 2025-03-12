package uk.ac.ox.softeng.mauro.controller.datamodel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Parameter
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.datamodel.*
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataElementRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataTypeContentRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.plugin.importer.DataModelImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

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

    @Get(Paths.DATA_MODEL_ID_ROUTE)
    DataModel show(UUID id) {
        super.show(id)
    }

    @Transactional
    @Post(Paths.FOLDER_LIST_DATA_MODEL)
    DataModel create(UUID folderId, @Body @NonNull DataModel dataModel) {
        super.create(folderId, dataModel) as DataModel
    }


    @Put(Paths.DATA_MODEL_ID_ROUTE)
    @Transactional
    DataModel update(UUID id, @Body @NonNull DataModel dataModel) {
        super.update(id, dataModel) as DataModel
    }

    @Transactional
    @Delete(Paths.DATA_MODEL_ID_ROUTE)
    HttpResponse delete(UUID id, @Body @Nullable DataModel dataModel) {
        super.delete(id, dataModel)
    }


    @Get(Paths.DATA_MODEL_SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(UUID id, @Parameter @Nullable SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        DataModel dataModel = dataModelRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        ListResponse.from(searchRepository.search(requestDTO))
    }

    @Post(Paths.DATA_MODEL_SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(UUID id, @Body SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        DataModel dataModel = dataModelRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        ListResponse.from(searchRepository.search(requestDTO))
    }


    @Get(Paths.FOLDER_LIST_DATA_MODEL)
    ListResponse<DataModel> list(UUID folderId) {
        super.list(folderId)
    }

    @Get(Paths.DATA_MODEL_ROUTE)
    ListResponse<DataModel> listAll() {
        super.listAll()
    }

    @Transactional
    @Put(Paths.DATA_MODEL_ID_FINALISE)
    DataModel finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Transactional
    @Put(Paths.DATA_MODEL_BRANCH_MODEL_VERSION)
    DataModel createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Get(Paths.DATA_MODEL_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModel(id, namespace, name, version)
    }

    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.DATA_MODEL_IMPORT)
    ListResponse<DataModel> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)
    }

    @Get(Paths.DATA_MODEL_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId) {
        DataModel dataModel = modelContentRepository.findWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataModel, "item with $id not found")
        DataModel otherDataModel = modelContentRepository.findWithContentById(otherId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataModel, "item with $otherId not found")

        accessControlService.checkRole(Role.READER, dataModel)
        accessControlService.checkRole(Role.READER, otherDataModel)

        dataModel.setAssociations()
        otherDataModel.setAssociations()
        dataModel.diff(otherDataModel)
    }

    @Get(Paths.DATA_MODEL_EXPORTERS)
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
    @Put(Paths.DATA_MODEL_SUBSET)
    SubsetData subset(UUID id, UUID otherId, @Body SubsetData subsetData) {
        DataModel dataModel = dataModelRepository.readById(id) // source i.e. rootDataModel
        accessControlService.canDoRole(Role.READER, dataModel)
        DataModel otherDataModel = dataModelContentRepository.findWithContentById(otherId) // target i.e. request model
        accessControlService.canDoRole(Role.EDITOR, otherDataModel)

        List<DataElement> additionDataElements = subsetData.additions.collect {dataElementCacheableRepository.findById(it)}
        additionDataElements.each {DataElement dataElement ->
            pathRepository.readParentItems(dataElement)
            if (dataElement.owner.id != dataModel.id) throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Subset DataElements must be within the source DataModel")
        }

        DataModel additionSubset = new DataModel()

        // copy missing DataElements and intermediate DataClasses into additionSubset
        additionDataElements.each {DataElement dataElement ->
            log.debug "subset: processing data element addition for id [$dataElement.id], label [$dataElement.label]"
            List<AdministeredItem> parents = pathRepository.readParentItems(dataElement)
            List<DataClass> dataClassParents = parents.takeWhile {it !instanceof Model}.tail().reverse() as List<DataClass>
            DataClass currentOtherModelOrAdditionParent = new DataClass(dataClasses: otherDataModel.dataClasses) // maintain as the copy of the parent of `DataClass child` in the otherDataModel
            dataClassParents.each {DataClass child ->
                DataClass otherModelOrAdditionChild = currentOtherModelOrAdditionParent?.dataClasses?.find {it.label == child.label} ?: additionSubset.dataClasses.find {it.id == child.id}
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
        dataModelContentRepository.saveContentOnly(additionSubset)

        new SubsetData(
            additions: additionSubset.dataElements.id,
            //deletions: [] TODO: implement deletion - not currently used by UI
        )
    }

    /**
     * Given a source DataModel, a list of source DataElement IDs and many target DataModels, give the intersection
     * (by path) of DataElements in the source and each target DataModel.
     * @param id of the source DataModel
     * @param intersectsManyData contains the source DataElement IDs and source DataModel IDs
     * @return {@link ListResponse} of {@link IntersectsData} containing the list of source DataElement IDs that intersected the target
     * DataModel
     */
    @Post(Paths.DATA_MODEL_INTERSECTS_MANY)
    ListResponse<IntersectsData> intersectsMany(UUID id, @Body IntersectsManyData intersectsManyData) {
        DataModel sourceDataModel = dataModelRepository.readById(id)
        accessControlService.canDoRole(Role.READER, sourceDataModel)
        List<DataModel> targetDataModels = intersectsManyData.targetDataModelIds.collect {dataModelRepository.readById(it)}
        targetDataModels.each {DataModel dataModel -> accessControlService.canDoRole(Role.READER, dataModel)}
        List<DataElement> dataElements = intersectsManyData.dataElementIds.collect {dataElementCacheableRepository.readById(it)}
        dataElements.each {DataElement dataElement ->
            pathRepository.readParentItems(dataElement)
            if (dataElement.owner.id != sourceDataModel.id) throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Intersection DataElements must be within the source DataModel")
        }

        Map<UUID, List<DataElement>> targetDataModelsDataElementsMap = targetDataModels.collectEntries {[it.id, dataElementRepository.readAllByDataModelId(it.id)]}

        Map<UUID, List<DataElement>> potentialTargetDataModelsIntersects = targetDataModelsDataElementsMap.collectEntries {UUID targetDataModelId, List<DataElement> targetDataElements ->
            [targetDataModelId, targetDataElements.findAll {dataElements.label.contains(it.label)}]
        }

        Map<UUID, List<DataElement>> dataElementsIntersects = potentialTargetDataModelsIntersects.collectEntries {UUID targetDataModelId, List<DataElement> targetDataElements ->
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
                potentialTargetIntersects.find {intersect.breadcrumbs.reverse().tail().collect {new Tuple2(it.domainType, it.label)} == it.breadcrumbs.reverse().tail().collect {new Tuple2(it.domainType, it.label)}}
            }]
        }

        ListResponse.from(dataElementsIntersects.collect {UUID targetDataModelId, List<DataElement> intersects ->
            new IntersectsData(sourceDataModelId: sourceDataModel.id, targetDataModelId: targetDataModelId, intersects: intersects.id)
        })
    }

    // TODO: implement stub endpoint
    @Get('/dataModels/{id}/simpleModelVersionTree')
    List<Map> simpleModelVersionTree(UUID id) {
        [
            [
                id         : id,
                branch     : 'main',
                displayName: 'main'
            ]
        ] as List<Map>
    }
}
