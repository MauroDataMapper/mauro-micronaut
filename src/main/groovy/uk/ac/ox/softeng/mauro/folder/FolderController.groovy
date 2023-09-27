package uk.ac.ox.softeng.mauro.folder

import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Slf4j
@Controller('/folders')
class FolderController {

    final static List<String> DISALLOWED_PROPERTIES = ['class', 'id']

    @Inject
    FolderRepository folderRepository

    @Get('/{id}')
    Mono<Folder> show(UUID id) {
        folderRepository.findById(id)
    }

    @Get('/{parentId}/folders/{id}')
    Mono<Folder> showChild(UUID parentId, UUID id) {
        show(id)
    }

    @Post
    Mono<Folder> createRoot(@Body Folder folder) {
        folder.createdBy = 'USER'
        folderRepository.save(folder)
    }

    @Transactional
    @Post('/{parentId}/folders')
    Mono<Folder> create(UUID parentId, @Body Folder folder) {
        folder.createdBy = 'USER'
        folderRepository.readById(parentId).flatMap {Folder parent ->
            folder.parentFolder = parent
            folderRepository.save(folder)
        }
    }

    @Put('/{id}')
    Mono<Folder> update(UUID id, @Body Folder folder) {
        folderRepository.readById(id).flatMap {Folder existing ->
            existing.properties.each {
                if (!DISALLOWED_PROPERTIES.contains(it.key) && folder[it.key] != null) {
                    existing[it.key] = folder[it.key]
                }
            }
            folderRepository.update(existing)
        }
    }

    @Put('/{parentId}/folders/{id}')
    Mono<Folder> updateChild(UUID parentId, UUID id, @Body Folder folder) {
        update(id, folder)
    }

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
            folderRepository.findAllByParentFolder(parent).collectList()
        }.map {
            ListResponse.from(it)
        }
    }

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
            folderRepository.findByIdAndVersion(id, folder.version).switchIfEmpty(Mono.error(new HttpStatusException(HttpStatus.NOT_FOUND, 'Folder not found for id and version'))).expand {
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
