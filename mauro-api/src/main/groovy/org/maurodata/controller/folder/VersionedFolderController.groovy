package org.maurodata.controller.folder

import io.swagger.v3.oas.annotations.Operation
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.annotation.Part
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.inject.Inject
import org.reactivestreams.Publisher
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.folder.VersionedFolderApi
import org.maurodata.api.model.MergeDiffDTO
import org.maurodata.api.model.MergeIntoDTO
import org.maurodata.api.model.ModelVersionDTO
import org.maurodata.api.model.ModelVersionedRefDTO
import org.maurodata.api.model.ModelVersionedWithTargetsRefDTO
import org.maurodata.api.model.PermissionsDTO
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ModelController
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.folder.FolderService
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.exception.MauroApplicationException
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository

import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class VersionedFolderController extends ModelController<Folder> implements VersionedFolderApi {

    @Inject
    FolderService folderService

    VersionedFolderController(FolderCacheableRepository folderRepository, FolderService folderService) {
        super(Folder, folderRepository, folderRepository, folderService)
        this.folderService = folderService
    }

    @Operation(operationId = 'showVersionedFolder', summary = "Get a versioned folder", description = "Returns a versioned folder.")
    @Get(Paths.VERSIONED_FOLDER_ID)
    Folder show(UUID id) {
        super.show(id)
    }

    @Operation(operationId = 'showVersionedFolderChild', summary = "Get a versioned folder", description = "Returns a versioned folder.")
    @Get(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder show(UUID parentId, UUID id) {
        super.show(id)
    }

    @Audit
    @Operation(operationId = 'createVersionedFolder', summary = "Create a versioned folder", description = "Creates a versioned folder.")
    @Post(Paths.VERSIONED_FOLDER_LIST)
    Folder create(@Body Folder folder) {
        cleanBody(folder)
        updateCreationProperties(folder)
        folder.authority = super.authorityService.getDefaultAuthority()
        folder.branchName = Model.DEFAULT_BRANCH_NAME

        pathRepository.readParentItems(folder)
        folder.updatePath()

        folderRepository.save(folder)
    }

    @Audit
    @Transactional
    @Operation(operationId = 'createChildVersionedFolder', summary = "Create a versioned folder", description = "Creates a versioned folder.")
    @Post(Paths.CHILD_VERSIONED_FOLDER_LIST)
    Folder create(UUID parentId, @Body @NonNull Folder folder) {
        folder.branchName = Model.DEFAULT_BRANCH_NAME
        super.create(parentId, folder)
    }

    @Audit
    @Operation(operationId = 'updateVersionedFolder', summary = "Update a versioned folder", description = "Updates a versioned folder.")
    @Put(Paths.VERSIONED_FOLDER_ID)
    Folder update(UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Audit
    @Operation(operationId = 'updateVersionedFolderChild', summary = "Update a versioned folder", description = "Updates a versioned folder.")
    @Put(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Operation(operationId = 'listAllVersionedFolder', summary = "List the versioned folders", description = "Returns the versioned folders.")
    @Get(Paths.VERSIONED_FOLDER_LIST_PAGED)
    ListResponse<Folder> listAll(@Nullable PaginationParams params = new PaginationParams()) {

        final ListResponse<Folder> listResponse = super.listAll() as ListResponse<Folder>
        final List<Folder> versionedFolders = listResponse.items.findAll {it.domainType == "VersionedFolder"} as List<Folder>

        return ListResponse.from(versionedFolders, params)
    }

    @Operation(operationId = 'listFolderChild', summary = "List the versioned folders", description = "Returns the versioned folders.")
    @Get(Paths.CHILD_VERSIONED_FOLDER_LIST_PAGED)
    ListResponse<Folder> list(UUID parentId, @Nullable PaginationParams params = new PaginationParams()) {

        final ListResponse<Folder> listResponse = super.list(parentId) as ListResponse<Folder>
        final List<Folder> versionedFolders = listResponse.items.findAll {it.domainType == "VersionedFolder"} as List<Folder>

        return ListResponse.from(versionedFolders, params)
    }

    @Audit(title = EditType.EXPORT, description = 'Export versioned folder')
    @Operation(operationId = 'exportModelVersionedFolder', summary = "Get a versioned folder", description = "Returns a versioned folder.")
    @Get(Paths.VERSIONED_FOLDER_EXPORT)
    @Override
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModels(namespace, name, version, [id])
    }

    @Audit(title = EditType.EXPORT, description = 'Export versioned folders')
    @Operation(summary = "Export the versioned folder", description = "Exports the versioned folder.")
    @Post(Paths.VERSIONED_FOLDER_EXPORT_MANY)
    @Override
    HttpResponse<byte[]> exportModels(@Nullable String namespace, @Nullable String name, @Nullable String version, @Body List<UUID> ids) {
        super.exportModels(namespace, name, version, ids)

    }

    @Audit
    @Transactional
    @Operation(operationId = 'finaliseVersionedFolder', summary = "Update a versioned folder", description = "Updates a versioned folder.")
    @Put(Paths.VERSIONED_FOLDER_FINALISE)
    Folder finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Audit
    @Transactional
    @Operation(summary = "Update a versioned folder", description = "Updates a versioned folder.")
    @Put(Paths.VERSIONED_FOLDER_NEW_BRANCH_MODEL_VERSION)
    Folder createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }


    @Override
    ListResponse<Folder> importModel(HttpRequest<?> request, @Part('importFile') @Nullable Publisher<StreamingFileUpload> importFile, String namespace, String name, @Nullable String version) {
        super.importModel(request, importFile, namespace, name, version)
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteVersionedFolder', summary = "Delete a versioned folder", description = "Deletes a versioned folder.")
    @Delete(Paths.VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder, permanent)
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteVersionedFolderChild', summary = "Delete a versioned folder", description = "Deletes a versioned folder.")
    @Delete(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID parentId, UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder, permanent)
    }

    @Audit
    @Operation(summary = "Update a versioned folder", description = "Updates a versioned folder.")
    @Put(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    @Transactional
    Folder allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as Folder
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a versioned folder", description = "Deletes a versioned folder.")
    @Delete(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Operation(summary = "Update a versioned folder", description = "Updates a versioned folder.")
    @Put(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    @Transactional
    Folder allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as Folder
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a versioned folder", description = "Deletes a versioned folder.")
    @Delete(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Operation(summary = "List the versioned folders", description = "Returns the versioned folders.")
    @Get(Paths.VERSIONED_FOLDER_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }

    @Operation(summary = "Get a versioned folder", description = "Returns a versioned folder.")
    @Get(Paths.VERSIONED_FOLDER_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }

    @Override
    @Operation(summary = "List the versioned folders", description = "Returns the versioned folders.")
    @Get(Paths.VERSIONED_FOLDER_SIMPLE_MODEL_VERSION_TREE)
    List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly) {
        super.simpleModelVersionTree(id,branchesOnly)
    }

    @Override
    @Operation(summary = "List the versioned folders", description = "Returns the versioned folders.")
    @Get(Paths.VERSIONED_FOLDER_MODEL_VERSION_TREE)
    List<ModelVersionedWithTargetsRefDTO> modelVersionTree(UUID id) {
        super.modelVersionTree(id)
    }

    @Override
    @Operation(summary = "Get a versioned folder", description = "Returns a versioned folder.")
    @Get(Paths.VERSIONED_FOLDER_CURRENT_MAIN_BRANCH)
    Folder currentMainBranch(UUID id) {
        super.currentMainBranch(id)
    }

    @Override
    @Operation(summary = "Get a versioned folder", description = "Returns a versioned folder.")
    @Get(Paths.VERSIONED_FOLDER_LATEST_MODEL_VERSION)
    ModelVersionDTO latestModelVersion(UUID id) {
        super.latestModelVersion(id)
    }

    @Override
    @Operation(summary = "Get a versioned folder", description = "Returns a versioned folder.")
    @Get(Paths.VERSIONED_FOLDER_LATEST_FINALISED_MODEL)
    ModelVersionedRefDTO latestFinalisedModel(UUID id) {
        super.latestFinalisedModel(id)
    }

    @Operation(summary = "Get a versioned folder", description = "Returns a versioned folder.")
    @Get(Paths.VERSIONED_FOLDER_COMMON_ANCESTOR)
    Folder commonAncestor(UUID id, UUID other_model_id) {
        super.commonAncestor(id,other_model_id)
    }

    @Operation(summary = "Get a versioned folder", description = "Returns a versioned folder.")
    @Get(Paths.VERSIONED_FOLDER_MERGE_DIFF)
    MergeDiffDTO mergeDiff(@NonNull UUID id, @NonNull UUID otherId)
    {
        super.mergeDiff(id,otherId)
    }

    @Audit
    @Transactional
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Override
    @Operation(summary = "Update a versioned folder", description = "Updates a versioned folder.")
    @Put(Paths.VERSIONED_FOLDER_MERGE_INTO)
    Folder mergeInto(@NonNull UUID id, @NonNull UUID otherId, @Body @Nullable MergeIntoDTO mergeIntoDTO)
    {
        super.mergeInto(id,otherId,mergeIntoDTO)
    }

    @Override
    void setBranchName(UUID parentFolderId, Folder folder) {
        Folder parentFolder = getFolderAncestors(parentFolderId)
        if(parentFolder &&
           (parentFolder.branchName || parentFolder.modelVersion || parentFolder.inAVersionedFolder())) {
               throw new MauroApplicationException("Cannot create a versioned folder inside another versioned folder")
        } else {
            // Otherwise, if a branch name isn't already set, we set it to the default
            if(!folder.branchName) {
                folder.branchName = Model.DEFAULT_BRANCH_NAME
            } // Otherwise we leave it as set
        }
    }

}
