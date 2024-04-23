package uk.ac.ox.softeng.mauro.controller.model

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.importer.DataModelImporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.FileParameter
import uk.ac.ox.softeng.mauro.plugin.importer.ImportParameters
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.exceptions.EmptyResultException
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.multipart.CompletedPart
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelService
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import reactor.core.publisher.Flux

import java.nio.charset.StandardCharsets

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
    List<AdministeredItemCacheableRepository> administeredItemRepositories

//    @Inject
//    AccessControlService accessControlService

    ModelContentRepository<M> modelContentRepository

    ModelService<M> modelService

    @Inject // Doesn't inject?
    ObjectMapper objectMapper


    ModelController(Class<M> modelClass, AdministeredItemCacheableRepository<M> modelRepository, FolderCacheableRepository folderRepository, ModelContentRepository<M> modelContentRepository, ObjectMapper objectMapper) {
        super(modelClass, modelRepository, folderRepository, modelContentRepository)
        this.itemClass = modelClass
        this.administeredItemRepository = modelRepository
        this.parentItemRepository = folderRepository
        this.modelContentRepository = modelContentRepository
        this.administeredItemContentRepository = modelContentRepository
        this.objectMapper = objectMapper
    }

    M show(UUID id) {
        M model
        try {
            model = modelRepository.findById(id)
        } catch (EmptyResultException e) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, e.getMessage())
        }
        if (!model) return null
        pathRepository.readParentItems(model)
        model.updatePath()
        model
    }

    @Transactional
    M update(UUID id, @Body @NonNull M model) {
        super.update(id, model)
    }

    @Transactional
    M moveFolder(UUID id, String destination) {
        M existing = modelRepository.readById(id)
        M original = (M) existing.clone()
        if (destination == 'root') {
            existing.folder = null
        } else {
            UUID destinationId
            try {
                destinationId = UUID.fromString(destination)
            } catch (IllegalArgumentException ignored) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, 'Destination not "root" or a valid UUID')
            }
            Folder folder = folderRepository.readById(destinationId)
            existing.folder = folder
        }
        pathRepository.readParentItems(existing)
        existing.updatePath()
        modelRepository.update(original, existing)
    }

    HttpStatus delete(UUID id, @Body @Nullable M model) {
        super.delete(id, model)
    }

    ListResponse<M> listAll() {
        List<M> models = modelRepository.readAll()
        models.each {
            pathRepository.readParentItems(it)
            it.updatePath()
        }
        ListResponse.from(models)
    }

    @Transactional
    M finalise(UUID id, @Body FinaliseData finaliseData) {
        M model = modelRepository.findById(id)
        M finalised = modelService.finaliseModel(model, finaliseData.version, finaliseData.versionChangeType, finaliseData.versionTag)
        modelRepository.update(finalised)
    }

    @Transactional
    M createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        if (!createNewVersionData) createNewVersionData = new CreateNewVersionData()
        M existing = modelRepository.findById(id)
        M copy = modelService.createNewBranchModelVersion(existing, createNewVersionData.branchName)

        M savedCopy = createEntity(copy.folder, copy)
        savedCopy.allContents.each {AdministeredItem item ->
            log.debug "*** Saving item [$item.id : $item.label] ***"
            item.updateCreationProperties()
            getRepository(item).save(item)
        }
        savedCopy
    }

    protected ModelCacheableRepository<M> getModelRepository() {
        (ModelCacheableRepository<M>) administeredItemRepository
    }

    protected FolderCacheableRepository getFolderRepository() {
        (FolderCacheableRepository) parentItemRepository
    }

    @NonNull
    AdministeredItemRepository getRepository(AdministeredItem item) {
        administeredItemRepositories.find {it.handles(item.class)}
    }

    M showNested(UUID uuid) {
        administeredItemRepository.findById(uuid)
    }




    <P extends ImportParameters> P readFromMultipartFormBody(MultipartBody body, Class<P> parametersClass) {
        Map<String, Object> importMap = Flux.from(body).collectList().block().collectEntries {CompletedPart cp ->
            if (cp instanceof CompletedFileUpload) {
                return [cp.name, new FileParameter(cp.filename, cp.contentType.toString(), cp.bytes)]
            } else {
                return [cp.name, new String(cp.bytes, StandardCharsets.UTF_8)]
            }

        }


        return objectMapper.convertValue(importMap, parametersClass)
    }

    ListResponse<M> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {

        ModelImporterPlugin mauroPlugin = MauroPluginService.getPlugin(ModelImporterPlugin, namespace, name, version)

        if(!mauroPlugin) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Model import plugin with namespace: ${namespace}, name: ${name} not found")
        }

        ImportParameters importParameters = readFromMultipartFormBody(body, mauroPlugin.importParametersClass())

        List<M> imported = (List<M>) mauroPlugin.importModels(importParameters)

        Folder folder = folderRepository.readById(importParameters.folderId)
        List<M> saved = imported.collect { M imp ->
            imp.folder = folder
            log.info '** about to saveWithContentBatched... **'
            M savedImported = modelContentRepository.saveWithContent(imp)
            log.info '** finished saveWithContentBatched **'
            savedImported
        }

        ListResponse.from(saved)
    }
}
