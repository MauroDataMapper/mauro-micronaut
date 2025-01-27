package uk.ac.ox.softeng.mauro.api.terminology

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.ModelApi
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.RequestBean
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@MauroApi
interface TerminologyApi extends ModelApi<Terminology> {

    @Get(Paths.TERMINOLOGY_ID)
    Terminology show(UUID id)

    @Post(Paths.FOLDER_LIST_TERMINOLOGY)
    Terminology create(UUID folderId, @Body @NonNull Terminology terminology)

    @Put(Paths.TERMINOLOGY_ID)
    Terminology update(UUID id, @Body @NonNull Terminology terminology)

    @Delete(Paths.TERMINOLOGY_ID)
    HttpResponse delete(UUID id, @Body @Nullable Terminology terminology)

    @Get(Paths.TERMINOLOGY_SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(UUID id, @RequestBean SearchRequestDTO requestDTO)

    @Post(Paths.TERMINOLOGY_SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(UUID id, @Body SearchRequestDTO requestDTO)

    @Get(Paths.FOLDER_LIST_TERMINOLOGY)
    ListResponse<Terminology> list(UUID folderId)

    @Get(Paths.TERMINOLOGY_LIST)
    ListResponse<Terminology> listAll()

    @Put(Paths.TERMINOLOGY_FINALISE)
    Terminology finalise(UUID id, @Body FinaliseData finaliseData)

    @Put(Paths.TERMINOLOGY_NEW_BRANCH_MODEL_VERSION)
    Terminology createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    @Get(Paths.TERMINOLOGY_EXPORT)
    StreamedFile exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.TERMINOLOGY_IMPORT)
    ListResponse<Terminology> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)
/*
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/terminologies/import{/namespace}{/name}{/version}')
    ListResponse<Terminology> importModel(@Body Map<String, String> importMap, @Nullable String namespace, @Nullable String name, @Nullable String version)
 */

    @Get(Paths.TERMINOLOGY_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId)
}
