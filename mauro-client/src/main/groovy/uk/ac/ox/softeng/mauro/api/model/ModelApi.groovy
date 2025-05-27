package uk.ac.ox.softeng.mauro.api.model

import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Put
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.client.multipart.MultipartBody

@CompileStatic
interface ModelApi<M extends Model> extends AdministeredItemApi<M, Folder> {

    M create(@NonNull UUID folderId, @Body @NonNull M model)

    M update(UUID id, @Body @NonNull M model)

    M moveFolder(UUID id, String destination)

    HttpResponse delete(UUID id, @Body @Nullable M model, @Nullable Boolean permanent)

    ListResponse<M> listAll()

    M finalise(UUID id, @Body FinaliseData finaliseData)

    M createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData)

    HttpResponse<byte[]> exportModel(UUID modelId, String namespace, String name, @Nullable String version)

    ListResponse<M> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)

    // To be implemented by the controller
    ListResponse<M> importModel(@Body io.micronaut.http.server.multipart.MultipartBody body, String namespace, String name, @Nullable String version)

    M allowReadByAuthenticated(UUID id)

    HttpResponse revokeReadByAuthenticated(UUID id)

    M allowReadByEveryone(UUID id)

    HttpResponse revokeReadByEveryone(UUID id)

    PermissionsDTO permissions(UUID id)
}