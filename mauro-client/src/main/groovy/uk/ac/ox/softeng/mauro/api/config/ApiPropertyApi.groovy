package uk.ac.ox.softeng.mauro.api.config

import uk.ac.ox.softeng.mauro.domain.config.ApiProperty
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface ApiPropertyApi {

    @Get('/properties')
    ListResponse<ApiProperty> listPubliclyVisible()

    @Get('/admin/properties')
    ListResponse<ApiProperty> listAll()

    @Get('/admin/properties/{id}')
    ApiProperty show(UUID id)

    @Post('/admin/properties')
    ApiProperty create(@Body @NonNull ApiProperty apiProperty)

    @Put('/admin/properties/{id}')
    ApiProperty update(UUID id, @Body @NonNull ApiProperty apiProperty)

    @Delete('/admin/properties/{id}')
    HttpStatus delete(UUID id, @Body @Nullable ApiProperty apiProperty)

}
