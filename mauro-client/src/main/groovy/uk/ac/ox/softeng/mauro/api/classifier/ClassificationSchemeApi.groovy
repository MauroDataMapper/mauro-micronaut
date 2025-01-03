package uk.ac.ox.softeng.mauro.api.classifier

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.ModelApi
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
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
    HttpStatus delete(UUID id, @Body @Nullable ClassificationScheme classificationScheme)


    @Get(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE)
    ListResponse<ClassificationScheme> list(UUID folderId)

    @Get(Paths.CLASSIFICATION_SCHEMES_ROUTE)
    ListResponse<ClassificationScheme> listAll()


    @Put(Paths.CLASSIFICATION_SCHEMES_BRANCH_MODEL_VERSION)
    ClassificationScheme createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    @Get(Paths.CLASSIFICATION_SCHEMES_EXPORT)
    StreamedFile exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.CLASSIFICATION_SCHEMES_IMPORT)
    ListResponse<ClassificationScheme> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)

    @Get(Paths.CLASSIFICATION_SCHEMES_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId)
}
