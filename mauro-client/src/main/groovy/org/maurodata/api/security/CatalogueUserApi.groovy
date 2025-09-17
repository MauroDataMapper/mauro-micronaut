package org.maurodata.api.security

import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.web.ChangePassword

@MauroApi
interface CatalogueUserApi {

    @Post(Paths.USER_ADMIN_REGISTER)
    CatalogueUser adminRegister(@Body @NonNull CatalogueUser newUser)

    @Get(Paths.USER_CURRENT_USER)
    CatalogueUser currentUser()

    @Get(Paths.USER_ID)
    CatalogueUser show(UUID id)

    @Put(Paths.USER_CHANGE_PASSWORD)
    CatalogueUser changePassword(@Body @NonNull ChangePassword changePasswordRequest)

    @Put(Paths.USER_ID_CHANGE_PASSWORD)
    CatalogueUser changePassword(UUID id, @Body @NonNull ChangePassword changePasswordRequest)

    @Put(Paths.USER_ID)
    CatalogueUser update(@NonNull UUID id, @Body @NonNull CatalogueUser catalogueUser)

    // todo Stub method to enable login with UI
    @Get(Paths.USER_PREFERENCES)
    Map showUserPreferences(UUID id)

    @Put(Paths.USER_PREFERENCES)
    CatalogueUser updateUserPreferences(@NonNull UUID id, @Body @NonNull String userPreferences)

    @Get(Paths.USER_IMAGE)
    HttpResponse<byte[]> userImage(UUID id)

    @Get(Paths.USER_ADMIN_PENDING_PAGED)
    ListResponse<CatalogueUser> pendingUsers(@Nullable PaginationParams params)

    @Get(Paths.USER_ADMIN_USER_EXISTS)
    Map userExists(String username)

    @Get(Paths.USER_LIST)
    ListResponse<CatalogueUser> index(@Nullable PaginationParams params)

    @Put(Paths.USER_ADMIN_PASSWORD_RESET)
    CatalogueUser adminPasswordReset(UUID id)

    @Post(Paths.USER_SEARCH_POST)
    ListResponse<CatalogueUser> searchPost(@Body SearchRequestDTO requestDTO)
}
