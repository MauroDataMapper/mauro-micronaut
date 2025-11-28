package org.maurodata.controller.folder

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
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
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.folder.FolderService
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.exception.MauroApplicationException
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository

import org.maurodata.web.ListResponse

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

    @Get(Paths.VERSIONED_FOLDER_ID)
    Folder show(UUID id) {
        super.show(id)
    }

    @Get(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder show(UUID parentId, UUID id) {
        super.show(id)
    }

    @Audit
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
    @Post(Paths.CHILD_VERSIONED_FOLDER_LIST)
    Folder create(UUID parentId, @Body @NonNull Folder folder) {
        folder.branchName = Model.DEFAULT_BRANCH_NAME
        super.create(parentId, folder)
    }

    @Audit
    @Put(Paths.VERSIONED_FOLDER_ID)
    Folder update(UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Audit
    @Put(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Get(Paths.VERSIONED_FOLDER_LIST)
    ListResponse<Folder> listAll() {

        final ListResponse<Folder> listResponse = super.listAll() as ListResponse<Folder>

        listResponse.items = listResponse.items.findAll {it.domainType == "VersionedFolder"}
        listResponse.count = listResponse.items.size()

        return listResponse
    }

    @Get(Paths.CHILD_VERSIONED_FOLDER_LIST)
    ListResponse<Folder> list(UUID parentId) {

        final ListResponse<Folder> listResponse = super.list(parentId) as ListResponse<Folder>

        listResponse.items = listResponse.items.findAll {it.domainType == "VersionedFolder"}
        listResponse.count = listResponse.items.size()

        return listResponse
    }

    @Audit
    @Transactional
    @Put(Paths.VERSIONED_FOLDER_FINALISE)
    Folder finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Audit
    @Transactional
    @Put(Paths.VERSIONED_FOLDER_NEW_BRANCH_MODEL_VERSION)
    Folder createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }


    @Override
    ListResponse<Folder> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)
    }

    @Audit
    @Transactional
    @Delete(Paths.VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder, permanent)
    }

    @Audit
    @Transactional
    @Delete(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID parentId, UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder, permanent)
    }

    @Audit
    @Put(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    @Transactional
    Folder allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as Folder
    }

    @Audit
    @Transactional
    @Delete(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Put(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    @Transactional
    Folder allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as Folder
    }

    @Audit
    @Transactional
    @Delete(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Get(Paths.VERSIONED_FOLDER_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }

    @Get(Paths.VERSIONED_FOLDER_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_SIMPLE_MODEL_VERSION_TREE)
    List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly) {
        super.simpleModelVersionTree(id,branchesOnly)
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_MODEL_VERSION_TREE)
    List<ModelVersionedWithTargetsRefDTO> modelVersionTree(UUID id) {
        super.modelVersionTree(id)
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_CURRENT_MAIN_BRANCH)
    Folder currentMainBranch(UUID id) {
        super.currentMainBranch(id)
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_LATEST_MODEL_VERSION)
    ModelVersionDTO latestModelVersion(UUID id) {
        super.latestModelVersion(id)
    }

    @Override
    @Get(Paths.VERSIONED_FOLDER_LATEST_FINALISED_MODEL)
    ModelVersionedRefDTO latestFinalisedModel(UUID id) {
        super.latestFinalisedModel(id)
    }

    @Get(Paths.VERSIONED_FOLDER_COMMON_ANCESTOR)
    Folder commonAncestor(UUID id, UUID other_model_id) {
        super.commonAncestor(id,other_model_id)
    }

    @Get(Paths.VERSIONED_FOLDER_MERGE_DIFF)
    MergeDiffDTO mergeDiff(@NonNull UUID id, @NonNull UUID otherId)
    {
        super.mergeDiff(id,otherId)
    }

    @Audit
    @Transactional
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Override
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