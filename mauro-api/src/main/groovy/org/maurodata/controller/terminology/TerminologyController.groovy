package org.maurodata.controller.terminology

import io.swagger.v3.oas.annotations.Operation
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
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
import io.micronaut.http.annotation.RequestBean
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
import org.maurodata.api.model.ModelVersionedRefDTO
import org.maurodata.api.model.PermissionsDTO
import org.maurodata.api.terminology.TerminologyApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ModelController
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.security.Role
import org.maurodata.domain.terminology.Terminology
import org.maurodata.domain.terminology.TerminologyService
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import org.maurodata.persistence.search.SearchRepository

import org.maurodata.plugin.exporter.TerminologyExporterPlugin
import org.maurodata.plugin.importer.TerminologyImporterPlugin
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@Slf4j
@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class TerminologyController extends ModelController<Terminology> implements TerminologyApi {

    TerminologyCacheableRepository terminologyRepository

    @Inject
    SearchRepository searchRepository

    @Inject
    TerminologyService terminologyService

    TerminologyController(TerminologyCacheableRepository terminologyRepository, FolderCacheableRepository folderRepository,
                          TerminologyService terminologyService) {
        super(Terminology, terminologyRepository, folderRepository, terminologyService)
        this.terminologyRepository = terminologyRepository
        this.terminologyService = terminologyService
    }

    @Audit
    @Operation(summary = "Get a terminology", description = "Returns a terminology.")
    @Get('/api/terminologies/undefined')
    Map showUndef() {
        [:]
    }

    @Audit
    @Operation(operationId = 'showTerminology', summary = "Get a terminology", description = "Returns a terminology.")
    @Get(Paths.TERMINOLOGY_ID)
    Terminology show(UUID id) {
        super.show(id)
    }

    @Audit
    @Transactional
    @Operation(operationId = 'createTerminology', summary = "Create a terminology", description = "Creates a terminology.")
    @Post(Paths.FOLDER_LIST_TERMINOLOGY)
    Terminology create(UUID folderId, @Body @NonNull Terminology terminology) {
        log.debug '*** TerminologyController.create ***'
        super.create(folderId, terminology)
    }

    @Audit
    @Operation(operationId = 'updateTerminology', summary = "Update a terminology", description = "Updates a terminology.")
    @Put(Paths.TERMINOLOGY_ID)
    Terminology update(UUID id, @Body @NonNull Terminology terminology) {
        super.update(id, terminology)
    }

    @Audit(deletedObjectDomainType = Terminology)
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteTerminology', summary = "Delete a terminology", description = "Deletes a terminology.")
    @Delete(Paths.TERMINOLOGY_ID)
    HttpResponse delete(UUID id, @Body @Nullable Terminology terminology, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, terminology, permanent)
    }

    @Audit
    @Operation(summary = "List the terminologies", description = "Returns the terminologies. You must have read privileges on the item in question.")
    @Get(Paths.TERMINOLOGY_SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(UUID id, @RequestBean SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        Terminology terminology = terminologyRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, terminology)
        List<SearchResultsDTO> searchResultsDTOs = searchRepository.search(requestDTO)
        searchResultsDTOs.each {result ->
            AdministeredItem item = findAdministeredItem(result.domainType, result.id)
            pathRepository.readParentItems(item)
            item.updateBreadcrumbs()
            result.breadcrumbs = item.breadcrumbs
        }
        ListResponse.from(searchResultsDTOs)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "List the terminologies", description = "Returns the terminologies. You must have read privileges on the item in question.")
    @Post(Paths.TERMINOLOGY_SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(UUID id, @Body SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        Terminology terminology = terminologyRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, terminology)
        List<SearchResultsDTO> searchResultsDTOs = searchRepository.search(requestDTO)
        searchResultsDTOs.each {result ->
            AdministeredItem item = findAdministeredItem(result.domainType, result.id)
            pathRepository.readParentItems(item)
            item.updateBreadcrumbs()
            result.breadcrumbs = item.breadcrumbs
        }
        ListResponse.from(searchResultsDTOs)
    }


    @Audit
    @Operation(operationId = 'listTerminologies', summary = "List the terminologies", description = "Returns the terminologies.")
    @Get(Paths.FOLDER_LIST_TERMINOLOGY_PAGED)
    ListResponse<Terminology> list(UUID folderId, @Nullable PaginationParams params = new PaginationParams()) {
        super.list(folderId, params)
    }

    @Audit
    @Operation(operationId = 'listAllTerminologyPaged', summary = "List the terminologies", description = "Returns the terminologies.")
    @Get(Paths.TERMINOLOGY_LIST_PAGED)
    ListResponse<Terminology> listAll(@Nullable PaginationParams params = new PaginationParams()) {
        
        super.listAll(params)
    }

    @Transactional
    @Audit(title = EditType.FINALISE, description = "Finalise Terminology")
    @Operation(operationId = 'finaliseTerminology', summary = "Update a terminology", description = "Updates a terminology.")
    @Put(Paths.TERMINOLOGY_FINALISE)
    Terminology finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Audit(title = EditType.COPY, description = "New Version of CodeSet")
    @Transactional
    @Operation(summary = "Update a terminology", description = "Updates a terminology.")
    @Put(Paths.TERMINOLOGY_NEW_BRANCH_MODEL_VERSION)
    Terminology createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Audit(title = EditType.EXPORT, description = 'Export terminology')
    @Operation(operationId = 'exportModelTerminology', summary = "Get a terminology", description = "Returns a terminology.")
    @Get(Paths.TERMINOLOGY_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModels(namespace, name, version, [id])
    }

    @Audit(title = EditType.EXPORT, description = 'Export terminologies')
    @Operation(summary = "Export the terminology", description = "Exports the terminology.")
    @Post(Paths.TERMINOLOGY_EXPORT_MANY)
    HttpResponse<byte[]> exportModels(@Nullable String namespace, @Nullable String name, @Nullable String version, @Body List<UUID> ids){
        super.exportModels(namespace, name, version, ids)
    }

    @Audit(title = EditType.IMPORT, description = "Import terminology")
    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(operationId = 'importModelTerminology', summary = "Import the terminology", description = "Imports the terminology.")
    @Post(Paths.TERMINOLOGY_IMPORT)
    ListResponse<Terminology> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)

    }

    /*
        @Transactional
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Operation(summary = "Import the terminology", description = "Imports the terminology.")
        @Post('/terminologies/import{/namespace}{/name}{/version}')
        ListResponse<Terminology> importModel(@Body Map<String, String> importMap, @Nullable String namespace, @Nullable String name, @Nullable String version) {
            log.info '** start importModel **'
            ExportModel importModel = objectMapper.readValue(importMap.importFile, ExportModel)
            log.info '*** imported JSON model ***'
            Terminology imported = importModel.terminology
            imported.setAssociations()
            imported.updateCreationProperties()
            log.info '* start updateCreationProperties *'
            imported.getAllContents().each {it.updateCreationProperties()}
            log.info '* finish updateCreationProperties *'

            UUID folderId = UUID.fromString(importMap.folderId)

            Folder folder = folderRepository.readById(folderId)
            imported.folder = folder
            log.info '** about to saveWithContentBatched... **'
            Terminology savedImported = modelContentRepository.saveWithContent(imported)
            log.info '** finished saveWithContentBatched **'
            ListResponse.from([show(savedImported.id)])
        }

     */

    @Audit
    @Operation(summary = "Get a terminology", description = "Returns a terminology. You must have read privileges on the item in question.")
    @Get(Paths.TERMINOLOGY_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId) {
        Terminology terminology = terminologyRepository.loadWithContent(id)

        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, terminology, "item not found : $id")
        Terminology other = terminologyRepository.loadWithContent(otherId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, terminology, "item not found : $otherId")

        accessControlService.checkRole(Role.READER, terminology)
        accessControlService.checkRole(Role.READER, other)

        terminology.setAssociations()
        other.setAssociations()

        pathRepository.readParentItems(terminology)
        terminology.updatePath()

        pathRepository.readParentItems(other)
        other.updatePath()

        terminology.diff(other)
    }

    @Audit
    @Operation(summary = "Update a terminology", description = "Updates a terminology.")
    @Put(Paths.TERMINOLOGY_READ_BY_AUTHENTICATED)
    @Transactional
    Terminology allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as Terminology
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a terminology", description = "Deletes a terminology.")
    @Delete(Paths.TERMINOLOGY_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Operation(summary = "Update a terminology", description = "Updates a terminology.")
    @Put(Paths.TERMINOLOGY_READ_BY_EVERYONE)
    @Transactional
    Terminology allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as Terminology
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a terminology", description = "Deletes a terminology.")
    @Delete(Paths.TERMINOLOGY_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Operation(summary = "List the terminologies", description = "Returns the terminologies.")
    @Get(Paths.TERMINOLOGY_LIST_IMPORTERS)
    List<TerminologyImporterPlugin> terminologyImporters() {
        mauroPluginService.listPlugins(TerminologyImporterPlugin)
    }

    @Operation(summary = "List the terminologies", description = "Returns the terminologies.")
    @Get(Paths.TERMINOLOGY_LIST_EXPORTERS)
    List<TerminologyExporterPlugin> terminologyExporters() {
        mauroPluginService.listPlugins(TerminologyExporterPlugin)
    }

    @Operation(summary = "List the terminologies", description = "Returns the terminologies.")
    @Get(Paths.TERMINOLOGY_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }

    @Operation(summary = "Get a terminology", description = "Returns a terminology.")
    @Get(Paths.TERMINOLOGY_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }

    @Override
    @Operation(summary = "List the terminologies", description = "Returns the terminologies.")
    @Get(Paths.TERMINOLOGY_SIMPLE_MODEL_VERSION_TREE)
    List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly) {

        branchesOnly = branchesOnly ?: false

        final ArrayList<Model> allModels = populateVersionTree(id, branchesOnly, null)

        // Create object DTOs

        final ArrayList<ModelVersionedRefDTO> simpleModelVersionTreeList = new ArrayList<>(allModels.size())

        for (Model model : allModels) {
            final ModelVersionedRefDTO modelVersionedRefDTO = new ModelVersionedRefDTO(model)
            simpleModelVersionTreeList.add(modelVersionedRefDTO)
        }

        return simpleModelVersionTreeList
    }

    @Audit(description = 'Move folder')
    @Transactional
    @Put(Paths.TERMINOLOGY_MOVE)
    Terminology moveFolder(UUID id, String destination) {
        super.moveFolder(id, destination)
    }

}
