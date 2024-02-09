package uk.ac.ox.softeng.mauro.controller.model

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelService
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.export.ExportMetadata
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableModelRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableModelRepository.CacheableFolderRepository
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.security.AccessControlService
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.time.Instant
import java.util.function.BiFunction

@Slf4j
@CompileStatic
abstract class ModelController<M extends Model> extends AdministeredItemController<M, Folder> {

    @Override
    List<String> getDisallowedProperties() {
        log.debug '***** ModelController::getDisallowedProperties *****'
        super.disallowedProperties +
        ['finalised', 'dateFinalised', 'readableByEveryone', 'readableByAuthenticatedUsers', 'modelType', 'deleted', 'folder', 'authority', 'branchName',
         'modelVersion', 'modelVersionTag']
    }

    @Override
    List<String> getDisallowedCreateProperties() {
        disallowedProperties - ['readableByEveryone', 'readableByAuthenticatedUsers']
    }

    @Inject
    List<CacheableAdministeredItemRepository> administeredItemRepositories

    @Inject
    AccessControlService accessControlService

    ModelContentRepository<M> modelContentRepository

    ModelService<M> modelService

    ModelController(Class<M> modelClass, CacheableAdministeredItemRepository<M> modelRepository, CacheableFolderRepository folderRepository, ModelContentRepository<M> modelContentRepository) {
        super(modelClass, modelRepository, folderRepository, modelContentRepository)
        this.itemClass = modelClass
        this.administeredItemRepository = modelRepository
        this.parentItemRepository = folderRepository
        this.modelContentRepository = modelContentRepository
        this.administeredItemContentRepository = modelContentRepository
    }

    Mono<M> show(UUID id) {
        modelRepository.findById(id).flatMap { M model ->
            accessControlService.canRead(model).then(
                pathRepository.readParentItems(model).map {
                    model.updatePath()
                    model
                }
            )
        } as Mono<M>
    }

    Mono<M> update(UUID id, @Body @NonNull M model) {
        update(null, id, model)
    }

    @Transactional
    Mono<M> moveFolder(UUID id, String destination) {
        modelRepository.readById(id).flatMap {M existing ->
            AdministeredItem original = existing.clone()
            if (destination == 'root') {
                existing.folder = null
                pathRepository.readParentItems(existing).flatMap {
                    existing.updatePath()
                    modelRepository.update(original, existing)
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
                        modelRepository.update(original, existing)
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

    @Transactional
    Mono<M> finalise(UUID id, @Body FinaliseData finaliseData) {
        modelRepository.findById(id).flatMap {M model ->
            M finalised = modelService.finaliseModel(model, finaliseData.version, finaliseData.versionChangeType, finaliseData.versionTag)
            modelRepository.update(finalised)
        }
    }

    @Transactional
    Mono<M> createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        if (!createNewVersionData) createNewVersionData = new CreateNewVersionData()
        modelRepository.findById(id).flatMap {M existing ->
            M copy = modelService.createNewBranchModelVersion(existing, createNewVersionData.branchName)

            createEntity(copy.folder, copy).flatMap {M savedCopy ->
                Flux.fromIterable(savedCopy.getAllContents()).concatMap {AdministeredItem item ->
                    log.debug "*** Saving item [$item.id : $item.label] ***"
                    updateCreationProperties(item)
                    getRepository(item).save(item)
                }.then(Mono.just(savedCopy))
            }
        }
    }

    protected CacheableModelRepository<M> getModelRepository() {
        (CacheableModelRepository<M>) administeredItemRepository
    }

    protected CacheableFolderRepository getFolderRepository() {
        (CacheableFolderRepository) parentItemRepository
    }

    @NonNull
    AdministeredItemRepository getRepository(AdministeredItem item) {
        administeredItemRepositories.find {it.handles(item.class)}
    }
}
