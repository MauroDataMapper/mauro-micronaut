package org.maurodata.api.config

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.config.ApiProperty
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

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

    @Get(Paths.API_PROPERTY_LIST_PUBLIC_PAGED)
    ListResponse<ApiProperty> listPubliclyVisible(@Nullable PaginationParams params)

    @Get(Paths.API_PROPERTY_LIST_ALL)
    ListResponse<ApiProperty> listAll()

    @Get(Paths.API_PROPERTY_LIST_ALL_PAGED)
    ListResponse<ApiProperty> listAll(@Nullable PaginationParams params)

    @Get(Paths.API_PROPERTY_SHOW)
    ApiProperty show(UUID id)

    @Post(Paths.API_PROPERTY_LIST_ALL)
    ApiProperty create(@Body @NonNull ApiProperty apiProperty)

    @Put(Paths.API_PROPERTY_SHOW)
    ApiProperty update(UUID id, @Body @NonNull ApiProperty apiProperty)

    @Delete(Paths.API_PROPERTY_SHOW)
    HttpResponse delete(UUID id, @Body @Nullable ApiProperty apiProperty)

}
