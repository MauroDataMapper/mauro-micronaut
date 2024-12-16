package uk.ac.ox.softeng.mauro.api.folder

import uk.ac.ox.softeng.mauro.api.model.ModelApi
import uk.ac.ox.softeng.mauro.domain.folder.Folder
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
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile

@CompileStatic
@Client('${micronaut.http.services.mauro.url}/folders')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface FolderApi extends ModelApi<Folder> {

    @Get('/{id}')
    Folder show(UUID id)

    @Get('/{parentId}/folders/{id}')
    Folder show(UUID parentId, UUID id)

    @Post
    Folder create(@Body Folder folder)

    @Post('/{parentId}/folders')
    Folder create(UUID parentId, @Body @NonNull Folder folder)

    @Put('/{id}')
    Folder update(UUID id, @Body @NonNull Folder folder)

    @Put('/{parentId}/folders/{id}')
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder)

    @Put('/{id}/folder/{destination}')
    Folder moveFolder(UUID id, String destination)

    @Get
    ListResponse<Folder> listAll()

    @Get('/{parentId}/folders')
    ListResponse<Folder> list(UUID parentId)

    @Delete('/{id}')
    HttpStatus delete(UUID id, @Body @Nullable Folder folder)

    @Delete('/{parentId}/folders/{id}')
    HttpStatus delete(UUID parentId, UUID id, @Body @Nullable Folder folder)

    @Get('/{id}/export{/namespace}{/name}{/version}')
    StreamedFile exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/import/{namespace}/{name}{/version}')
    ListResponse<Folder> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version)
}