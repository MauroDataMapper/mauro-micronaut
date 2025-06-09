package uk.ac.ox.softeng.mauro.api.folder

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.multipart.MultipartBody
import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.MergeDiffDTO
import uk.ac.ox.softeng.mauro.api.model.ModelApi
import uk.ac.ox.softeng.mauro.api.model.ModelVersionDTO
import uk.ac.ox.softeng.mauro.api.model.ModelVersionedRefDTO
import uk.ac.ox.softeng.mauro.api.model.ModelVersionedWithTargetsRefDTO
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.web.ListResponse

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
}