package uk.ac.ox.softeng.mauro.controller.folder

import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.folder.FolderService
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Slf4j
@Controller('/folders')
class FolderController extends ModelController<Folder> {

    FolderRepository folderRepository

    FolderController(FolderRepository folderRepository, ModelContentRepository<Folder> folderContentRepository, FolderService folderService) {
        super(Folder, folderRepository, folderContentRepository, folderRepository, folderService)
        this.folderRepository = folderRepository
    }

    @Get('/{id}')
    Mono<Folder> show(UUID id) {
        super.show(id)
    }

    @Get('/{parentId}/folders/{id}')
    Mono<Folder> showChild(UUID parentId, UUID id) {
        show(id)
    }

    @Post
    Mono<Folder> createRoot(@Body Folder folder) {
        Folder defaultFolder = modelClass.getDeclaredConstructor().newInstance()
        disallowedCreateProperties.each {String key ->
            if (defaultFolder.hasProperty(key).properties.setter && folder[key] != defaultFolder[key]) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Property [' + key + '] cannot be set directly')
            }
        }

        folder.createdBy = 'USER'
        folder.updatePath()
        folderRepository.save(folder)
    }

    @Transactional
    @Post('/{parentId}/folders')
    Mono<Folder> create(UUID parentId, @Body @NonNull Folder folder) {
        super.create(parentId, folder)
    }

    @Put('/{id}')
    Mono<Folder> update(UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Put('/{parentId}/folders/{id}')
    Mono<Folder> updateChild(UUID parentId, UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    // Todo: needs repository method 'updateWithContent'
    // needs to recursively update all child folders
    @Transactional
    @Put('/{id}/folder/{destination}')
    Mono<Folder> moveFolder(UUID id, String destination) {
        folderRepository.readById(id).flatMap {Folder existing ->
            if (destination == 'root') {
                existing.folder = null
                folderRepository.update(existing)
            } else {
                UUID destinationId
                try {
                    destinationId = UUID.fromString(destination)
                } catch (IllegalArgumentException ignored) {
                    throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Destination not a valid UUID')
                }
                folderRepository.readById(destinationId).flatMap {Folder destinationFolder ->
                    existing.folder = destinationFolder
                    folderRepository.update(existing)
                }
            }
        }
    }

    @Get
    Mono<ListResponse<Folder>> listAll() {
        folderRepository.findAll().collectList().map {
            ListResponse.from(it)
        }
    }

    @Get('/{parentId}/folders')
    Mono<ListResponse<Folder>> list(UUID parentId) {
        folderRepository.readById(parentId).flatMap {Folder parent ->
            folderRepository.readAllByParentFolder(parent).collectList()
        }.map {
            ListResponse.from(it)
        }
    }


    // Todo: needs repository method 'deleteWithContent'
    @Transactional
    @Delete('/{id}')
    Mono<Long> delete(UUID id, @Nullable @Body Folder folder) {
        if (folder?.version == null) {
            folderRepository.findById(id).switchIfEmpty(Mono.error(new HttpStatusException(HttpStatus.NOT_FOUND, 'Folder not found for id'))).expand {
                if (it.childFolders) Flux.fromIterable(it.childFolders).flatMap {folderRepository.readById(it.id)}
                else Flux.empty()
            }.flatMapSequential {folderRepository.deleteById(it.id)}.collectList().map {
                it.first()
            }
        } else {
            folderRepository.findByIdAndVersion(id, folder.version).switchIfEmpty(
                Mono.error(new HttpStatusException(HttpStatus.NOT_FOUND, 'Folder not found for id and version'))).expand {
                if (it.childFolders) Flux.fromIterable(it.childFolders).flatMap {folderRepository.readById(it.id)}
                else Flux.empty()
            }.flatMapSequential {folderRepository.deleteById(it.id)}.collectList().map {
                it.first()
            }
        }
    }

    @Delete('/{parentId}/folders/{id}')
    Mono<Long> deleteChild(UUID parentId, UUID id, @Nullable @Body Folder folder) {
        delete(id, folder)
    }
}
