package uk.ac.ox.softeng.mauro.api.terminology

import uk.ac.ox.softeng.mauro.api.model.ModelApi
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
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
interface TerminologyApi extends ModelApi<Terminology> {

    @Get('/terminologies/{id}')
    Terminology show(UUID id)

    @Post('/folders/{folderId}/terminologies')
    Terminology create(UUID folderId, @Body @NonNull Terminology terminology)

    @Put('/terminologies/{id}')
    Terminology update(UUID id, @Body @NonNull Terminology terminology)

    @Delete('/terminologies/{id}')
    HttpStatus delete(UUID id, @Body @Nullable Terminology terminology)

    @Get('/terminologies/{id}/search{?requestDTO}')
    ListResponse<SearchResultsDTO> searchGet(UUID id, @RequestBean SearchRequestDTO requestDTO)

    @Post('/terminologies/{id}/search')
    ListResponse<SearchResultsDTO> searchPost(UUID id, @Body SearchRequestDTO requestDTO)

    @Get('/folders/{folderId}/terminologies')
    ListResponse<Terminology> list(UUID folderId)

    @Get('/terminologies')
    ListResponse<Terminology> listAll()

    @Put('/terminologies/{id}/finalise')
    Terminology finalise(UUID id, @Body FinaliseData finaliseData)

    @Put('/terminologies/{id}/newBranchModelVersion')
    Terminology createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    @Get('/terminologies/{id}/export{/namespace}{/name}{/version}')
    StreamedFile exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/terminologies/import/{namespace}/{name}{/version}')
    ListResponse<Terminology> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)
/*
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/terminologies/import{/namespace}{/name}{/version}')
    ListResponse<Terminology> importModel(@Body Map<String, String> importMap, @Nullable String namespace, @Nullable String name, @Nullable String version)
 */

    @Get('/terminologies/{id}/diff/{otherId}')
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId)
}
