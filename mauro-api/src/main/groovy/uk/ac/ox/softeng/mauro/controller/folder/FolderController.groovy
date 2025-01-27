package uk.ac.ox.softeng.mauro.controller.folder

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.folder.FolderApi
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.folder.FolderContentRepository
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.service.plugin.PluginService
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile
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

    @Get(Paths.FOLDER_ID)
    Folder show(UUID id) {
        super.show(id)
    }

    @Get(Paths.CHILD_FOLDER_ID)
    Folder show(UUID parentId, UUID id) {
        super.show(id)
    }

    @Post(Paths.FOLDER_LIST)
    Folder create(@Body Folder folder) {
        cleanBody(folder)
        updateCreationProperties(folder)
        folder.authority = super.authorityService.getDefaultAuthority()
        pathRepository.readParentItems(folder)
        folder.updatePath()
        folderRepository.save(folder)
    }

    @Transactional
    @Post(Paths.CHILD_FOLDER_LIST)
    Folder create(UUID parentId, @Body @NonNull Folder folder) {
        super.create(parentId, folder)
    }

    @Put(Paths.FOLDER_ID)
    Folder update(UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Put(Paths.CHILD_FOLDER_ID)
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Transactional
    @Put(Paths.FOLDER_MOVE)
    Folder moveFolder(UUID id, String destination) {
        super.moveFolder(id, destination)
    }

    @Get(Paths.FOLDER_LIST)
    ListResponse<Folder> listAll() {
        super.listAll()
    }

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

    @Transactional
    @Delete(Paths.FOLDER_ID)
    HttpResponse delete(UUID id, @Body @Nullable Folder folder) {
        super.delete(id, folder)
    }

    @Transactional
    @Delete(Paths.CHILD_FOLDER_ID)
    HttpResponse delete(UUID parentId, UUID id, @Body @Nullable Folder folder) {
        super.delete(id, folder)
    }

    @Get(Paths.FOLDER_EXPORT)
    StreamedFile exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        ModelExporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelExporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)
        Folder existing = folderContentRepository.findWithContentById(id)
        StreamedFile streamedFile = exportedModelData(mauroPlugin, existing)
        streamedFile
    }

    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.FOLDER_IMPORT)
    ListResponse<Folder> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
       super.importModel(body, namespace, name, version)
    }
}