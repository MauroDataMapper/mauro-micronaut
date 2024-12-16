package uk.ac.ox.softeng.mauro.api.datamodel

import uk.ac.ox.softeng.mauro.api.model.ModelApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.plugin.importer.DataModelImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.RequestBean
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface DataModelApi extends ModelApi<DataModel> {


    @Get('/dataModels/{id}')
    DataModel show(UUID id)

    @Post('/folders/{folderId}/dataModels')
    DataModel create(UUID folderId, @Body @NonNull DataModel dataModel)


    @Put('/dataModels/{id}')
    DataModel update(UUID id, @Body @NonNull DataModel dataModel)

    @Delete('/dataModels/{id}')
    HttpStatus delete(UUID id, @Body @Nullable DataModel dataModel)


    @Get('/dataModels/{id}/search{?requestDTO}')
    ListResponse<SearchResultsDTO> searchGet(UUID id, @RequestBean SearchRequestDTO requestDTO)

    @Post('/dataModels/{id}/search')
    ListResponse<SearchResultsDTO> searchPost(UUID id, @Body SearchRequestDTO requestDTO)


    @Get('/folders/{folderId}/dataModels')
    ListResponse<DataModel> list(UUID folderId)

    @Get('/dataModels')
    ListResponse<DataModel> listAll()

    @Put('/dataModels/{id}/finalise')
    DataModel finalise(UUID id, @Body FinaliseData finaliseData)

    @Put('/dataModels/{id}/newBranchModelVersion')
    DataModel createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    @Get('/dataModels/{id}/export{/namespace}{/name}{/version}')
    StreamedFile exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/dataModels/import/{namespace}/{name}{/version}')
    ListResponse<DataModel> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)

    @Get('/dataModels/{id}/diff/{otherId}')
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId)

    @Get('/dataModels/providers/importers')
    List<DataModelImporterPlugin> dataModelImporters()

}
