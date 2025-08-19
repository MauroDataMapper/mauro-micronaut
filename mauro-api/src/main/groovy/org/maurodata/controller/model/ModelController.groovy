package org.maurodata.controller.model

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.api.model.ModelApi
import org.maurodata.api.model.ModelRefDTO
import org.maurodata.api.model.PermissionsDTO
import org.maurodata.api.model.VersionLinkDTO
import org.maurodata.domain.diff.ArrayDiff
import org.maurodata.domain.diff.FieldDiff
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelService
import org.maurodata.domain.model.Path
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.UserGroup
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.facet.VersionLinkRepository
import org.maurodata.persistence.model.ModelContentRepository
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.exporter.ModelExporterPlugin
import org.maurodata.plugin.importer.ImportParameters
import org.maurodata.plugin.importer.ModelImporterPlugin
import org.maurodata.service.core.AuthorityService
import org.maurodata.service.model.ImportExportModelService
import org.maurodata.service.plugin.PluginService
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@Slf4j
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
abstract class ModelController<M extends Model> extends AdministeredItemController<M, Folder> implements ModelApi<M> {

    @Inject
    FacetCacheableRepository.ReferenceFileCacheableRepository referenceFileCacheableRepository

    @Inject
    ImportExportModelService importExportModelService

    @Override
    List<String> getDisallowedProperties() {
        log.debug '***** ModelController::getDisallowedProperties *****'
        super.disallowedProperties +
                ['finalised', 'dateFinalised', 'readableByEveryone', 'readableByAuthenticatedUsers', 'modelType', 'deleted', 'folder', 'branchName',
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
    AuthorityService authorityService

    @Inject
    VersionLinkRepository versionLinkRepositoryUncached

    ModelController(Class<M> modelClass, AdministeredItemCacheableRepository<M> modelRepository, FolderCacheableRepository folderRepository,
                    ModelContentRepository<M> modelContentRepository) {
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
        model.authority = authorityService.getDefaultAuthority()
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
                throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Destination not "root" or a valid UUID')
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
    HttpResponse delete(UUID id, @Body @Nullable M model, @Nullable Boolean permanent) {

        M modelToDelete = (M) modelContentRepository.findWithContentById(id)

        if (modelToDelete == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found for deletion")
        }

        accessControlService.checkRole(Role.CONTAINER_ADMIN, modelToDelete)

        if (model?.version) modelToDelete.version = model.version

        if (permanent) {

            if (!administeredItemContentRepository.deleteWithContent(modelToDelete)) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
            }
        } else {
            modelToDelete.deleted(true)
            administeredItemRepository.update(modelToDelete)
        }
        HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    @Transactional
    M putReadByAuthenticated(UUID id) {
        M modelToUse = (M) modelContentRepository.findWithContentById(id)

        if (modelToUse == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found for readByAuthenticated")
        }

        accessControlService.checkRole(Role.CONTAINER_ADMIN, modelToUse)

        modelToUse.readableByAuthenticatedUsers(true)
        administeredItemRepository.update(modelToUse)

        modelToUse
    }

    @Transactional
    HttpResponse deleteReadByAuthenticated(UUID id) {
        M modelToUse = (M) modelContentRepository.findWithContentById(id)

        if (modelToUse == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found for readByAuthenticated")
        }

        accessControlService.checkRole(Role.CONTAINER_ADMIN, modelToUse)

        modelToUse.readableByAuthenticatedUsers(false)
        administeredItemRepository.update(modelToUse)

        HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    @Transactional
    M putReadByEveryone(UUID id) {
        M modelToUse = (M) modelContentRepository.findWithContentById(id)

        if (modelToUse == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found for readByAuthenticated")
        }

        accessControlService.checkRole(Role.CONTAINER_ADMIN, modelToUse)

        modelToUse.readableByEveryone(true)
        administeredItemRepository.update(modelToUse)

        modelToUse
    }

    @Transactional
    HttpResponse deleteReadByEveryone(UUID id) {
        M modelToUse = (M) modelContentRepository.findWithContentById(id)

        if (modelToUse == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found for readByAuthenticated")
        }

        accessControlService.checkRole(Role.CONTAINER_ADMIN, modelToUse)

        modelToUse.readableByEveryone(false)
        administeredItemRepository.update(modelToUse)

        HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    ListResponse<M> listAll(@Nullable PaginationParams params = new PaginationParams()) {

        List<M> models = modelRepository.readAll()
        models = models.findAll { accessControlService.canDoRole(Role.READER, it) }
        models.each {
            pathRepository.readParentItems(it)
            it.updatePath()
        }
        ListResponse.from(models, params)
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

        if (existing == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found")
        }

        M copy = createCopyModelWithAssociations(existing, createNewVersionData)

        M savedCopy = modelContentRepository.saveWithContent(copy)

        final VersionLink versionLink = new VersionLink(versionLinkType: VersionLink.NEW_MODEL_VERSION_OF)
        versionLink.setTargetModel(savedCopy)
        existing.versionLinks.add(versionLink)

        final List<AdministeredItem> toSave = new LinkedList<>()
        toSave.add(existing)
        modelContentRepository.saveVersionLinks(toSave)

        savedCopy
    }

    //todo: implement actual
    List<Map> simpleModelVersionTree(UUID id) {
        [
                [
                        id         : id,
                        branch     : 'main',
                        displayName: 'main'
                ]
        ] as List<Map>
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


    HttpResponse<byte[]> exportModel(UUID modelId, String namespace, String name, @Nullable String version) {
        ModelExporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelExporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)
        M existing = getModelWithContent(modelId)
        importExportModelService.createExportResponse(mauroPlugin, existing)
    }

    List<M> importModelList(@Body MultipartBody body, String namespace, String name, @Nullable String version) {

        ModelImporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelImporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)

        ImportParameters importParameters = importExportModelService.readFromMultipartFormBody(body, mauroPlugin.importParametersClass())

        if (importParameters.folderId == null) {
            ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, importParameters.folderId, "Please choose the folder into which the Model/s should be imported.")
        }

        List<M> imported = (List<M>) mauroPlugin.importModels(importParameters)

        Folder folder = folderRepository.readById(importParameters.folderId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, folder, "Folder with id $importParameters.folderId not found")
        accessControlService.checkRole(Role.EDITOR, folder)
        imported.each {M imp ->
            imp.folder = folder
            updateCreationProperties(imp)
        }
        imported

    }

    ListResponse<M> importModel(@Body io.micronaut.http.client.multipart.MultipartBody body, String namespace, String name, @Nullable String version) {
        throw new Exception("Client version of import model has been called.. hint client MultipartBody ")
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



    protected VersionLinkDTO constructVersionLinkDTO(final M sourceModel, final VersionLink versionLink) {
        // Look up target model

        final M targetModel = show(versionLink.targetModelId)
        if (targetModel == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found")
        }

        ModelRefDTO sourceModelDto = new ModelRefDTO(id: sourceModel.id, domainType: sourceModel.domainType, label: sourceModel.label)
        ModelRefDTO targetModelDto = new ModelRefDTO(id: targetModel.id, domainType: targetModel.domainType, label: targetModel.label, model: targetModel.owner? targetModel.owner.id : null )

        final VersionLinkDTO versionLinkDTO = new VersionLinkDTO(id: versionLink.id, linkType: versionLink.versionLinkType, sourceModel: sourceModelDto,
                targetModel: targetModelDto)

        versionLinkDTO
    }

    M findCommonAncestorBetweenModels(M leftModel, M rightModel) {

        final M finalisedLeftParent = getFinalisedParent(leftModel)
        final M finalisedRightParent = getFinalisedParent(rightModel)

        if (!finalisedLeftParent) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "MS01. Model [${leftModel.id.toString()}] has no finalised parent therefore cannot have a common ancestor with Model " +
                            "[${rightModel.id}]")
        }

        if (!finalisedRightParent) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "MS02. Model [${rightModel.id.toString()}] has no finalised parent therefore cannot have a common ancestor with Model " +
                            "[${leftModel.id}]")
        }

        return (finalisedLeftParent.modelVersion < finalisedRightParent.modelVersion) ? finalisedLeftParent : finalisedRightParent
    }

    M getFinalisedParent(final M model) {
        M currentModel = model

        for (; ;) {
            if (currentModel == null) {
                break
            }
            if (currentModel.finalised && currentModel.isVersionable()) {
                return currentModel
            }
            final UUID currentId = currentModel.id
            final UUID sourceModelUUID = versionLinkRepositoryUncached.findSourceModel(currentId)

            if (sourceModelUUID != null) {
                currentModel = modelContentRepository.findWithContentById(sourceModelUUID)
                continue
            }
            break
        }

        return null
    }

    ArrayList<Model> populateVersionTree(UUID id, boolean branchesOnly, final Map<UUID, Map<String, Boolean>> flags) {
        /*
        Get the UUIDs of upstream versions using
        versionLinkRepositoryUncached.findSourceModel
         */

        final Model givenModel = show(id)
        if (givenModel == null) throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found")

        UUID currentId = id

        for (; ;) {
            final UUID sourceModelUUID = versionLinkRepositoryUncached.findSourceModel(currentId)
            if (sourceModelUUID != null) {
                currentId = sourceModelUUID
                continue
            }
            break
        }

        final Model rootObject = modelContentRepository.findWithContentById(currentId)

        if (rootObject == null) throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find root object")

        final ArrayList<Model> allModels = new ArrayList<>(10)
        allModels.add(rootObject)
        populateByVersionLink(rootObject, allModels, flags, branchesOnly)

        return allModels
    }

    private void populateByVersionLink(final Model parent, final ArrayList<Model> into, final Map<UUID, Map<String, Boolean>> flags, final boolean branchesOnly) {
        if (parent == null) {
            return
        }
        if (parent.versionLinks == null) {
            return
        }
        if (parent.versionLinks.isEmpty()) {
            return
        }

        for (VersionLink childVersion : parent.versionLinks) {
            final UUID targetModelId = childVersion.targetModelId
            if (targetModelId == null) {
                continue
            }

            // It's assumed that the 'branchesOnly' flag means: not forks
            if (branchesOnly && childVersion.versionLinkType == VersionLink.NEW_FORK_OF) {
                continue
            }

            if (flags != null) {
                if (childVersion.versionLinkType == VersionLink.NEW_MODEL_VERSION_OF) {
                    flags.put(targetModelId, ["isNewBranchModelVersion": true])
                } else if (childVersion.versionLinkType == VersionLink.NEW_FORK_OF) {
                    flags.put(targetModelId, ["isNewFork": true])
                }
            }

            final Model childModel = modelContentRepository.findWithContentById(targetModelId)

            if (childModel == null) {
                continue
            }

            into.add(childModel)
            populateByVersionLink(childModel, into, flags, branchesOnly)
        }
    }

    Map<String, Map<String, FieldDiff>> flattenDiff(final ObjectDiff objectDiff) {
        final Map<String, FieldDiff> mapCreated = new LinkedHashMap<>(50)
        final Map<String, FieldDiff> mapModified = new LinkedHashMap<>(50)
        final Map<String, FieldDiff> mapDeleted = new LinkedHashMap<>(50)

        // Make the assumption that the top level field diffs are modifications
        flattenDiffInto(objectDiff, mapCreated, mapModified, mapDeleted, DIFF_TYPE_MODIFIED)

        final Map<String, Map<String, FieldDiff>> flattened = new HashMap<>(3)
        flattened.put("created", mapCreated)
        flattened.put("modified", mapModified)
        flattened.put("deleted", mapDeleted)

        return flattened
    }

    private static int DIFF_TYPE_CREATED = 0, DIFF_TYPE_MODIFIED = 1, DIFF_TYPE_DELETED = 2

    private static void flattenDiffInto(
            final ObjectDiff objectDiff,
            final Map<String, FieldDiff> mapCreated,
            final Map<String, FieldDiff> mapModified,
            final Map<String, FieldDiff> mapDeleted,
            final int diffType
    ) {

        final Map<String, FieldDiff> intoMap
        if (diffType == DIFF_TYPE_CREATED) {
            intoMap = mapCreated
        } else if (diffType == DIFF_TYPE_MODIFIED) {
            intoMap = mapModified
        } else if (diffType == DIFF_TYPE_DELETED) {
            intoMap = mapDeleted
        } else {
            throw new IllegalArgumentException("diffType")
        }

        //  Deletes must cascade
        for (FieldDiff fieldDiff in objectDiff.diffs) {
            if (fieldDiff instanceof ArrayDiff) {
                final ArrayDiff arrayDiff = (ArrayDiff) fieldDiff

                for (ObjectDiff modification : arrayDiff.modified) {
                    flattenDiffInto(modification, mapCreated, mapModified, mapDeleted, diffType == DIFF_TYPE_DELETED ? DIFF_TYPE_DELETED : DIFF_TYPE_MODIFIED)
                }
                for (ObjectDiff creation : (arrayDiff.created as List<ObjectDiff>)) {
                    flattenDiffInto(creation, mapCreated, mapModified, mapDeleted, diffType == DIFF_TYPE_DELETED ? DIFF_TYPE_DELETED : DIFF_TYPE_CREATED)
                }
                for (ObjectDiff deletion : (arrayDiff.deleted as List<ObjectDiff>)) {
                    flattenDiffInto(deletion, mapCreated, mapModified, mapDeleted, DIFF_TYPE_CREATED)
                }
            } else {
                flattenDiffIntoMap(fieldDiff, intoMap)
            }

        }
    }

    private static void flattenDiffIntoMap(final FieldDiff fieldDiff, final Map<String, FieldDiff> intoMap) {
        final String pathString

        if (fieldDiff.rhsDiffableItem != null) {
            pathString = fieldDiff.rhsDiffableItem.getDiffIdentifier()
        } else if (fieldDiff.lhsDiffableItem != null) {
            pathString = fieldDiff.lhsDiffableItem.getDiffIdentifier()
        } else {
            return
        }

        // Make the path's model identifier the same so paths from different
        // models are comparable
        final Path path = new Path(pathString)

        final Path.PathNode firstNode = path.nodes[0]
        firstNode.modelIdentifier = '--------'
        path.setNodes(path.nodes)

        intoMap.put(path.toString(), fieldDiff)
    }

    @Override
    PermissionsDTO permissions(UUID id) {

        M modelToUse = (M) modelContentRepository.findWithContentById(id)

        if (modelToUse == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found for permissions")
        }

        boolean readableByEveryone = modelToUse.readableByEveryone
        boolean readableByAuthenticatedUsers = modelToUse.readableByAuthenticatedUsers

        // TODO: Perhaps want to use SecurableResourceGroupRoleCacheableRepository to get the
        // list of SecurableResourceGroupRole
        Set<UserGroup> readableByGroups = [];
        Set<UserGroup> writeableByGroups = [];
        Set<CatalogueUser> readableByUsers = [];
        Set<CatalogueUser> writeableByUsers = [];

        PermissionsDTO permissions = new PermissionsDTO()
        permissions.readableByEveryone = readableByEveryone
        permissions.readableByAuthenticated = readableByAuthenticatedUsers
        permissions.readableByGroups = readableByGroups
        permissions.writeableByGroups = writeableByGroups
        permissions.readableByUsers = readableByUsers
        permissions.writeableByUsers = writeableByUsers

        return permissions
    }

    protected M getModelWithContent(UUID modelId) {
        M existing = modelContentRepository.findWithContentById(modelId)
        existing.setAssociations()
        existing
    }
}