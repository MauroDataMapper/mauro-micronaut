package org.maurodata.api.folder

import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import org.maurodata.api.model.MergeIntoDTO

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.MergeDiffDTO
import org.maurodata.api.model.ModelApi
import org.maurodata.api.model.ModelVersionDTO
import org.maurodata.api.model.ModelVersionedRefDTO
import org.maurodata.api.model.ModelVersionedWithTargetsRefDTO
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.web.ListResponse

@MauroApi
interface VersionedFolderApi extends ModelApi<Folder> {

    @Get(Paths.VERSIONED_FOLDER_ID)
    Folder show(UUID id)

    @Get(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder show(UUID parentId, UUID id)

    @Post(Paths.VERSIONED_FOLDER_LIST)
    Folder create(@Body Folder folder)

    @Post(Paths.CHILD_VERSIONED_FOLDER_LIST)
    Folder create(UUID parentId, @Body @NonNull Folder folder)

    @Put(Paths.VERSIONED_FOLDER_ID)
    Folder update(UUID id, @Body @NonNull Folder folder)

    @Put(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder)

    @Get(Paths.VERSIONED_FOLDER_LIST)
    ListResponse<Folder> listAll()

    @Get(Paths.CHILD_VERSIONED_FOLDER_LIST)
    ListResponse<Folder> list(UUID parentId)

    @Put(Paths.VERSIONED_FOLDER_FINALISE)
    Folder finalise(UUID id, @Body FinaliseData finaliseData)

    @Put(Paths.VERSIONED_FOLDER_NEW_BRANCH_MODEL_VERSION)
    Folder createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    @Delete(Paths.VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID id, @Body @Nullable Folder folder, @Nullable Boolean permanent)

    @Delete(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID parentId, UUID id, @Body @Nullable Folder folder, @Nullable Boolean permanent)

    @Get(Paths.VERSIONED_FOLDER_DOI)
    Map doi(UUID id)

    @Get(Paths.VERSIONED_FOLDER_SIMPLE_MODEL_VERSION_TREE)
    List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly)

    @Get(Paths.VERSIONED_FOLDER_MODEL_VERSION_TREE)
    List<ModelVersionedWithTargetsRefDTO> modelVersionTree(UUID id)

    @Get(Paths.VERSIONED_FOLDER_CURRENT_MAIN_BRANCH)
    Folder currentMainBranch(UUID id)

    @Get(Paths.VERSIONED_FOLDER_LATEST_MODEL_VERSION)
    ModelVersionDTO latestModelVersion(UUID id)

    @Get(Paths.VERSIONED_FOLDER_LATEST_FINALISED_MODEL)
    ModelVersionedRefDTO latestFinalisedModel(UUID id)

    @Get(Paths.VERSIONED_FOLDER_COMMON_ANCESTOR)
    Folder commonAncestor(UUID id, UUID other_model_id)

    @Get(Paths.VERSIONED_FOLDER_MERGE_DIFF)
    MergeDiffDTO mergeDiff(@NonNull UUID id, @NonNull UUID otherId)

    @Put(Paths.VERSIONED_FOLDER_MERGE_INTO)
    Folder mergeInto(@NonNull UUID id, @NonNull UUID otherId, @Body @Nullable MergeIntoDTO mergeIntoDTO)

    @Put(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    Folder allowReadByEveryone(UUID id)
    @Delete(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id)

    @Put(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    Folder allowReadByAuthenticated(UUID id)

    @Delete(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id)

    @Get(Paths.VERSIONED_FOLDER_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @Produces(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.VERSIONED_FOLDER_IMPORT)
    ListResponse<Folder> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)

    // This is the version that will be implemented by the controller
    ListResponse<Folder> importModel(@Body io.micronaut.http.server.multipart.MultipartBody body, String namespace, String name, @Nullable String version)
}