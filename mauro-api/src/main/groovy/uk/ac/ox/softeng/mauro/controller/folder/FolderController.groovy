package uk.ac.ox.softeng.mauro.controller.folder

import uk.ac.ox.softeng.mauro.service.plugin.PluginService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
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
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.folder.FolderContentRepository
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
@CompileStatic
@Controller('/folders')
@Secured(SecurityRule.IS_ANONYMOUS)
class FolderController extends ModelController<Folder> {

    @Inject
    FolderContentRepository folderContentRepository

    FolderController(FolderCacheableRepository folderRepository, FolderContentRepository folderContentRepository) {
        super(Folder, folderRepository, folderRepository, folderContentRepository)
    }

    @Get('/{id}')
    Folder show(UUID id) {
        super.show(id)
    }

    @Get('/{parentId}/folders/{id}')
    Folder show(UUID parentId, UUID id) {
        super.show(id)
    }

    @Post
    Folder create(@Body Folder folder) {
        cleanBody(folder)
        updateCreationProperties(folder)

        pathRepository.readParentItems(folder)
        folder.updatePath()

        folderRepository.save(folder)
    }

    @Transactional
    @Post('/{parentId}/folders')
    Folder create(UUID parentId, @Body @NonNull Folder folder) {
        super.create(parentId, folder)
    }

    @Put('/{id}')
    Folder update(UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Put('/{parentId}/folders/{id}')
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Transactional
    @Put('/{id}/folder/{destination}')
    Folder moveFolder(UUID id, String destination) {
        super.moveFolder(id, destination)
    }

    @Get
    ListResponse<Folder> listAll() {
        super.listAll()
    }

    @Get('/{parentId}/folders')
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
    @Delete('/{id}')
    HttpStatus delete(UUID id, @Body @Nullable Folder folder) {
        super.delete(id, folder)
    }

    @Transactional
    @Delete('/{parentId}/folders/{id}')
    HttpStatus delete(UUID parentId, UUID id, @Body @Nullable Folder folder) {
        super.delete(id, folder)
    }

    @Get('/{id}/export{/namespace}{/name}{/version}')
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
    @Post('/import/{namespace}/{name}{/version}')
    ListResponse<Folder> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
       super.importModel(body, namespace, name, version)
    }
}