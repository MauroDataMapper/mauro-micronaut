package org.maurodata.api.security

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.domain.security.UserGroup
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface UserGroupApi {

    @Post(Paths.USER_GROUP_LIST)
    UserGroup create(@Body @NonNull UserGroup userGroup)

    @Get(Paths.USER_GROUP_LIST)
    ListResponse<UserGroup> index(@Nullable PaginationParams params)

    @Get(Paths.USER_GROUP_ID)
    UserGroup show(UUID id)

    @Get(Paths.USER_GROUP_CATALOGUE_USERS_PAGED)
    ListResponse<CatalogueUser> users(UUID id, @Nullable PaginationParams params)

    @Put(Paths.USER_GROUP_ID)
    UserGroup update(@NonNull UUID id, @Body @NonNull UserGroup userGroup)
}
