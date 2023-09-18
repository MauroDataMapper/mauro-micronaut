package uk.ac.ox.softeng.mauro.folder

import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.json.tree.JsonObject
import jakarta.inject.Inject
import jakarta.persistence.EntityNotFoundException
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

    @Post
    Mono<Folder> create(@Body Folder folder) {
        folder.createdBy = 'USER'
        println folder.toString()
        folderRepository.save(folder)
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

    @Get
    Mono<List<Folder>> list() {
        folderRepository.findAll().collectList()
    }

    @Transactional
    @Delete('/{id}')
    Mono<Long> delete(UUID id, @Nullable @Body Folder folder) {
        if (folder?.version == null) {
            folderRepository.readById(id).single().log().expand {
                if (!it) return Mono.error(new EntityNotFoundException('Folder not found for id'))
                if (it.childFolders) Flux.fromIterable(it.childFolders).flatMap {folderRepository.readById(it.id)}
                else Flux.empty()
            }.flatMapSequential {folderRepository.deleteById(it.id)}.collectList().map {
                it.first()
            }
        } else {
            folderRepository.readByIdAndVersion(id, folder.version).single().log().expand {
                if (!it) return Mono.error(new EntityNotFoundException('Folder not found for id and version'))
                if (it.childFolders) Flux.fromIterable(it.childFolders).flatMap {folderRepository.readById(it.id)}
                else Flux.empty()
            }.flatMapSequential {folderRepository.deleteById(it.id)}.collectList().map {
                it.first()
            }
        }
    }
}
