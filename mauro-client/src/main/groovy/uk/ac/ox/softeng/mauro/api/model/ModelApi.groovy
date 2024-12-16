package uk.ac.ox.softeng.mauro.api.model


import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile

@CompileStatic
interface ModelApi<M extends Model> extends AdministeredItemApi<M, Folder> {

    M create(@NonNull UUID folderId, @Body @NonNull M model)

    M update(UUID id, @Body @NonNull M model)

    M moveFolder(UUID id, String destination)

    HttpStatus delete(UUID id, @Body @Nullable M model)

    ListResponse<M> listAll()

    M finalise(UUID id, @Body FinaliseData finaliseData)

    M createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    StreamedFile exportModel(UUID modelId, String namespace, String name, @Nullable String version)

    ListResponse<M> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)

}