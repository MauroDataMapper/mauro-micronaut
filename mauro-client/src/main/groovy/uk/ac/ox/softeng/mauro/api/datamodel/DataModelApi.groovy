package uk.ac.ox.softeng.mauro.api.datamodel

import io.micronaut.context.annotation.Parameter
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.ModelApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.IntersectsData
import uk.ac.ox.softeng.mauro.domain.datamodel.IntersectsManyData
import uk.ac.ox.softeng.mauro.domain.datamodel.SubsetData
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.plugin.importer.DataModelImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

@MauroApi
interface DataModelApi extends ModelApi<DataModel> {

    @Get(Paths.DATA_MODEL_ID_ROUTE)
    DataModel show(UUID id)

    @Post(Paths.FOLDER_LIST_DATA_MODEL)
    DataModel create(UUID folderId, @Body @NonNull DataModel dataModel)

    @Put(Paths.DATA_MODEL_ID_ROUTE)
    DataModel update(UUID id, @Body @NonNull DataModel dataModel)

    @Delete(Paths.DATA_MODEL_ID_ROUTE)
    HttpResponse delete(UUID id, @Body @Nullable DataModel dataModel)

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
    SubsetData subset(UUID id, UUID otherId, @Body SubsetData subsetData)

    @Post(Paths.DATA_MODEL_INTERSECTS_MANY)
    ListResponse<IntersectsData> intersectsMany(UUID id, @Body IntersectsManyData intersectsManyData)

}
