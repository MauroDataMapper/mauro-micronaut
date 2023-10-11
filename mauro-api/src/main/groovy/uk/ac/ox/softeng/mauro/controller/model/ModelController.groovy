package uk.ac.ox.softeng.mauro.controller.model

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.ModelService

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
abstract class ModelController<M extends Model> extends AdministeredItemController<M> {

    @Override
    List<String> getDisallowedProperties() {
        super.disallowedProperties +
        ['finalised', 'dateFinalised', 'readableByEveryone', 'readableByAuthenticatedUsers', 'modelType', 'deleted', 'folder', /*'authority',*/ 'branchName',
         'modelVersion', 'modelVersionTag']
    }

    @Override
    List<String> getCascadeUpdateProperties() {
        super.cascadeUpdateProperties +
        ['finalised', 'branchName', 'modelVersion']
    }

    /**
     * Properties disallowed in a simple create request.
     */
    List<String> getDisallowedCreateProperties() {
        disallowedProperties - ['readableByEveryone', 'readableByAuthenticatedUsers']
    }

    ModelRepository<M> modelRepository

    FolderRepository folderRepository

    ModelService<M> modelService

    Class<M> modelClass

    ModelController(Class<M> modelClass, ModelRepository<M> modelRepository, FolderRepository folderRepository, ModelService<M> modelService) {
        this.modelClass = modelClass
        this.modelRepository = modelRepository
        this.folderRepository = folderRepository
        this.modelService = modelService
    }

    Mono<M> show(UUID id) {
        modelRepository.findById(id)
    }

    @Transactional
    Mono<M> create(UUID folderId, @Body @NonNull M model) {
        M defaultModel = modelClass.getDeclaredConstructor().newInstance()
        disallowedCreateProperties.each {String key ->
            if (model[key] != defaultModel[key]) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Property [' + key + '] cannot be set directly')
            }
        }

        folderRepository.readById(folderId).flatMap {Folder folder ->
            model.folder = folder
            model.createdBy = 'USER'
            model.updatePath()
            modelRepository.save(model)
        }
    }

    Mono<M> update(UUID id, @Body @NonNull M model) {
        M defaultModel = modelClass.getDeclaredConstructor().newInstance()
        disallowedProperties.each {String key ->
            if (model[key] != defaultModel[key]) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Property [' + key + '] cannot be set directly')
            }
        }
        model.properties.each {
            if (it.value instanceof Collection || it.value instanceof Map) {
                model[it.key] = null
            }
        }

        boolean cascadeUpdate = cascadeUpdateProperties.any {model[it] != null}
        boolean doUpdate
        if (cascadeUpdate) {
            boolean doCascadeUpdate
            modelRepository.findById(id).flatMap {M existing ->
                existing.properties.each {
                    if (!disallowedProperties.contains(it.key) && model[it.key] != null) {
                        if (existing[it.key] != model[it.key]) {
                            existing[it.key] = model[it.key]
                            doUpdate = true
                            if (cascadeUpdateProperties.contains(it.key)) doCascadeUpdate = true
                        }
                    }
                }
                if (doUpdate) {
                    if (doCascadeUpdate) {
                        modelService.updateDerived(existing)
                        modelRepository.updateWithContent(existing)
                    } else {
                        modelRepository.update(existing)
                    }
                }
            }
        } else {
            modelRepository.readById(id).flatMap {M existing ->
                existing.properties.each {
                    if (!disallowedProperties.contains(it.key) && model[it.key] != null) {
                        if (existing[it.key] != model[it.key]) {
                            existing[it.key] = model[it.key]
                            doUpdate = true
                        }
                    }
                }
                if (doUpdate) {
                    modelRepository.update(existing)
                }
            }
        }
    }

    @Transactional
    Mono<HttpStatus> delete(UUID id, @Body @Nullable M model) {
        M deleteModel = modelClass.getDeclaredConstructor().newInstance()
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
