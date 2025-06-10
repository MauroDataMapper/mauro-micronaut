package uk.ac.ox.softeng.mauro.controller.folder

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.folder.FolderApi
import uk.ac.ox.softeng.mauro.api.model.PermissionsDTO
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.facet.EditType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.folder.FolderContentRepository
import uk.ac.ox.softeng.mauro.domain.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.domain.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.service.plugin.PluginService
import uk.ac.ox.softeng.mauro.web.ListResponse

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
import jakarta.inject.Inject

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class FolderController extends ModelController<Folder> implements FolderApi {

    @Inject
    FolderContentRepository folderContentRepository

    FolderController(FolderCacheableRepository folderRepository, FolderContentRepository folderContentRepository) {
        super(Folder, folderRepository, folderRepository, folderContentRepository)
    }

    @Audit
    @Get(Paths.FOLDER_ID)
    Folder show(UUID id) {
        super.show(id)
    }

    @Audit
    @Get(Paths.CHILD_FOLDER_ID)
    Folder show(UUID parentId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.FOLDER_LIST)
    Folder create(@Body Folder folder) {
        cleanBody(folder)
        updateCreationProperties(folder)
        folder.authority = super.authorityService.getDefaultAuthority()
        pathRepository.readParentItems(folder)
        folder.updatePath()
        folderRepository.save(folder)
    }

    @Audit
    @Transactional
    @Post(Paths.CHILD_FOLDER_LIST)
    Folder create(UUID parentId, @Body @NonNull Folder folder) {
        super.create(parentId, folder)
    }

    @Audit
    @Put(Paths.FOLDER_ID)
    Folder update(UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Audit
    @Put(Paths.CHILD_FOLDER_ID)
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Audit(description = 'Move folder')
    @Transactional
    @Put(Paths.FOLDER_MOVE)
    Folder moveFolder(UUID id, String destination) {
        super.moveFolder(id, destination)
    }

    @Audit
    @Get(Paths.FOLDER_LIST)
    ListResponse<Folder> listAll() {
        super.listAll()
    }

    @Audit
    @Get(Paths.CHILD_FOLDER_LIST)
    ListResponse<Folder> list(UUID parentId) {
        super.list(parentId)
    }

    //    // Todo: needs repository method 'deleteWithContent'
    //    @Transactional
    //    @Delete('/{id}')
    //    Mono<Long> delete(UUID id, @Nullable @Body Folder folder) {
    //        if (folder?.version == null) {
    //            folderRepository.findById(id).switchIfEmpty(Mono.error(new HttpStatusException(HttpStatus.NOT_FOUND, 'Folder not found for id'))).expand {
    //                if (it.childFolders) Flux.fromIterable(it.childFolders).flatMap {folderRepository.readById(it.id)}
    //                else Flux.empty()
    //            }.flatMapSequential {folderRepository.deleteById(it.id)}.collectList().map {
    //                it.first()
    //            }
    //        } else {
    //            folderRepository.findByIdAndVersion(id, folder.version).switchIfEmpty(
    //                Mono.error(new HttpStatusException(HttpStatus.NOT_FOUND, 'Folder not found for id and version'))).expand {
    //                if (it.childFolders) Flux.fromIterable(it.childFolders).flatMap {folderRepository.readById(it.id)}
    //                else Flux.empty()
    //            }.flatMapSequential {folderRepository.deleteById(it.id)}.collectList().map {
    //                it.first()
    //            }
    //        }
    //    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY, deletedObjectDomainType = Folder)
    @Transactional
    @Delete(Paths.FOLDER_ID)
    HttpResponse delete(UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder, permanent)
    }

    @Transactional
    @Audit(deletedObjectDomainType = Folder, parentDomainType = Folder)
    @Delete(Paths.CHILD_FOLDER_ID)
    HttpResponse delete(UUID parentId, UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder, permanent)
    }

    @Audit(title = EditType.EXPORT, description = 'Export folder')
    @Get(Paths.FOLDER_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        ModelExporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelExporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)
        Folder existing = folderContentRepository.findWithContentById(id)
        createExportResponse(mauroPlugin, existing)
    }

    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Audit(title = EditType.IMPORT, description = 'Import folder')
    @Post(Paths.FOLDER_IMPORT)
    ListResponse<Folder> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)
    }

    @Get('/folder/search{?requestDTO}')
    ListResponse<SearchResultsDTO> searchGet(@RequestBean SearchRequestDTO requestDTO) {
        // TODO
        return null
    }

    @Audit
    @Post('/folder/search')
    ListResponse<SearchResultsDTO> searchPost(@Body SearchRequestDTO requestDTO) {
        // TODO
        return null
    }

    @Audit
    @Put(Paths.FOLDER_READ_BY_AUTHENTICATED)
    @Transactional
    Folder allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as Folder
    }

    @Audit
    @Transactional
    @Delete(Paths.FOLDER_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Put(Paths.FOLDER_READ_BY_EVERYONE)
    @Transactional
    Folder allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as Folder
    }

    @Audit
    @Transactional
    @Delete(Paths.FOLDER_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Audit
    @Get(Paths.FOLDER_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }

    @Get(Paths.FOLDER_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }
}