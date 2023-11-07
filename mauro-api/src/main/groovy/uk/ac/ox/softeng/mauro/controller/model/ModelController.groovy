package uk.ac.ox.softeng.mauro.controller.model

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.export.ExportMetadata
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import reactor.core.publisher.Mono

import java.time.OffsetDateTime
import java.util.function.BiFunction

@Slf4j
@CompileStatic
abstract class ModelController<M extends Model> extends AdministeredItemController<M, Folder> {

    @Override
    List<String> getDisallowedProperties() {
        super.disallowedProperties +
        ['finalised', 'dateFinalised', 'readableByEveryone', 'readableByAuthenticatedUsers', 'modelType', 'deleted', 'folder', 'authority', 'branchName',
         'modelVersion', 'modelVersionTag']
    }

    @Override
    List<String> getDisallowedCreateProperties() {
        disallowedProperties - ['readableByEveryone', 'readableByAuthenticatedUsers']
    }

    ModelController(Class<M> modelClass, ModelRepository<M> modelRepository, FolderRepository folderRepository, ModelContentRepository<M> modelContentRepository) {
        super(modelClass, modelRepository, (AdministeredItemRepository<Folder>) folderRepository, modelContentRepository)
        this.itemClass = modelClass
        this.administeredItemRepository = modelRepository
        this.parentItemRepository = folderRepository
        this.administeredItemContentRepository = modelContentRepository
    }

    Mono<M> show(UUID id) {
        modelRepository.readById(id).flatMap {M model ->
            pathRepository.readParentItems(model).map {
                model.updatePath()
                model
            }
        }
    }

    Mono<M> update(UUID id, @Body @NonNull M model) {
        update(null, id, model)
    }

    @Transactional
    Mono<M> moveFolder(UUID id, String destination) {
        modelRepository.readById(id).flatMap {M existing ->
            if (destination == 'root') {
                existing.folder = null
                pathRepository.readParentItems(existing).flatMap {
                    existing.updatePath()
                    modelRepository.update(existing)
                }
            } else {
                UUID destinationId
                try {
                    destinationId = UUID.fromString(destination)
                } catch (IllegalArgumentException _) {
                    throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Destination not "root" or a valid UUID')
                }
                folderRepository.readById(destinationId).flatMap {Folder folder ->
                    existing.folder = folder
                    pathRepository.readParentItems(existing).flatMap {
                        existing.updatePath()
                        modelRepository.update(existing)
                    }
                }
            }
        }
    }

    Mono<HttpStatus> delete(UUID id, @Body @Nullable M model) {
        delete(null, id, model)
    }

    Mono<ListResponse<M>> listAll() {
        modelRepository.readAll().flatMap {M model ->
            Mono.zip(Mono.just(model), pathRepository.readParentItems(model), (BiFunction<M, List<AdministeredItem>, M>) {it, _ -> it})
        }.collectList().map {List<M> models ->
            models.each {((Model) it).updatePath()}
            ListResponse.from(models)
        }
    }

//    @Transactional
//    Mono<M> finalise(UUID id, @Body FinaliseData finaliseData) {
//        modelRepository.findById(id).flatMap {M model ->
//            M finalised = modelService.finaliseModel
//        }
//    }

    protected ModelRepository<M> getModelRepository() {
        (ModelRepository<M>) administeredItemRepository
    }

    protected FolderRepository getFolderRepository() {
        (FolderRepository) parentItemRepository
    }
}
