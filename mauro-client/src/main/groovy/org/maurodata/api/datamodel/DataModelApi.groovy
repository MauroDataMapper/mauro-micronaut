package org.maurodata.api.datamodel

import org.maurodata.api.model.MergeIntoDTO

import io.micronaut.http.annotation.QueryValue
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.MergeDiffDTO
import org.maurodata.api.model.ModelApi
import org.maurodata.api.model.ModelVersionDTO
import org.maurodata.api.model.ModelVersionedRefDTO
import org.maurodata.api.model.ModelVersionedWithTargetsRefDTO
import org.maurodata.api.model.VersionLinkDTO
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.IntersectsData
import org.maurodata.domain.datamodel.IntersectsManyData
import org.maurodata.domain.datamodel.SubsetData
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.plugin.datatype.DefaultDataTypeProviderPlugin
import org.maurodata.plugin.importer.DataModelImporterPlugin
import org.maurodata.web.ListResponse

import io.micronaut.context.annotation.Parameter
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@MauroApi
interface DataModelApi extends ModelApi<DataModel> {

    @Get(Paths.DATA_MODEL_ID_ROUTE)
    DataModel show(UUID id)

    @Post(Paths.CREATE_DATA_MODEL)
    DataModel create(UUID folderId, @Body @NonNull DataModel dataModel, @Nullable @QueryValue String defaultDataTypeProvider)

    @Post(Paths.CREATE_DATA_MODEL)
    DataModel create(UUID folderId, @Body @NonNull DataModel dataModel)

    @Put(Paths.DATA_MODEL_ID_ROUTE)
    DataModel update(UUID id, @Body @NonNull DataModel dataModel)

    @Delete(Paths.DATA_MODEL_ID_ROUTE)
    HttpResponse delete(UUID id, @Body @Nullable DataModel dataModel, @Nullable Boolean permanent)

    @Get(Paths.DATA_MODEL_SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(UUID id, @Parameter @Nullable SearchRequestDTO requestDTO)

    @Post(Paths.DATA_MODEL_SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(UUID id, @Body SearchRequestDTO requestDTO)

    @Get(Paths.FOLDER_LIST_DATA_MODEL)
    ListResponse<DataModel> list(UUID folderId)

    @Get(Paths.DATA_MODEL_ROUTE)
    ListResponse<DataModel> listAll()

    @Put(Paths.DATA_MODEL_ID_FINALISE)
    DataModel finalise(UUID id, @Body FinaliseData finaliseData)

    @Put(Paths.DATA_MODEL_BRANCH_MODEL_VERSION)
    DataModel createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    @Get(value = Paths.DATA_MODEL_EXPORT, produces = MediaType.ALL)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @ExecuteOn(TaskExecutors.IO)
    @Produces(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.DATA_MODEL_IMPORT)
    ListResponse<DataModel> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)

    // This is the version that will be implemented by the controller
    ListResponse<DataModel> importModel(@Body io.micronaut.http.server.multipart.MultipartBody body, String namespace, String name, @Nullable String version)

    @Get(Paths.DATA_MODEL_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId)

    @Get(Paths.DATA_MODEL_EXPORTERS)
    List<DataModelImporterPlugin> dataModelImporters()

    @Put(Paths.DATA_MODEL_SUBSET)
    DataModel subset(UUID id, UUID otherId, @Body SubsetData subsetData)

    @Post(Paths.DATA_MODEL_INTERSECTS_MANY)
    ListResponse<IntersectsData> intersectsMany(UUID id, @Body IntersectsManyData intersectsManyData)

    @Get(Paths.DATA_MODEL_VERSION_LINKS)
    ListResponse<VersionLinkDTO> listVersionLinks(UUID id)

    @Get(Paths.DATA_MODEL_SIMPLE_MODEL_VERSION_TREE)
    List<ModelVersionedRefDTO> simpleModelVersionTree(UUID id, @Nullable Boolean branchesOnly)

    @Get(Paths.DATA_MODEL_MODEL_VERSION_TREE)
    List<ModelVersionedWithTargetsRefDTO> modelVersionTree(UUID id)

    @Get(Paths.DATA_MODEL_CURRENT_MAIN_BRANCH)
    DataModel currentMainBranch(UUID id)

    @Get(Paths.DATA_MODEL_LATEST_MODEL_VERSION)
    ModelVersionDTO latestModelVersion(UUID id)

    @Get(Paths.DATA_MODEL_LATEST_FINALISED_MODEL)
    ModelVersionedRefDTO latestFinalisedModel(UUID id)

    @Get(Paths.DATA_MODEL_COMMON_ANCESTOR)
    DataModel commonAncestor(UUID id, UUID other_model_id)

    @Get(Paths.DATA_MODEL_MERGE_DIFF)
    MergeDiffDTO mergeDiff(@NonNull UUID id, @NonNull UUID otherId)

    @Put(Paths.DATA_MODEL_MERGE_INTO)
    DataModel mergeInto(@NonNull UUID id, @NonNull UUID otherId, @Body @Nullable MergeIntoDTO mergeIntoDTO)

    @Get(Paths.DATA_MODEL_DOI)
    Map doi(UUID id)

    @Get(Paths.DATA_MODEL_DATATYPE_PROVIDERS)
    List<DefaultDataTypeProviderPlugin> defaultDataTypeProviders()

    @Get(Paths.DATA_MODEL_TYPES)
    List<String> dataModelTypes()

    @Put(Paths.DATA_MODEL_READ_BY_EVERYONE)
    DataModel allowReadByEveryone(UUID id)

    @Delete(Paths.DATA_MODEL_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id)

    @Put(Paths.DATA_MODEL_READ_BY_AUTHENTICATED)
    DataModel allowReadByAuthenticated(UUID id)

    @Delete(Paths.DATA_MODEL_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id)
}
