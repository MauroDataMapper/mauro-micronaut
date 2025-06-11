package org.maurodata.api.classifier

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.ModelApi
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.web.ListResponse

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
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn

@MauroApi

interface ClassificationSchemeApi extends ModelApi<ClassificationScheme> {

    @Get(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    ClassificationScheme show(UUID id)

    @Post(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE)
    ClassificationScheme create(UUID folderId, @Body @NonNull ClassificationScheme classificationScheme)

    @Put(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    ClassificationScheme update(UUID id, @Body @NonNull ClassificationScheme classificationScheme)

    @Delete(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    HttpResponse delete(UUID id, @Body @Nullable ClassificationScheme classificationScheme, @Nullable Boolean permanent)


    @Get(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE)
    ListResponse<ClassificationScheme> list(UUID folderId)

    @Get(Paths.CLASSIFICATION_SCHEMES_LIST)
    ListResponse<ClassificationScheme> listAll()


    @Put(Paths.CLASSIFICATION_SCHEMES_BRANCH_MODEL_VERSION)
    ClassificationScheme createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    @Get(Paths.CLASSIFICATION_SCHEMES_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.CLASSIFICATION_SCHEMES_IMPORT)
    ListResponse<ClassificationScheme> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)

    @Get(Paths.CLASSIFICATION_SCHEMES_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId)
}
