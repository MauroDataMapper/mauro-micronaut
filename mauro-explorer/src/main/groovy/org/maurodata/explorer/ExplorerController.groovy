package org.maurodata.explorer

import uk.ac.ox.softeng.mauro.controller.folder.FolderController
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemReader
import uk.ac.ox.softeng.mauro.controller.search.SearchController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Path
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.explorer.ExplorerRepository
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.security.AccessControlService
import uk.ac.ox.softeng.mauro.web.ListResponse

import com.fasterxml.jackson.annotation.JsonView
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject

@Slf4j
@CompileStatic
@Controller
@JsonView(DataModel.BackwardsCompatibleView)
@Secured(SecurityRule.IS_ANONYMOUS)
class ExplorerController implements AdministeredItemReader {

    @Inject
    FolderCacheableRepository folderCacheableRepository

    @Inject
    DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    DataModelContentRepository dataModelContentRepository

    @Inject
    DataClassCacheableRepository dataClassCacheableRepository

    @Inject
    DataElementCacheableRepository dataElementCacheableRepository

    @Inject
    AccessControlService accessControlService

    @Inject
    FolderController folderController

    @Inject
    SearchController searchController

    @Inject
    PathRepository pathRepository

    @Inject
    SearchRepository searchRepository

    @Inject
    ExplorerRepository explorerRepository

    @Transactional
    @Post('/explorer/userFolder')
    Folder userFolder() {
        final String DATA_SPECIFICATION_FOLDER_LABEL = 'Explorer User Data Specifications'

        List<Folder> rootFolders = folderCacheableRepository.readAllRootFolders()

        Folder dataSpecFolder = rootFolders.find {it.label == DATA_SPECIFICATION_FOLDER_LABEL}
        if (!dataSpecFolder) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Explorer User Data Specifications root folder not found')

        List<Folder> dataSpecFolders = folderCacheableRepository.readAllByFolder(dataSpecFolder)

        String userFolderLabel = accessControlService.user.emailAddress.replace("@", "[at]")

        Folder userFolder = dataSpecFolders.find {it.label == userFolderLabel}

        if (userFolder) {
            return userFolder
        } else {
            userFolder = new Folder(label: userFolderLabel)
            userFolder.folder = dataSpecFolder
            userFolder.catalogueUser = accessControlService.user
            folderCacheableRepository.save(userFolder)
        }
    }

    @Transactional
    @Get('/explorer/rootDataModel')
    DataModel rootDataModel() {
        final String ROOT_DATA_MODEL_PATH = 'fo:Explorer Root Data Model|dm:TVS SDE Gold Export'

        Path rootDataModelPath = new Path(ROOT_DATA_MODEL_PATH)
        if (rootDataModelPath.nodes.prefix != ['fo', 'dm']) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 'Root Data Model path must be within a top level Folder')
        }

        String folderLabel = rootDataModelPath.nodes[0].identifier
        String dataModelLabel = rootDataModelPath.nodes[1].identifier

        Folder rootFolder = folderCacheableRepository.readAllRootFolders().find {it.label == folderLabel}
        DataModel rootDataModel = dataModelCacheableRepository.readAllByFolder(rootFolder).find {it.label == dataModelLabel}

        accessControlService.canDoRole(Role.READER, rootDataModel)

        rootDataModel
    }

    @Post('/explorer/getRequiredCoreTableDataElementIds')
    Set<UUID> getRequiredCoreTableDataElementIds(@Body List<UUID> dataElementIds) {
        log.debug 'Getting rootDataModel...'
        DataModel rootDataModel = dataModelCacheableRepository.findById(rootDataModel().id)

        List<DataElement> dataElements = dataElementIds.collect {UUID id -> dataElementCacheableRepository.readById(id)}
        List<UUID> dataClassIds = dataElements.dataClass.id.unique()

        String coreSchemaAndTable = rootDataModel.metadata.find {it.namespace == 'uk.ac.ox.softeng.maurodatamapper.plugins.explorer.querybuilder' && it.key == 'queryBuilderCoreTable'}.value
        String[] schemaAndTableNames = coreSchemaAndTable.split('\\.')
        def (String coreSchemaName, String coreTableName) = [schemaAndTableNames[0], schemaAndTableNames[1]]
        log.debug "coreSchemaName: [$coreSchemaName], coreTableName: [$coreTableName]"
        DataClass coreSchema = dataClassCacheableRepository.readAllByDataModelAndParentDataClassIsNull(rootDataModel).find {it.label == coreSchemaName}
        DataClass coreTable = dataClassCacheableRepository.readAllByParentDataClass(coreSchema).find {it.label == coreTableName}

        UUID coreTablePrimaryKeyDataElementId = dataElementCacheableRepository.readAllByParent(coreTable).find {it.label == 'Warehouse_Identifier'}.id

        Set<UUID> requiredForeignKeyDataElementIds = explorerRepository.getIdsOfChildForeignKeyDataElements(dataClassIds, coreSchemaName, coreTableName)

        requiredForeignKeyDataElementIds.add(coreTablePrimaryKeyDataElementId)

        requiredForeignKeyDataElementIds
    }

    @Get('/explorer/getLatestModelDataSpecifications')
    ListResponse<DataModel> getLatestModelDataSpecifications() {
        Folder userFolder = userFolder()

        List<DataModel> dataSpecifications = dataModelCacheableRepository.readAllByFolder(userFolder)

        ListResponse.from(dataSpecifications)
    }

//    @Post('/dataModels/{dataModelId}/intersectsMany')
//    ListResponse<UUID> intersectsMany(UUID dataModelId, @Body Map body) {
//        ListResponse.from([])
//    }

    @Post('/dataModels/{dataModelId}/profiles/uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research/ResearchDataElementProfileProviderService/search')
    ListResponse dataModelResearchDataElementsSearch(UUID dataModelId, @Body SearchRequestDTO requestDTO) {
        DataModel dataModel = dataModelCacheableRepository.readById(dataModelId)
        accessControlService.checkRole(Role.READER, dataModel)

        String searchTerm = requestDTO.searchTerm == '*' ? '%' : requestDTO.searchTerm

        List<DataElement> elements = searchRepository.researchDataElementSearch(searchTerm, null, dataModelId)

        elements.each {
            pathRepository.readParentItems(it)
            it.updatePath()
            it.updateBreadcrumbs()
            it.profileFields = []
            it.dataModel = (DataModel) it.owner
        }

        ListResponse.from(elements)
    }

    @Post('/dataClasses/{dataClassId}/profiles/uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research/ResearchDataElementProfileProviderService/search')
    ListResponse dataClassResearchDataElementsSearch(UUID dataClassId, @Body SearchRequestDTO requestDTO) {
        DataClass dataClass = dataClassCacheableRepository.readById(dataClassId)
        accessControlService.checkRole(Role.READER, dataClass)

        String searchTerm = requestDTO.searchTerm == '*' ? '%' : requestDTO.searchTerm

        List<DataElement> elements = searchRepository.researchDataElementSearch(searchTerm, dataClassId, null)

        elements.each {
            pathRepository.readParentItems(it)
            it.updatePath()
            it.updateBreadcrumbs()
            it.profileFields = []
            it.dataModel = (DataModel) it.owner
        }

        ListResponse.from(elements)
    }

    @CompileDynamic
    @Get('/dataModels/{id}/hierarchy')
    DataModel hierarchy(UUID id) {
        DataModel dataModel = dataModelContentRepository.findWithContentById(id)
        //accessControlService.checkRole(Role.READER, dataModel)
        dataModel
    }

//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    @Get('/dataElements/{dataElementId}/profile/uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research/ResearchDataElementProfileProviderService')
//    Map dataElementResearchDataElementProfile(UUID dataElementId) {
//        DataElement dataElement = dataElementCacheableRepository.readById(dataElementId)
//        accessControlService.checkRole(Role.READER, dataElement)
//
//        [
//            domainType: 'DataElement',
//            id: dataElement.id,
//            label: dataElement.label,
//            sections: []
//        ]
//    }

//    @Post('/{domainType}/{domainId}/profiles/uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research/ResearchDataElementProfileProviderService/search')
//    ListResponse researchDataElementsSearch(String domainType, UUID domainId, @Body SearchRequestDTO requestDTO) {
//        AdministeredItem item = findAdministeredItem(domainType, domainId)
//
//        pathRepository.readParentItems(item)
//        Model model = item.owner
//        requestDTO.withinModelId = model.id
//
//        if (requestDTO.searchTerm == '*') {
//            requestDTO.prefixSearch = true
//            requestDTO.searchTerm = '%'
//        }
//
////        ListResponse<SearchResultsDTO> modelResults = searchController.searchPost(requestDTO)
////        modelResults.items.each {
////            it.model = model.id
////        }
////
////        List<DataElement> elements = (List<DataElement>) modelResults.items.collect {findAdministeredItem(it.domainType, it.id)}
//
//        List<DataElement> elements = searchRepository.researchDataElementSearch(requestDTO.searchTerm, domainId)
//
//        elements.each {
//            pathRepository.readParentItems(it)
//            it.updatePath()
//            it.updateBreadcrumbs()
//            it.profileFields = []
//        }
//
//        ListResponse.from(elements)
//
////        List test = [
////            [
////                id         : "f435a5ff-0b02-414c-a0de-b9e0f386a5bd",
////                domainType : "DataElement",
////                label      : "Active_Ind",
////                path       : 'dm:TVS SDE Gold$1.0.0|dc:Hospital_Events|dc:Inpatient_Encounter|de:Active_Ind',
////                model      : "1af97b1c-45fc-40f5-a1dd-340d372a3c90",
////                description: "test",
////                breadcrumbs: [
////                    [
////                        "id": "1af97b1c-45fc-40f5-a1dd-340d372a3c90",
////                        "label": "TVS SDE Gold",
////                        "domainType": "DataModel",
////                        "finalised": true
////                    ],
////                    [
////                        "id": "8a5d95f0-3a82-403d-bc3b-f278220a0abf",
////                        "label": "Hospital_Events",
////                        "domainType": "DataClass"
////                    ],
////                    [
////                        "id": "fcba5541-2c4e-4417-8463-c27541f0e140",
////                        "label": "Inpatient_Encounter",
////                        "domainType": "DataClass"
////                    ]
////                ],
////                profileFields: []
////            ]
////        ]
////
////        ListResponse.from(test)
//    }
}
