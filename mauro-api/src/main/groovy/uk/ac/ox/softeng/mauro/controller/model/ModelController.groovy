package uk.ac.ox.softeng.mauro.controller.model

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.multipart.CompletedPart
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelService
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.classifier.ClassifierRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.FileParameter
import uk.ac.ox.softeng.mauro.plugin.importer.ImportParameters
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

import java.nio.charset.StandardCharsets

@Slf4j
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
abstract class ModelController<M extends Model> extends AdministeredItemController<M, Folder> {

    @Inject
    FacetCacheableRepository.ReferenceFileCacheableRepository referenceFileCacheableRepository

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

    @Inject
    MauroPluginService mauroPluginService

    ModelContentRepository<M> modelContentRepository


    ModelService<M> modelService

    @Inject
    ObjectMapper objectMapper

    ModelController(Class<M> modelClass, AdministeredItemCacheableRepository<M> modelRepository, FolderCacheableRepository folderRepository, ModelContentRepository<M> modelContentRepository) {
        super(modelClass, modelRepository, folderRepository, modelContentRepository)
        this.itemClass = modelClass
        this.administeredItemRepository = modelRepository
        this.parentItemRepository = folderRepository
        this.modelContentRepository = modelContentRepository
        this.administeredItemContentRepository = modelContentRepository
    }

    ModelController(Class<M> modelClass, AdministeredItemCacheableRepository<M> modelRepository, FolderCacheableRepository folderRepository,
                    ModelContentRepository<M> modelContentRepository, ModelService modelService) {
        this(modelClass, modelRepository, folderRepository, modelContentRepository)
        this.modelService = modelService
    }

    M show(UUID id) {
        super.show(id)
    }

    @Transactional
    M create(@NonNull UUID folderId, @Body @NonNull M model) {
        super.create(folderId, model) as M
    }

    @Transactional
    M update(UUID id, @Body @NonNull M model) {
        super.update(id, model) as M
    }

    @Transactional
    M moveFolder(UUID id, String destination) {
        M existing = modelRepository.readById(id)
        accessControlService.checkRole(Role.CONTAINER_ADMIN, existing)
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
            accessControlService.checkRole(Role.EDITOR, folder)
            existing.folder = folder
        }
        pathRepository.readParentItems(existing)
        existing.updatePath()
        modelRepository.update(original, existing)
    }

    @Transactional
    HttpStatus delete(UUID id, @Body @Nullable M model) {
        M modelToDelete = (M) modelContentRepository.findWithContentById(id)

        accessControlService.checkRole(Role.CONTAINER_ADMIN, modelToDelete)

        if (model?.version) modelToDelete.version = model.version
        Long deleted = administeredItemContentRepository.deleteWithContent(modelToDelete)
        if (deleted) {
            HttpStatus.NO_CONTENT
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    ListResponse<M> listAll() {
        List<M> models = modelRepository.readAll()
        models = models.findAll { accessControlService.canDoRole(Role.READER, it) }
        models.each {
            pathRepository.readParentItems(it)
            it.updatePath()
        }
        ListResponse.from(models)
    }

    @Transactional
    M finalise(UUID id, @Body FinaliseData finaliseData) {
        M model = modelRepository.findById(id)

        accessControlService.checkRole(Role.EDITOR, model)

        M finalised = modelService.finaliseModel(model, finaliseData.version, finaliseData.versionChangeType, finaliseData.versionTag)
        modelRepository.update(finalised)
    }

    @Transactional
    M createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        if (!createNewVersionData) createNewVersionData = new CreateNewVersionData()
        M existing = getExistingWithContent(id)

        M copy = createCopyModelWithAssociations(existing, createNewVersionData)

        M savedCopy = modelContentRepository.saveWithContent(copy)
        savedCopy
    }

    protected M createCopyModelWithAssociations(M existing, CreateNewVersionData createNewVersionData) {
        M copy = modelService.createNewBranchModelVersion(existing, createNewVersionData.branchName)
        copy.parent = existing.parent
        updateCreationProperties(copy)
        updateDerivedProperties(copy)

        getReferenceFileFileContent([copy] as Collection<AdministeredItem>)
        copy
    }

    protected M getExistingWithContent(UUID id) {
        M existing = modelContentRepository.findWithContentById(id)
        existing.setAssociations()
        getReferenceFileFileContent(existing.getAllContents())

        accessControlService.checkRole(Role.EDITOR, existing)
        accessControlService.checkRole(Role.EDITOR, existing.folder)
        existing
    }

    protected ModelCacheableRepository<M> getModelRepository() {
        (ModelCacheableRepository<M>) administeredItemRepository
    }

    protected FolderCacheableRepository getFolderRepository() {
        (FolderCacheableRepository) parentItemRepository
    }

    @NonNull
    AdministeredItemRepository getRepository(AdministeredItem item) {
        administeredItemRepositories.find { it.handles(item.class) }
    }

    <P extends ImportParameters> P readFromMultipartFormBody(MultipartBody body, Class<P> parametersClass) {
        Map<String, Object> importMap = Flux.from(body).collectList().block().collectEntries { CompletedPart cp ->
            if (cp instanceof CompletedFileUpload) {
                return [cp.name, new FileParameter(cp.filename, cp.contentType.toString(), cp.bytes)]
            } else {
                return [cp.name, new String(cp.bytes, StandardCharsets.UTF_8)]
            }
        }
        return objectMapper.convertValue(importMap, parametersClass)
    }

    StreamedFile exportModel(UUID modelId, String namespace, String name, @Nullable String version) {
        ModelExporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelExporterPlugin, namespace, name, version)
        mauroPluginService.handlePluginNotFound(mauroPlugin, namespace, name)

        M existing = modelContentRepository.findWithContentById(modelId)
        existing.setAssociations()

        StreamedFile export = exportedModelData(mauroPlugin, existing)
        export
    }

    ListResponse<M> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {

        ModelImporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelImporterPlugin, namespace, name, version)
        mauroPluginService.handlePluginNotFound(mauroPlugin, namespace, name)

        ImportParameters importParameters = readFromMultipartFormBody(body, mauroPlugin.importParametersClass())

        List<M> imported = (List<M>) mauroPlugin.importModels(importParameters)

        Folder folder = folderRepository.readById(importParameters.folderId)
        accessControlService.checkRole(Role.EDITOR, folder)
        List<M> saved = imported.collect { M imp ->
            imp.folder = folder
            log.info '** about to saveWithContentBatched... **'
            M savedImported = modelContentRepository.saveWithContent(imp)
            log.info '** finished saveWithContentBatched **'
            savedImported
        }
        List<M> smallerResponse = saved.collect { model ->
            show(model.id)
        }
        ListResponse.from(smallerResponse)
    }

    protected void handlePluginNotFound(MauroPlugin mauroPlugin, String namespace, String name) {
        if (!mauroPlugin) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Model import plugin with namespace: ${namespace}, name: ${name} not found")
        }
    }

    protected StreamedFile exportedModelData(ModelExporterPlugin mauroPlugin, M existing) {
        byte[] fileContents = mauroPlugin.exportModel(existing)
        String filename = mauroPlugin.getFileName(existing)
        new StreamedFile(new ByteArrayInputStream(fileContents), MediaType.APPLICATION_JSON_TYPE).attach(filename)
    }

    protected void getReferenceFileFileContent(Collection<AdministeredItem> administeredItems) {
        administeredItems.each {
            if (it.referenceFiles) {
                it.referenceFiles.each { referenceFile ->
                    if (!referenceFile.fileContents) {
                        log.debug("Model $it.id has reference files. file: $referenceFile.fileName, filecontents is $referenceFile.fileContents")
                        ReferenceFile retrieved = referenceFileCacheableRepository.findById(referenceFile.id) as ReferenceFile
                        if (!retrieved) throw new HttpStatusException(HttpStatus.NOT_FOUND, "Not found for item $it.id")
                        referenceFile.fileContents = retrieved.fileContent()
                    }
                }
            }
        }
    }

    protected void handleNotFoundError(M model, UUID id) {
        if (!model) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Model not found, $id")
        }
    }
}