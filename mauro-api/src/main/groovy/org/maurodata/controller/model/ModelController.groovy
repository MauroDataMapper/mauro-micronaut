package org.maurodata.controller.model

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.server.exceptions.InternalServerException
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.FieldConstants
import org.maurodata.api.model.FieldPatchDataDTO
import org.maurodata.api.model.MergeDiffDTO
import org.maurodata.api.model.MergeFieldDiffDTO
import org.maurodata.api.model.MergeIntoDTO
import org.maurodata.api.model.ModelApi
import org.maurodata.api.model.ModelRefDTO
import org.maurodata.api.model.ModelVersionDTO
import org.maurodata.api.model.ModelVersionedRefDTO
import org.maurodata.api.model.ModelVersionedWithTargetsRefDTO
import org.maurodata.api.model.ObjectPatchDataDTO
import org.maurodata.api.model.PermissionsDTO
import org.maurodata.api.model.VersionLinkDTO
import org.maurodata.api.model.VersionLinkTargetDTO
import org.maurodata.controller.facet.EditController
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.diff.ArrayDiff
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.FieldDiff
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelService
import org.maurodata.domain.model.Path
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.UserGroup
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.facet.VersionLinkRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository
import org.maurodata.persistence.model.AdministeredItemRepository
import org.maurodata.persistence.model.ModelContentRepository
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.exporter.ModelExporterPlugin
import org.maurodata.plugin.importer.FolderImporterPlugin
import org.maurodata.plugin.importer.ImportParameters
import org.maurodata.plugin.importer.ModelImporterPlugin
import org.maurodata.service.core.AuthorityService
import org.maurodata.service.model.ImportExportModelService
import org.maurodata.service.plugin.PluginService
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import java.lang.reflect.Method

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
    ObjectMapper objectMapper

    @Inject
    VersionLinkRepository versionLinkRepositoryUncached

    @Inject
    EditController editController

    @Inject
    List<AdministeredItemContentRepository> administeredItemContentRepositories

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
        administeredItemRepositories.find {it.handles(item.class) || it.handles(item.domainType)}
    }


    HttpResponse<byte[]> exportModel(UUID modelId, String namespace, String name, @Nullable String version) {
        ModelExporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelExporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)
        M existing = getModelWithContent(modelId)
        importExportModelService.createExportResponse(mauroPlugin, existing)
    }

    ListResponse<M> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {

        ModelImporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelImporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)

        ImportParameters importParameters = importExportModelService.readFromMultipartFormBody(body, mauroPlugin.importParametersClass())

        List<M> imported = (List<M>) mauroPlugin.importModels(importParameters)
        String importedFolder = importParameters.folderId ?: null
        Folder folder = null
        if (!importedFolder && [FieldConstants.FOLDER, FieldConstants.VERSIONED_FOLDER].disjoint(imported.domainType.unique())){
            ErrorHandler.handleError(HttpStatus.NOT_FOUND,  "Folder/VersionedFolder with id $importParameters.folderId not found")
        } else {
            if (importParameters.folderId) {
                folder = folderRepository.readById(importParameters.folderId)
                ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, folder, "Folder with id $importParameters.folderId not found")
                accessControlService.checkRole(Role.EDITOR, folder)
            }
        }
        List<M> saved = imported.collect {M imp ->
            imp.folder = folder
            log.info '** about to saveWithContentBatched... **'
            updateCreationProperties(imp)
            M savedImported = modelContentRepository.saveWithContent(imp)
            log.info '** finished saveWithContentBatched **'
            savedImported
        }
        List<M> smallerResponse = saved.collect {model ->
            show(model.id)
        }
        ListResponse.from(smallerResponse)
    }




    ListResponse<M> importModel(@Body io.micronaut.http.client.multipart.MultipartBody body, String namespace, String name, @Nullable String version) {
        throw new Exception("Client version of import model has been called.. hint client MultipartBody ")
    }

    protected void getReferenceFileFileContent(Collection<AdministeredItem> administeredItems) {
        administeredItems.each {
            if (it.referenceFiles) {
                it.referenceFiles.each {referenceFile ->
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
        ModelRefDTO targetModelDto = new ModelRefDTO(id: targetModel.id, domainType: targetModel.domainType, label: targetModel.label,
                                                     model: targetModel.owner ? targetModel.owner.id : null)

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

        final M chosenCommonAncestor = (finalisedLeftParent.modelVersion < finalisedRightParent.modelVersion) ? finalisedLeftParent : finalisedRightParent

        if (!isOnVersionPath(finalisedLeftParent, chosenCommonAncestor) || !isOnVersionPath(finalisedRightParent, chosenCommonAncestor)) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                          "MS04. Models don't share a common ancestor")
        }

        chosenCommonAncestor
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

    boolean isOnVersionPath(final M modelToFind, final M asAncestorOf) {
        M currentModel = asAncestorOf

        for (; ;) {
            if (currentModel == null) {
                break
            }
            final UUID currentId = currentModel.id
            if (currentId == modelToFind.id) {return true}

            final UUID sourceModelUUID = versionLinkRepositoryUncached.findSourceModel(currentId)

            if (sourceModelUUID != null) {
                currentModel = modelContentRepository.findWithContentById(sourceModelUUID)
                continue
            }
            break
        }

        return false
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

    Map<String, Map<String, FieldDiff>> flattenDiff(final ObjectDiff objectDiff, final String startingPathNodeString) {
        final Map<String, FieldDiff> mapCreated = new LinkedHashMap<>(50)
        final Map<String, FieldDiff> mapModified = new LinkedHashMap<>(50)
        final Map<String, FieldDiff> mapDeleted = new LinkedHashMap<>(50)

        // Make the assumption that the top level field diffs are modifications
        flattenDiffInto(objectDiff, mapCreated, mapModified, mapDeleted, DIFF_TYPE_MODIFIED, startingPathNodeString)

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
        final int diffType,
        final String startingPathNodeString
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
                    flattenDiffInto(modification, mapCreated, mapModified, mapDeleted, diffType == DIFF_TYPE_DELETED ? DIFF_TYPE_DELETED : DIFF_TYPE_MODIFIED,
                                    startingPathNodeString)
                }
                final Map<String, FieldDiff> intoMapCreation = diffType == DIFF_TYPE_DELETED ? mapDeleted : mapCreated
                for (CollectionDiff creation : arrayDiff.created as List<CollectionDiff>) {
                    flattenDiffIntoMap(creation, fieldDiff, intoMapCreation, startingPathNodeString)
                }
                for (CollectionDiff deletion : arrayDiff.deleted as List<CollectionDiff>) {
                    flattenDiffIntoMap(deletion, fieldDiff, mapDeleted, startingPathNodeString)
                }
            } else {
                flattenDiffIntoMap(fieldDiff, intoMap, startingPathNodeString)
            }
        }
    }

    private static void flattenDiffIntoMap(final FieldDiff fieldDiff, final Map<String, FieldDiff> intoMap, final String startingPathNodeString) {
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
        // Root the path starting from the startingPathNodeString as the context item
        final Path path = new Path(pathString).trimUntil(startingPathNodeString)

        final Path.PathNode firstNode = path.nodes[0]
        firstNode.modelIdentifier = '--------'
        path.updatePathString()

        intoMap.put(path.toString(), fieldDiff)
    }

    private static void flattenDiffIntoMap(final CollectionDiff collectionDiff, final FieldDiff fieldDiff, final Map<String, FieldDiff> intoMap,
                                           final String startingPathNodeString) {
        final String pathString = collectionDiff.diffIdentifier

        // Make the path's model identifier the same so paths from different
        // models are comparable
        // Root the path starting from the startingPathNodeString as the context item

        System.out.println("flattenDiffIntoMap " + pathString + " trimUntil " + startingPathNodeString)

        final Path path
        try {
            path = new Path(pathString).trimUntil(startingPathNodeString)
        }
        catch (IllegalArgumentException iae) {
            iae.printStackTrace()

            System.err.println(fieldDiff.toString())
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, iae.toString())
        }

        final Path.PathNode firstNode = path.nodes[0]
        firstNode.modelIdentifier = '--------'
        path.updatePathString()

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

    protected M saveDataModel(DataModel dataModel) {
        DataModel savedImport = modelContentRepository.saveWithContent(dataModel as M) as DataModel
        savedImport as M
    }

    protected M saveFolder(Folder folder) {
        Folder savedImport = modelContentRepository.saveWithContent(folder as M) as Folder
        savedImport as M
    }

    protected M saveCodeSet(CodeSet codeSet) {
        CodeSet savedImport = modelContentRepository.saveWithContent(codeSet as M) as CodeSet
        savedImport as M
    }

    protected M saveTerminology(Terminology terminology) {
        Terminology savedImport = modelContentRepository.saveWithContent(terminology as M) as Terminology
        savedImport as M
    }

    protected M saveModel(M model) {
        modelContentRepository.saveWithContent(model)
    }

    protected ModelVersionDTO latestModelVersion(UUID id) {
        final List<Model> allModels = populateVersionTree(id, false, null)

        if (allModels.size() == 0) {
            new ModelVersionDTO(modelVersion: FieldConstants.DEFAULT_MODEL_VERSION)
        }

        ModelVersion highestVersion = null

        for (int m = 0; m < allModels.size(); m++) {
            final Model givenModel = allModels.get(m)
            final ModelVersion modelVersion = givenModel.modelVersion
            if (modelVersion == null) {
                continue
            }

            if (highestVersion == null) {
                highestVersion = modelVersion
                continue
            }

            if (modelVersion > highestVersion) {
                highestVersion = modelVersion
            }
        }

        if (highestVersion == null) {
            log.warn("There are no versioned models for data model :$id")
        }

        return new ModelVersionDTO(modelVersion: highestVersion ?: ModelVersion.from(FieldConstants.DEFAULT_MODEL_VERSION))
    }

    protected ModelVersionedRefDTO latestFinalisedModel(UUID id) {
        final List<Model> allModels = populateVersionTree(id, false, null)

        if (allModels.size() == 0) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "There are no models")
        }

        Model highestVersionModel = null

        for (int m = 0; m < allModels.size(); m++) {
            final Model givenModel = allModels.get(m)
            final ModelVersion modelVersion = givenModel.modelVersion
            if (modelVersion == null) {
                continue
            }

            if (highestVersionModel == null) {
                highestVersionModel = givenModel
                continue
            }

            if (modelVersion > highestVersionModel.modelVersion) {
                highestVersionModel = givenModel
            }
        }

        if (highestVersionModel == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "There are no versioned models")
        }

        return new ModelVersionedRefDTO(id: highestVersionModel.id, domainType: highestVersionModel.domainType, label: highestVersionModel.label,
                                        branch: highestVersionModel.branchName, branchName: highestVersionModel.branchName,
                                        modelVersion: highestVersionModel.modelVersion?.toString(), modelVersionTag: highestVersionModel.modelVersionTag,
                                        documentationVersion: highestVersionModel.documentationVersion, displayName: highestVersionModel.pathModelIdentifier)
    }

    protected M commonAncestor(UUID id, UUID other_model_id) {
        final M left = show(id)
        if (!left) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "No data model found for id [${left.id.toString()}]")
        }
        final M right = show(other_model_id)
        if (!right) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "No data model found for id [${right.id.toString()}]")
        }

        return findCommonAncestorBetweenModels(left, right)
    }

    protected M currentMainBranch(UUID id) {

        // Like Highlander, there can be only one
        // main/draft version
        // and it downstream

        final List<Model> allModels = populateVersionTree(id, true, null)

        for (int m = allModels.size() - 1; m >= 0; m--) {
            final Model givenModel = allModels.get(m)
            if (givenModel.branchName == Model.DEFAULT_BRANCH_NAME && !givenModel.finalised) {
                return givenModel as M
            }
        }

        throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "There is no main draft model")
    }

    protected List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly) {

        branchesOnly = branchesOnly ?: false

        final List<Model> allModels = populateVersionTree(id, branchesOnly, null)

        // Create object DTOs

        final ArrayList<ModelVersionedRefDTO> simpleModelVersionTreeList = new ArrayList<>(allModels.size())

        allModels.forEach {Model model ->

            if (!branchesOnly || !model.finalised) {
                simpleModelVersionTreeList.add(
                    new ModelVersionedRefDTO(id: model.id, domainType: model.domainType, label: model.label, model: model.owner.id, branch: model.branchName,
                                             branchName: model.branchName,
                                             modelVersion: model.modelVersion?.toString(), modelVersionTag: model.modelVersionTag,
                                             documentationVersion: model.documentationVersion,
                                             displayName: model.pathModelIdentifier))
            }
        }

        simpleModelVersionTreeList
    }

    protected List<ModelVersionedWithTargetsRefDTO> modelVersionTree(UUID id) {

        final Map<UUID, Map<String, Boolean>> flags = [:]
        final List<Model> allModels = populateVersionTree(id, false, flags)

        // Create object DTOs

        final ArrayList<ModelVersionedWithTargetsRefDTO> modelVersionTreeList = new ArrayList<>(allModels.size())

        for (Model model : allModels) {
            final ModelVersionedWithTargetsRefDTO modelVersionedWithTargetsRefDTO = new ModelVersionedWithTargetsRefDTO(id: model.id, branch: model.branchName,
                                                                                                                        branchName: model.branchName,
                                                                                                                        modelVersion: model.modelVersion?.toString(),
                                                                                                                        modelVersionTag: model.modelVersionTag,
                                                                                                                        documentationVersion: model.documentationVersion,
                                                                                                                        displayName: model.pathModelIdentifier,
                                                                                                                        domainType: model.domainType)

            // Have any flags been set during recursion?
            final Map<String, Boolean> modelFlags = flags.get(modelVersionedWithTargetsRefDTO.id)

            if (modelFlags != null) {
                Boolean isNewBranchModelVersion = modelFlags.get("isNewBranchModelVersion")
                Boolean isNewFork = modelFlags.get("isNewFork")

                if (isNewBranchModelVersion != null && isNewBranchModelVersion) {
                    modelVersionedWithTargetsRefDTO.isNewBranchModelVersion = true
                } else if (isNewFork != null && isNewFork) {
                    modelVersionedWithTargetsRefDTO.isNewFork = true
                }
            }

            // Add in targets

            if (model.versionLinks != null && !model.versionLinks.isEmpty()) {
                for (VersionLink childVersion : model.versionLinks) {
                    final UUID targetModelId = childVersion.targetModelId
                    if (targetModelId == null) {
                        continue
                    }

                    final VersionLinkTargetDTO versionLinkTargetDTO = new VersionLinkTargetDTO(id: targetModelId, description: childVersion.description)

                    modelVersionedWithTargetsRefDTO.targets.add(versionLinkTargetDTO)
                }
            }

            modelVersionTreeList.add(modelVersionedWithTargetsRefDTO)
        }

        modelVersionTreeList
    }

    protected MergeDiffDTO mergeDiff(@NonNull UUID id, @NonNull UUID otherId) {
        final M dataModelOne = modelContentRepository.findWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataModelOne, "item with $id not found")

        final M dataModelTwo = modelContentRepository.findWithContentById(otherId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataModelTwo, "item with $otherId not found")

        accessControlService.checkRole(Role.READER, dataModelOne)
        accessControlService.checkRole(Role.READER, dataModelTwo)

        // The model to merge into must not be finalised
        if (dataModelTwo.finalised) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MS03 The target model must not be finalised")
        }

        final M commonAncestor = findCommonAncestorBetweenModels(dataModelOne, dataModelTwo)

        dataModelOne.setAssociations()
        dataModelTwo.setAssociations()
        commonAncestor.setAssociations()

        connectFacets(commonAncestor)
        connectFacets(dataModelOne)
        connectFacets(dataModelTwo)

        pathRepository.readParentItems(dataModelOne)
        dataModelOne.updatePath()

        pathRepository.readParentItems(dataModelTwo)
        dataModelTwo.updatePath()

        // For a merge, there are two diffs between the common ancestor and model being merged

        final ObjectDiff objectDiffOne = commonAncestor.diff(dataModelOne)
        final ObjectDiff objectDiffTwo = commonAncestor.diff(dataModelTwo)

        // Remove branchName differences since these are merges of branches
        for (ObjectDiff objectDiff : ([objectDiffOne, objectDiffTwo] as List<ObjectDiff>)) {
            final ArrayList<FieldDiff> diffsToRemove = new ArrayList<>(2)
            for (fieldDiff in objectDiff.diffs) {
                if (fieldDiff.name == 'branchName') {
                    diffsToRemove.add(fieldDiff)
                }
            }
            objectDiff.diffs.removeAll(diffsToRemove)
        }

        // recurse down through each diff and create a map of Path-> FieldDiff

        Map<String, Map<String, FieldDiff>> flattenedDiffOne = flattenDiff(objectDiffOne, dataModelOne.pathNodeString)
        Map<String, Map<String, FieldDiff>> flattenedDiffTwo = flattenDiff(objectDiffTwo, dataModelTwo.pathNodeString)

        // Collate the set of all paths used as keys

        final Map<String, FieldDiff> flattenedDiffOneCreated = flattenedDiffOne.get('created')
        final Map<String, FieldDiff> flattenedDiffOneModified = flattenedDiffOne.get('modified')
        final Map<String, FieldDiff> flattenedDiffOneDeleted = flattenedDiffOne.get('deleted')

        final Map<String, FieldDiff> flattenedDiffTwoCreated = flattenedDiffTwo.get('created')
        final Map<String, FieldDiff> flattenedDiffTwoModified = flattenedDiffTwo.get('modified')
        final Map<String, FieldDiff> flattenedDiffTwoDeleted = flattenedDiffTwo.get('deleted')

        // Remove changes from the common ancestor that are common to both models

        flattenedDiffTwoCreated.keySet().forEach {
            flattenedDiffOneCreated.remove(it)
        }

        flattenedDiffTwoDeleted.keySet().forEach {
            flattenedDiffOneDeleted.remove(it)
        }

        // Remove any 'creations' that are already in the target model

        flattenedDiffOneCreated.keySet().forEach {
            flattenedDiffTwoCreated.remove(it)
        }

        final List<Map<String, FieldDiff>> flattenedDiffOneMapList = [flattenedDiffOneCreated, flattenedDiffOneModified, flattenedDiffOneDeleted]
        final List<Map<String, FieldDiff>> flattenedDiffTwoMapList = [flattenedDiffTwoCreated, flattenedDiffTwoModified, flattenedDiffTwoDeleted]

        final Set<String> allPaths = new LinkedHashSet<>()
        for (Map<String, FieldDiff> pathToFieldDiff : flattenedDiffOneMapList + flattenedDiffTwoMapList) {
            final Set keys = pathToFieldDiff.keySet()
            allPaths.addAll(keys)
        }

        List<Path.PathNode> pathNodes = []

        AdministeredItem node = dataModelOne
        pathNodes.add(0, new Path.PathNode(prefix: node.pathPrefix, identifier: node.pathIdentifier, modelIdentifier: node.pathModelIdentifier))

        final Path path = new Path(pathNodes)
        final String sourcePath = path.toString()

        final List<MergeFieldDiffDTO> diffs = []
        final MergeDiffDTO mergeDiffDTO = new MergeDiffDTO(sourceId: dataModelOne.id, targetId: dataModelTwo.id, path: sourcePath, label: dataModelOne.label, diffs: diffs)

        // With ancestor = common ancestor, one = source of merge, two = target of merge
        // The conflicts are detected like this:

        // 1. Deletion conflict rules
        // deletions are a conflict when a deletion from one is not matched by a deletion in two
        // All descendant paths must be flagged too

        // 2. Modification conflict rules
        // for each key, compare what needs to be done to merge
        // modifications are only in conflict when the value of the ancestor, branch one, branch two all have
        // different values e.g. there are 3 distinct values
        // For each distinct path, look up the value held in each of the three places and add it to a set
        // if the set has 3 values, there is a conflict, otherwise it a simple change

        final String targetPrefix = dataModelTwo.pathPrefix
        final String targetPathIdentifier = dataModelTwo.pathIdentifier
        final String targetPathModelIdentifier = dataModelTwo.pathModelIdentifier

        // Deletion conflicts
        // For all deletions in one, find all descendants are not deleted in two

        final String[] flattenedDiffOneDeletedPaths = flattenedDiffOneDeleted.keySet().toArray(new String[0])
        final String[] flattenedDiffTwoDeletedPaths = flattenedDiffTwoDeleted.keySet().toArray(new String[0])
        final String[] flattenedDiffTwoCreatedPaths = flattenedDiffTwoCreated.keySet().toArray(new String[0])
        final String[] flattenedDiffTwoModifiedPaths = flattenedDiffTwoModified.keySet().toArray(new String[0])

        List<String> haveDeleted = []
        for (String deletionInOne : flattenedDiffOneDeletedPaths) {
            boolean deletedAsWell = false
            for (String deletionInTwo : flattenedDiffTwoDeletedPaths) {
                if (deletionInOne == deletionInTwo) {
                    deletedAsWell = true
                    break
                }
            }

            if (!deletedAsWell) {

                // Deletion / existence conflict

                if (!haveDeleted.contains(deletionInOne)) {

                    allPaths.remove(deletionInOne)

                    final FieldDiff conflictingFieldDiff = flattenedDiffOneDeleted.get(deletionInOne)
                    final String fieldName = conflictingFieldDiff.name

                    Path ancestorBasedPath = new Path(deletionInOne)
                    ancestorBasedPath.nodes.get(0).tap {
                        prefix = targetPrefix
                        identifier = targetPathIdentifier
                        modelIdentifier = targetPathModelIdentifier
                    }
                    ancestorBasedPath = new Path(ancestorBasedPath.nodes)

                    final MergeFieldDiffDTO mergeFieldDiffDTO = new MergeFieldDiffDTO(fieldName: fieldName, sourceValue: null, targetValue: null,
                                                                                      commonAncestorValue: null, isMergeConflict: true, _type: "deletion",
                                                                                      path: ancestorBasedPath)
                    diffs.add(mergeFieldDiffDTO)
                }

                // Find descendants that would be deleted
                for (String otherDeletionInOne : flattenedDiffOneDeletedPaths) {
                    if (otherDeletionInOne.startsWith(deletionInOne + '|') && otherDeletionInOne != deletionInOne) {

                        final FieldDiff conflictingFieldDiff = flattenedDiffOneDeleted.get(otherDeletionInOne)
                        final String fieldName = conflictingFieldDiff.name

                        String descendantFragment = otherDeletionInOne.substring(deletionInOne.length())

                        StringTokenizer st = new StringTokenizer(descendantFragment, '|')
                        String descendantPath = deletionInOne
                        while (st.hasMoreTokens()) {

                            descendantPath += '|' + st.nextToken()

                            // Done it already
                            if (haveDeleted.contains(descendantPath)) {
                                continue
                            }
                            // Will be done later
                            if (st.hasMoreTokens() && allPaths.contains(descendantPath)) {
                                continue
                            }

                            allPaths.remove(descendantPath)
                            haveDeleted.add(descendantPath)

                            Path ancestorBasedPath = new Path(descendantPath)
                            ancestorBasedPath.nodes.get(0).tap {
                                prefix = targetPrefix
                                identifier = targetPathIdentifier
                                modelIdentifier = targetPathModelIdentifier
                            }
                            ancestorBasedPath = new Path(ancestorBasedPath.nodes)

                            final MergeFieldDiffDTO mergeFieldDiffDTO = new MergeFieldDiffDTO(fieldName: fieldName, sourceValue: null, targetValue: null,
                                                                                              commonAncestorValue: null, isMergeConflict: true, _type: "deletion",
                                                                                              path: ancestorBasedPath)
                            diffs.add(mergeFieldDiffDTO)
                        }
                    }
                }
                for (String modificationInTwo : flattenedDiffTwoModifiedPaths) {
                    if (modificationInTwo.startsWith(deletionInOne + '|')) {

                        final FieldDiff conflictingFieldDiff = flattenedDiffTwoModified.get(modificationInTwo)
                        final String fieldName = conflictingFieldDiff.name

                        String descendantFragment = modificationInTwo.substring(deletionInOne.length())

                        StringTokenizer st = new StringTokenizer(descendantFragment, '|')
                        String descendantPath = deletionInOne
                        while (st.hasMoreTokens()) {

                            descendantPath += '|' + st.nextToken()

                            // Done it already
                            if (haveDeleted.contains(descendantPath)) {
                                continue
                            }
                            // Will be done later
                            if (st.hasMoreTokens() && allPaths.contains(descendantPath)) {
                                continue
                            }

                            allPaths.remove(descendantPath)
                            haveDeleted.add(descendantPath)

                            Path ancestorBasedPath = new Path(descendantPath)
                            ancestorBasedPath.nodes.get(0).tap {
                                prefix = targetPrefix
                                identifier = targetPathIdentifier
                                modelIdentifier = targetPathModelIdentifier
                            }
                            ancestorBasedPath = new Path(ancestorBasedPath.nodes)

                            final MergeFieldDiffDTO mergeFieldDiffDTO = new MergeFieldDiffDTO(fieldName: fieldName, sourceValue: null, targetValue: null,
                                                                                              commonAncestorValue: null, isMergeConflict: true, _type: "deletion",
                                                                                              path: ancestorBasedPath)
                            diffs.add(mergeFieldDiffDTO)
                        }
                    }
                }
                for (String creationInTwo : flattenedDiffTwoCreatedPaths) {
                    if (creationInTwo.startsWith(deletionInOne + '|')) {

                        final FieldDiff conflictingFieldDiff = flattenedDiffTwoCreated.get(creationInTwo)
                        final String fieldName = conflictingFieldDiff.name

                        String descendantFragment = creationInTwo.substring(deletionInOne.length())

                        StringTokenizer st = new StringTokenizer(descendantFragment, '|')
                        String descendantPath = deletionInOne
                        while (st.hasMoreTokens()) {

                            descendantPath += '|' + st.nextToken()

                            // Done it already
                            if (haveDeleted.contains(descendantPath)) {
                                continue
                            }
                            // Will be done later
                            if (st.hasMoreTokens() && allPaths.contains(descendantPath)) {
                                continue
                            }

                            allPaths.remove(descendantPath)
                            haveDeleted.add(descendantPath)

                            Path ancestorBasedPath = new Path(descendantPath)
                            ancestorBasedPath.nodes.get(0).tap {
                                prefix = targetPrefix
                                identifier = targetPathIdentifier
                                modelIdentifier = targetPathModelIdentifier
                            }
                            ancestorBasedPath = new Path(ancestorBasedPath.nodes)

                            final MergeFieldDiffDTO mergeFieldDiffDTO = new MergeFieldDiffDTO(fieldName: fieldName, sourceValue: null, targetValue: null,
                                                                                              commonAncestorValue: null, isMergeConflict: true, _type: "deletion",
                                                                                              path: ancestorBasedPath)
                            diffs.add(mergeFieldDiffDTO)
                        }
                    }
                }
            }
        }

        // Changes
        for (String key : allPaths) {
            FieldDiff fieldDiffOne = null
            String _type = 'modification'

            lookingInOne:
            for (Map<String, FieldDiff> pathToFieldDiff : flattenedDiffOneMapList) {
                final foundFieldDiff = pathToFieldDiff.get(key)
                if (foundFieldDiff != null) {
                    fieldDiffOne = foundFieldDiff
                    if (pathToFieldDiff == flattenedDiffOneCreated) {
                        _type = 'creation'
                    } else if (pathToFieldDiff == flattenedDiffOneDeleted) {
                        _type = 'deletion'
                    }
                    break lookingInOne
                }
            }

            FieldDiff fieldDiffTwo = null
            lookingInTwo:
            for (Map<String, FieldDiff> pathToFieldDiff : flattenedDiffTwoMapList) {
                final foundFieldDiff = pathToFieldDiff.get(key)
                if (foundFieldDiff != null) {
                    fieldDiffTwo = foundFieldDiff
                    break lookingInTwo
                }
            }

            // Find the common ancestor version
            // In the FieldDiff lhs is the value object from the commonAncestor, rhs is a branch

            final Object fieldOneValue, fieldTwoValue, ancestorValue
            final String fieldName

            // Establish the ancestorValue first, since it is the backstop value

            if (fieldDiffOne != null) {
                ancestorValue = fieldDiffOne.left
                fieldName = fieldDiffOne.name
            } else if (fieldDiffTwo != null) {
                ancestorValue = fieldDiffTwo.left
                fieldName = fieldDiffTwo.name
            } else {
                ancestorValue = null
                fieldName = null
            }

            // If there is a change, record the value
            // or else it is the same as the ancestor value

            if (fieldDiffOne != null) {
                fieldOneValue = fieldDiffOne.right
            } else {
                fieldOneValue = ancestorValue
            }

            if (fieldDiffTwo != null) {
                fieldTwoValue = fieldDiffTwo.right
            } else {
                fieldTwoValue = ancestorValue
            }

            // Changes to do with versioning itself are not wanted here
            if (fieldName == 'finalised' || fieldName == 'dateFinalised' || fieldName == 'pathModelIdentifier' || fieldName == 'modelVersionTag') {
                continue
            }

            // If a fieldValueOne or fieldValueTwo value do not exist and the ancestor value does, they are
            // no different from the ancestor value
            final HashSet<Object> values = [fieldOneValue, fieldTwoValue, ancestorValue]

            final boolean isMergeConflict = values.size() == 3

            // Because the target branch is branch two, we are only interested in
            // changes or conflicts coming from the source branch (branch one)
            // anything that is not a conflict from branch two is a no-op
            //                 | ---------------(Branch Two)-->
            //  (ancestor) --> | ---(Branch One)----^
            // The diffs are replayed from the common ancestor, on top of the target branch, so paths must be in terms of the target branch
            // There's a side effect of changing a label: paths change

            if (!isMergeConflict && fieldDiffOne == null) {
                continue
            }

            // Is it actually a change in the target?
            if (fieldOneValue != null && fieldTwoValue != null && fieldOneValue == fieldTwoValue) {
                continue
            }

            Path ancestorBasedPath = new Path(key)
            ancestorBasedPath.nodes.get(0).tap {
                prefix = targetPrefix
                identifier = targetPathIdentifier
                modelIdentifier = targetPathModelIdentifier
            }
            ancestorBasedPath = new Path(ancestorBasedPath.nodes)

            final MergeFieldDiffDTO mergeFieldDiffDTO = new MergeFieldDiffDTO(fieldName: fieldName, sourceValue: fieldOneValue, targetValue: fieldTwoValue,
                                                                              commonAncestorValue: ancestorValue, isMergeConflict: isMergeConflict, _type: _type,
                                                                              path: ancestorBasedPath)

            diffs.add(mergeFieldDiffDTO)
        }
        return mergeDiffDTO
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    protected M mergeInto(@NonNull UUID id, @NonNull UUID otherId, @Body @Nullable MergeIntoDTO mergeIntoDTO) {
        if (mergeIntoDTO.patch.sourceId != id) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Source model id passed in request body does not match source model id in URI.')
        }
        if (mergeIntoDTO.patch.targetId != otherId) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Target model id passed in request body does not match target model id in URI.')
        }

        M sourceModel = modelContentRepository.findWithContentById(id)
        if (sourceModel == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, id.toString())
        }

        M targetModel = modelContentRepository.findWithContentById(otherId)
        if (targetModel == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, targetModel.toString())
        }

        // The model to merge into must not be finalised
        if (targetModel.finalised) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "MS03 The target model must not be finalised")
        }

        accessControlService.checkRole(Role.READER, sourceModel)
        accessControlService.checkRole(Role.EDITOR, targetModel)

        // Do merge here
        // SEE: grails ModelService.mergeObjectPatchDataIntoModel

        final ObjectPatchDataDTO objectPatchDataDTO = mergeIntoDTO.patch

        if (objectPatchDataDTO.patches == null || objectPatchDataDTO.patches.size() == 0) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'No patch data to merge into ' + targetModel.id)
        }

        final String changeNotice = mergeIntoDTO.changeNotice

        final Map<UUID, Boolean> addedChangeNotice = [:]

        if (changeNotice) {
            // add change notice edit for current user
            editController.createEdit(targetModel, EditType.CHANGENOTICE, changeNotice)
            addedChangeNotice.put(targetModel.id, true)
        }

        List<FieldPatchDataDTO> sortedFieldPatchDataForMerging = getSortedFieldPatchDataForMerging(objectPatchDataDTO)

        sortedFieldPatchDataForMerging.each {fieldPatch ->
            switch (fieldPatch._type) {
                case 'creation':
                    return processCreationPatchIntoModel(fieldPatch, targetModel, sourceModel, changeNotice, addedChangeNotice)
                case 'deletion':
                    return
                case 'modification':
                    return processModificationPatchIntoModel(fieldPatch, targetModel, sourceModel, changeNotice, addedChangeNotice)
                default:
                    System.err.println('Unknown field patch type ' + fieldPatch._type)
            }
        }

        List<FieldPatchDataDTO> sortedFieldPatchDataForMergingDeletion = getSortedFieldPatchDataForMergingDeletion(objectPatchDataDTO)

        sortedFieldPatchDataForMergingDeletion.reverse().each {fieldPatch ->
            switch (fieldPatch._type) {
                case 'deletion':
                    return processDeletionPatchIntoModel(fieldPatch, targetModel, sourceModel, changeNotice, addedChangeNotice)
            }
        }

        if (mergeIntoDTO.deleteBranch) {

            // TODO:
            // Check permissions
            // delete the model
        }

        administeredItemRepository.update(targetModel)

        targetModel
    }

    List<FieldPatchDataDTO> getSortedFieldPatchDataForMerging(ObjectPatchDataDTO objectPatchData) {
        /*
          We have to process modifications in after everything else incase the modifications require something to have been created
          Process creations before deletions, that way any deletions will automatically take care of any links to potentially created objects
           */
        objectPatchData.patches.sort {l, r ->
            if (l._type == r._type) {return 0}
            return l <=> r
        }
    }

    List<FieldPatchDataDTO> getSortedFieldPatchDataForMergingDeletion(ObjectPatchDataDTO objectPatchData) {
        /*
            We process leaves first and delete towards the root
         */
        objectPatchData.patches.sort {l, r ->
            return l.path.toString() <=> r.path.toString()
        }
    }

    void processCreationPatchIntoModel(FieldPatchDataDTO creationPatch, M targetModel, M sourceModel, final String changeNotice, final Map<UUID, Boolean> addedChangeNotice) {

        // Check whether the target path already exists - it is possible that it has been created by re-referencing during creation
        // of reference types, and that would not be an error
        try {
            AdministeredItem checkForExistenceAdministeredItemTarget = pathRepository.findResourcesByPathFromRootResource(targetModel, creationPatch.path)
            if (checkForExistenceAdministeredItemTarget != null) {
                // no op
                return
            }
        }
        catch (Exception ignored) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create path for ${creationPatch.path.toString()}")
        }

        // Find the Item referenced by the creationPatch in the source model
        // and the parent item to create it in, in the target model using their paths

        // findResourcesByPathFromRootResource throws an exception if the path is not found
        // The source path
        final AdministeredItem administeredItemSource = getWithContents(pathRepository.findResourcesByPathFromRootResource(sourceModel, creationPatch.path))

        // The target parent path
        AdministeredItem administeredItemTargetParent = null

        try {
            administeredItemTargetParent = pathRepository.findResourcesByPathFromRootResource(targetModel, creationPatch.path.parent)

            if (administeredItemTargetParent == null) {
                processCreationPatchIntoModel(creationPatch.getParentPathPatch(), targetModel, sourceModel, changeNotice, addedChangeNotice)
                // The target's parent should have been created by this point, if not this will throw an error
                try {
                    administeredItemTargetParent = pathRepository.findResourcesByPathFromRootResource(targetModel, creationPatch.path.parent)
                }
                catch (Exception ignoredAgain) {
                    throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create path for ${creationPatch.path.toString()}")
                }
                if (administeredItemTargetParent == null) {
                    throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create path for ${creationPatch.path.toString()}")
                }
            }
        }
        catch (Exception ignored) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create path for ${creationPatch.path.toString()}")
        }

        // Make a copy of the AdministeredItem from the source, then attach the copy to the target parent
        final AdministeredItem clonedAdministeredItem = administeredItemSource.clone()

        // reset any properties to do with versioning or branches etc and, of course, the Id as it will
        // need a new one

        if (clonedAdministeredItem instanceof Model) {
            final Model clonedAdministeredItemAsModel = (Model) clonedAdministeredItem

            clonedAdministeredItemAsModel.finalised = false
            clonedAdministeredItemAsModel.dateFinalised = null
            clonedAdministeredItemAsModel.modelVersion = null
            clonedAdministeredItemAsModel.modelVersionTag = null
            clonedAdministeredItemAsModel.branchName = sourceModel.branchName
            clonedAdministeredItemAsModel.versionLinks = []
        }

        clonedAdministeredItem.updateCreationProperties()
        clonedAdministeredItem.catalogueUser = accessControlService.getUser()
        clonedAdministeredItem.parent = administeredItemTargetParent

        // Make an initial save here, so that child items may have something to reference as a parent

        pathRepository.readParentItems(clonedAdministeredItem)
        clonedAdministeredItem.updatePath()
        clonedAdministeredItem.updateBreadcrumbs()
        AdministeredItemRepository simpleRepositoryToUse = pathRepository.getRepository(clonedAdministeredItem)
        final AdministeredItem savedClonedAdministeredItem = (AdministeredItem) simpleRepositoryToUse.save(clonedAdministeredItem)

        /*
         Is this clonedAdministeredItem referencing something within the scope of its own 'versionable' (Versioned folder, Data model)?
         If so, it must reference an equivalent in the target model rather than the reference it has to a source item

         These kinds of things that reference other things must implement ItemReferencer.

         E.g.
         DataType references -> Model, DataClass
         SemanticLink relations
         CodeSet
         VersionLink
         DataClasses can extend other classes

         */

        if (savedClonedAdministeredItem instanceof ItemReferencer) {

            // This works by asking ItemReferencer for the list of items that it references
            // Using the path, ask whether the item being referenced is within the scope
            // of what is being merged
            // If so, clone those and ask the ItemReferencer to update its references to the
            // cloned ones
            ItemReferencer itemReferencer = (ItemReferencer) savedClonedAdministeredItem
            List<ItemReference> referencedItems = itemReferencer.itemReferences

            // Resolve these to a list of paths
            List<Path> pathsToReferencedItems = pathRepository.resolveItemReferences(referencedItems)

            // Are any of these paths inside the source model?
            final Map<UUID, ItemReference> toReplace = [:]
            for (int p = 0; p < pathsToReferencedItems.size(); p++) {
                Path pathToReferencedItem = pathsToReferencedItems.get(p)

                Path pathToReferencedItemFromSource = pathToReferencedItem.trimUntil(sourceModel.pathNodeString)

                // This is the source item if found
                Item item = pathToReferencedItem.findNodeItem(sourceModel.id)

                if (item != null) {

                    // Yes, this reference is the source model
                    // It is in the target model?

                    Path pathToReferencedItemFromTarget = pathToReferencedItem.trimUntil(targetModel.pathNodeString)

                    //System.out.println("targetModel.pathNodeString "+targetModel.pathNodeString)
                    //System.out.println("pathToReferencedItemFromTarget "+pathToReferencedItemFromTarget.toString())

                    AdministeredItem targetToReferencedItem
                    try {
                        targetToReferencedItem = pathRepository.findResourcesByPathFromRootResource(targetModel, pathToReferencedItemFromTarget)

                        if (targetToReferencedItem == null) {
                            // No? Needs to be created then
                            processCreationPatchIntoModel(new FieldPatchDataDTO(path: pathToReferencedItemFromSource, _type: creationPatch._type), targetModel,
                                                          sourceModel, changeNotice, addedChangeNotice)
                            try {
                                targetToReferencedItem = pathRepository.findResourcesByPathFromRootResource(targetModel, pathToReferencedItemFromTarget)
                            }
                            catch (Exception ignoredOnceMore) {
                                ignoredOnceMore.printStackTrace()
                                throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create path for ${pathToReferencedItemFromTarget.toString()}")
                            }
                            if (targetToReferencedItem == null) {
                                throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create path for ${pathToReferencedItemFromTarget.toString()}")
                            }
                        }
                    }
                    catch (Exception ignored) {
                        ignored.printStackTrace()
                        throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create path for ${pathToReferencedItemFromTarget.toString()}")
                    }

                    // Resolve the source item from the path so it can be swapped out
                    AdministeredItem sourceOfReferencedItem

                    try {
                        sourceOfReferencedItem = pathRepository.findResourcesByPathFromRootResource(sourceModel, pathToReferencedItemFromSource)
                        if (sourceOfReferencedItem == null) {
                            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find path for ${pathToReferencedItemFromSource.toString()}")
                        }
                    }
                    catch (Exception ignored) {
                        throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find path for ${pathToReferencedItemFromSource.toString()}")
                    }

                    // Record a map of the ItemReferences the ItemReferencer will need to update to use the new reference
                    toReplace.put(sourceOfReferencedItem.id, ItemReference.from(targetToReferencedItem))
                }
            }

            // Ask the ItemReferencer to update to use the cloned items
            itemReferencer.replaceItemReferences(toReplace)

            // Then for each item, call update

            toReplace.values().forEach {ItemReference itemReference ->

                if (itemReference.theItem && itemReference.theItem instanceof AdministeredItem) {
                    AdministeredItemCacheableRepository administeredItemCacheableRepository =
                        administeredItemContentRepository.getRepository((AdministeredItem) itemReference.theItem)
                    administeredItemCacheableRepository.update((AdministeredItem) itemReference.theItem)
                }
            }
        }

        // Record edits
        // Put a change notice once up front for all local changes
        if (changeNotice && !addedChangeNotice.get(administeredItemTargetParent.id)) {
            editController.createEdit(administeredItemTargetParent, EditType.CHANGENOTICE, changeNotice)
            addedChangeNotice.put(administeredItemTargetParent.id, true)
        }
        // Record the change locally and globally
        String mergeEditDescription = "Item '$creationPatch.path' created in '$targetModel.label\$$targetModel.branchName'"
        editController.createEdit(administeredItemTargetParent, EditType.MERGE, mergeEditDescription)
        editController.createEdit(targetModel, EditType.MERGE, mergeEditDescription)
    }

    void processDeletionPatchIntoModel(FieldPatchDataDTO deletionPatch, M targetModel, M sourceModel, final String changeNotice, final Map<UUID, Boolean> addedChangeNotice) {

        // Find the Item in the target model

        final AdministeredItem administeredItem

        try {
            administeredItem = pathRepository.findResourcesByPathFromRootResource(targetModel, deletionPatch.path)
            if (administeredItem == null) {
                // Doesn't exist. That's ok because we don't want it to exist
                return
            }
        }
        catch (Exception findingResource) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find path for ${deletionPatch.path.toString()}")
        }

        String itemRemovedLabel = administeredItem.label ?: "No label"
        String mergeEditSuffix = itemRemovedLabel ?: administeredItem.domainType ?: ""

        // Delete the item
        // Use the deletes from controllers where possible


        deleteAdministeredItem(administeredItem)

        // Record edits

        AdministeredItem parent = administeredItem.parent

        // Put a change notice once up front for all local changes
        if (changeNotice && parent != null && !addedChangeNotice.get(parent.id)) {
            editController.createEdit(parent, EditType.CHANGENOTICE, changeNotice)
            addedChangeNotice.put(parent.id, true)
        }
        // Record the change locally and globally
        String mergeEditDescription = "Item removed from '$sourceModel.label\$$sourceModel.branchName' - $mergeEditSuffix"
        if (parent != null) {
            editController.createEdit(parent, EditType.MERGE, mergeEditDescription)
        }
        editController.createEdit(targetModel, EditType.MERGE, mergeEditDescription)
    }

    void processModificationPatchIntoModel(FieldPatchDataDTO modificationPatch, M targetModel, M sourceModel, final String changeNotice,
                                           final Map<UUID, Boolean> addedChangeNotice) {

        // Find the Item in the target model

        // This throws an exception if the path is not found
        // The source path
        final AdministeredItem administeredItemSource = pathRepository.findResourcesByPathFromRootResource(sourceModel, modificationPatch.path)
        // The target path
        final AdministeredItem administeredItemTarget = pathRepository.findResourcesByPathFromRootResource(targetModel, modificationPatch.path)

        final String fieldNameToModify = modificationPatch.fieldName

        if (
            administeredItemSource[fieldNameToModify] instanceof Number ||
            administeredItemSource[fieldNameToModify] instanceof String ||
            administeredItemSource[fieldNameToModify] instanceof Boolean ||
            administeredItemSource[fieldNameToModify].getClass().isEnum()
        ) {
            administeredItemTarget[fieldNameToModify] = administeredItemSource[fieldNameToModify]
        } else if (administeredItemSource[fieldNameToModify].getClass().getMethod("clone")) {
            try {
                Method method = administeredItemSource[fieldNameToModify].getClass().getMethod("clone")
                administeredItemTarget[fieldNameToModify] = method.invoke(administeredItemSource[fieldNameToModify])
            } catch (Exception e) {
                administeredItemTarget[fieldNameToModify] = administeredItemSource[fieldNameToModify]
            }
        } else {
            administeredItemTarget[fieldNameToModify] = administeredItemSource[fieldNameToModify]
        }

        if (changeNotice && !addedChangeNotice.get(administeredItemTarget.id)) {
            editController.createEdit(administeredItemTarget, EditType.CHANGENOTICE, changeNotice)
            addedChangeNotice.put(administeredItemTarget.id, true)
        }
        // Record the change locally and globally
        String mergeEditDescription = "Item modified in '$targetModel.label\$$targetModel.branchName'"

        editController.createEdit(administeredItemTarget, EditType.MERGE, mergeEditDescription)
        editController.createEdit(targetModel, EditType.MERGE, mergeEditDescription)

        AdministeredItemRepository specificAdministeredItemRepository = pathRepository.getRepository(administeredItemTarget)

        specificAdministeredItemRepository.update(administeredItemTarget)
    }

    AdministeredItem getWithContents(AdministeredItem item) {
        AdministeredItemContentRepository specificAdministeredItemContentRepository = getAdministeredItemContentRepository(item)
        AdministeredItem itemWithContents = specificAdministeredItemContentRepository.readWithContentById(item.id)
        return itemWithContents
    }

    void deleteAdministeredItem(AdministeredItem item) throws HttpException {
        if (item == null) {
            throw new InternalServerException("Null passed for deletion")
        }

        try {
            if (item instanceof Model) {
                accessControlService.checkRole(Role.CONTAINER_ADMIN, item)
            } else {
                deletePre(item)
                accessControlService.checkRole(Role.EDITOR, item)
            }

            AdministeredItemContentRepository specificAdministeredItemContentRepository = getAdministeredItemContentRepository(item)

            AdministeredItem itemWithContents = specificAdministeredItemContentRepository.readWithContentById(item.id)

            if (!specificAdministeredItemContentRepository.deleteWithContent(itemWithContents)) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion: ' + item.label)
            }
        }
        catch (Throwable th) {
            if (th instanceof HttpException) {
                throw (HttpException) th
            }
            throw new InternalServerException("Deletion failed", th)
        }
    }

    @NonNull
    AdministeredItemContentRepository getAdministeredItemContentRepository(AdministeredItem item) {
        administeredItemContentRepositories.find {
            it.getClass().simpleName != 'AdministeredItemContentRepository' &&
            it.getClass().simpleName != 'ModelContentRepository' &&
            it.handles(item.class)
        } ?:
        administeredItemContentRepositories.find {
            it.getClass().simpleName != 'ModelContentRepository' &&
            it.handles(item.class)
        } ?:
        administeredItemContentRepositories.find {
            it.getClass().simpleName != 'AdministeredItemContentRepository'
        }
    }

    protected M getModelWithContent(UUID modelId) {
        M existing = modelContentRepository.findWithContentById(modelId)
        existing.setAssociations()
        existing
    }

    private void connectFacets(AdministeredItem administeredItem) {
        if (administeredItem.metadata) {
            administeredItem.metadata.each {
                updateMultiAwareData(administeredItem, it)
            }
        }
        if (administeredItem.summaryMetadata) {
            administeredItem.summaryMetadata.each {
                updateMultiAwareData(administeredItem, it)
            }
        }
        if (administeredItem.rules) {
            administeredItem.rules.each {
                updateMultiAwareData(administeredItem, it)
            }
        }
        if (administeredItem.annotations) {
            administeredItem.annotations.each {
                updateMultiAwareData(administeredItem, it)
                if (it.childAnnotations) {
                    it.childAnnotations.forEach {child ->
                        updateMultiAwareData(administeredItem, child)
                    }
                }
            }
        }
        if (administeredItem.referenceFiles) {
            administeredItem.referenceFiles.each {
                updateMultiAwareData(administeredItem, it)
            }
        }
        if (administeredItem instanceof Model) {
            if (((Model) administeredItem).versionLinks) {
                ((Model) administeredItem).versionLinks.each {
                    updateMultiAwareData(administeredItem, it)
                }
            }
        }
        if (administeredItem.semanticLinks) {
            administeredItem.semanticLinks.each {
                updateMultiAwareData(administeredItem, it)
            }
        }
    }

    private void updateMultiAwareData(AdministeredItem item, Facet it) {
        it.multiFacetAwareItemDomainType = item.domainType
        it.multiFacetAwareItemId = item.id
        it.multiFacetAwareItem = item
    }

}