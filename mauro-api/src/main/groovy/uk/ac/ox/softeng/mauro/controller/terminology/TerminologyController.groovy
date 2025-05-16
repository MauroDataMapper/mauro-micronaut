package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.domain.facet.EditType

import io.micronaut.http.HttpStatus
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.terminology.TerminologyApi
import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.plugin.exporter.DataModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.exporter.TerminologyExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.TerminologyImporterPlugin

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.domain.terminology.TerminologyService
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class TerminologyController extends ModelController<Terminology> implements TerminologyApi {

    TerminologyCacheableRepository terminologyRepository
    @Inject
    TerminologyContentRepository terminologyContentRepository

    @Inject
    SearchRepository searchRepository

    @Inject
    TerminologyService terminologyService

    TerminologyController(TerminologyCacheableRepository terminologyRepository, FolderCacheableRepository folderRepository, TerminologyContentRepository terminologyContentRepository,
    TerminologyService terminologyService) {
        super(Terminology, terminologyRepository, folderRepository, terminologyContentRepository, terminologyService)
        this.terminologyRepository = terminologyRepository
        this.terminologyContentRepository = terminologyContentRepository
        this.terminologyService = terminologyService
    }

    @Audit
    @Get(Paths.TERMINOLOGY_ID)
    Terminology show(UUID id) {
        super.show(id)
    }

    @Audit
    @Transactional
    @Post(Paths.FOLDER_LIST_TERMINOLOGY)
    Terminology create(UUID folderId, @Body @NonNull Terminology terminology) {
        log.debug '*** TerminologyController.create ***'
        super.create(folderId, terminology)
    }

    @Audit
    @Put(Paths.TERMINOLOGY_ID)
    Terminology update(UUID id, @Body @NonNull Terminology terminology) {
        super.update(id, terminology)
    }

    @Audit(deletedObjectDomainType = Terminology)
    @Transactional
    @Delete(Paths.TERMINOLOGY_ID)
    HttpResponse delete(UUID id, @Body @Nullable Terminology terminology) {
        super.delete(id, terminology)
    }

    @Audit
    @Get(Paths.TERMINOLOGY_SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(UUID id, @RequestBean SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        Terminology terminology = terminologyRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, terminology)
        ListResponse.from(searchRepository.search(requestDTO))
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.TERMINOLOGY_SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(UUID id, @Body SearchRequestDTO requestDTO) {
        requestDTO.withinModelId = id
        Terminology terminology = terminologyRepository.readById(requestDTO.withinModelId)
        accessControlService.checkRole(Role.READER, terminology)
        ListResponse.from(searchRepository.search(requestDTO))
    }


    @Audit
    @Get(Paths.FOLDER_LIST_TERMINOLOGY)
    ListResponse<Terminology> list(UUID folderId) {
        super.list(folderId)
    }

    @Audit
    @Get(Paths.TERMINOLOGY_LIST)
    ListResponse<Terminology> listAll() {
        super.listAll()
    }

    @Transactional
    @Audit(title = EditType.FINALISE, description = "Finalise Terminology")
    @Put(Paths.TERMINOLOGY_FINALISE)
    Terminology finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Audit(title = EditType.COPY, description = "New Version of CodeSet")
    @Transactional
    @Put(Paths.TERMINOLOGY_NEW_BRANCH_MODEL_VERSION)
    Terminology createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Audit
    @Get(Paths.TERMINOLOGY_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModel(id, namespace, name, version)
    }

    @Audit(title = EditType.IMPORT, description = "Import terminology")
    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.TERMINOLOGY_IMPORT)
    ListResponse<Terminology> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)
    }
/*
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
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
    @Get(Paths.TERMINOLOGY_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId) {
        Terminology terminology = modelContentRepository.findWithContentById(id)

        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, terminology, "item not found : $id")
        Terminology other = modelContentRepository.findWithContentById(otherId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, terminology, "item not found : $otherId")

        accessControlService.checkRole(Role.READER, terminology)
        accessControlService.checkRole(Role.READER, other)

        terminology.setAssociations()
        other.setAssociations()
        terminology.diff(other)
    }

    @Get('/terminologies/providers/importers')
    List<TerminologyImporterPlugin> terminologyImporters() {
        mauroPluginService.listPlugins(TerminologyImporterPlugin)
    }

    @Get('/terminologies/providers/exporters')
    List<TerminologyExporterPlugin> terminologyExporters() {
        mauroPluginService.listPlugins(TerminologyExporterPlugin)
    }

    //stub endpoint todo: actual
    @Get('/terminologies/{id}/simpleModelVersionTree')
    List<Map> simpleModelVersionTree(UUID id){
        super.simpleModelVersionTree(id)
    }


    //todo: implement actual
    @Get('/terminologies/{id}/permissions')
    List<Map> permissions(UUID id) {
        super.permissions(id)
    }

}
