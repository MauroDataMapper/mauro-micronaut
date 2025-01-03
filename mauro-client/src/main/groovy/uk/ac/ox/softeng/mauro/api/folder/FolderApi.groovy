package uk.ac.ox.softeng.mauro.api.folder

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.ModelApi
import uk.ac.ox.softeng.mauro.domain.folder.Folder
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
    HttpStatus delete(UUID id, @Body @Nullable Folder folder)

    @Delete(Paths.CHILD_FOLDER_ID)
    HttpStatus delete(UUID parentId, UUID id, @Body @Nullable Folder folder)

    @Get(Paths.FOLDER_EXPORT)
    StreamedFile exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.FOLDER_IMPORT)
    ListResponse<Folder> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)
}