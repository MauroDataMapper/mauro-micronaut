package uk.ac.ox.softeng.mauro.controller.model

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.validation.Valid
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
abstract class ModelController<M extends Model> {

    static List<String> DISALLOWED_PROPERTIES = ['class', 'id']

    static List<String> getDISALLOWED_PROPERTIES() {
        DISALLOWED_PROPERTIES
    }

    ModelRepository<M> modelRepository

    FolderRepository folderRepository

    Class<M> modelClass

    ModelController(Class<M> modelClass, ModelRepository<M> modelRepository, FolderRepository folderRepository) {
        this.modelClass = modelClass
        this.modelRepository = modelRepository
        this.folderRepository = folderRepository
    }

    Mono<M> show(UUID id) {
        modelRepository.findById(id)
    }

    Mono<M> create(UUID folderId, @Body @Valid @NonNull M model) {
        folderRepository.readById(folderId).flatMap {Folder folder ->
            model.folder = folder
            model.createdBy = 'USER'
            modelRepository.save(model)
        }
    }

    Mono<M> update(UUID id, @Body @NonNull M model) {
        modelRepository.readById(id).flatMap {M existing ->
            existing.properties.each {
                if (!DISALLOWED_PROPERTIES.contains(it.key) && model[it.key] != null) {
                    existing[it.key] = model[it.key]
                }
            }
            modelRepository.update(existing)
        }
    }

    @Transactional
    Mono<HttpStatus> delete(UUID id, @Body @Nullable M model) {
        M deleteModel = modelClass.newInstance()
        deleteModel.id = id
        deleteModel.version = model?.version
        modelRepository.deleteWithContent(deleteModel).map {Boolean deleted ->
            if (deleted) {
                HttpStatus.NO_CONTENT
            } else {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
            }
        }
    }

    Mono<ListResponse<M>> list(UUID folderId) {
        folderRepository.readById(folderId).flatMap {Folder folder ->
            modelRepository.readAllByFolder(folder).collectList().map {
                ListResponse.from(it)
            }
        }
    }

    Mono<ListResponse<M>> listAll() {
        modelRepository.readAll().collectList().map {
            ListResponse.from(it)
        }
    }
}
