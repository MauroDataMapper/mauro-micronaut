package org.maurodata.api.folder

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.ModelApi
import org.maurodata.domain.folder.Folder
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
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.multipart.MultipartBody

@MauroApi
interface FolderApi extends ModelApi<Folder> {

    @Get(Paths.FOLDER_ID)
    Folder show(UUID id)

    @Get(Paths.CHILD_FOLDER_ID)
    Folder show(UUID parentId, UUID id)

    @Post(Paths.FOLDER_LIST)
    Folder create(@Body Folder folder)

    @Post(Paths.CHILD_FOLDER_LIST)
    Folder create(UUID parentId, @Body @NonNull Folder folder)

    @Put(Paths.FOLDER_ID)
    Folder update(UUID id, @Body @NonNull Folder folder)

    @Put(Paths.CHILD_FOLDER_ID)
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder)

    @Put(Paths.FOLDER_MOVE)
    Folder moveFolder(UUID id, String destination)

    @Get(Paths.FOLDER_LIST)
    ListResponse<Folder> listAll()

    @Get(Paths.CHILD_FOLDER_LIST)
    ListResponse<Folder> list(UUID parentId)

    @Delete(Paths.FOLDER_ID)
    HttpResponse delete(UUID id, @Body @Nullable Folder folder, @Nullable Boolean permanent)

    @Delete(Paths.CHILD_FOLDER_ID)
    HttpResponse delete(UUID parentId, UUID id, @Body @Nullable Folder folder, @Nullable Boolean permanent)

    @Get(Paths.FOLDER_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @Produces(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.FOLDER_IMPORT)
    ListResponse<Folder> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)

    // This is the version that will be implemented by the controller
    ListResponse<Folder> importModel(@Body io.micronaut.http.server.multipart.MultipartBody body, String namespace, String name, @Nullable String version)

    @Get(Paths.FOLDER_DOI)
    Map doi(UUID id)

    @Put(Paths.FOLDER_READ_BY_EVERYONE)
    Folder allowReadByEveryone(UUID id)

    @Delete(Paths.FOLDER_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id)

    @Put(Paths.FOLDER_READ_BY_AUTHENTICATED)
    Folder allowReadByAuthenticated(UUID id)

    @Delete(Paths.FOLDER_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id)


}