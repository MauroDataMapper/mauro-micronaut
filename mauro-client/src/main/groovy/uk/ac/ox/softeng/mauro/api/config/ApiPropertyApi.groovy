package uk.ac.ox.softeng.mauro.api.config

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.config.ApiProperty
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface ApiPropertyApi {

    @Get(Paths.API_PROPERTY_LIST_PUBLIC)
    ListResponse<ApiProperty> listPubliclyVisible()

    @Get(Paths.API_PROPERTY_LIST_ALL)
    ListResponse<ApiProperty> listAll()

    @Get(Paths.API_PROPERTY_SHOW)
    ApiProperty show(UUID id)

    @Post(Paths.API_PROPERTY_LIST_ALL)
    ApiProperty create(@Body @NonNull ApiProperty apiProperty)

    @Put(Paths.API_PROPERTY_SHOW)
    ApiProperty update(UUID id, @Body @NonNull ApiProperty apiProperty)

    @Delete(Paths.API_PROPERTY_SHOW)
    HttpResponse delete(UUID id, @Body @Nullable ApiProperty apiProperty)

}
