package uk.ac.ox.softeng.mauro.api.security

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.web.ChangePassword

@MauroApi
interface CatalogueUserApi {

    @Post(Paths.USER_ADMIN_REGISTER)
    CatalogueUser adminRegister(@Body @NonNull CatalogueUser newUser)

    @Get(Paths.USER_CURRENT_USER)
    CatalogueUser currentUser()

    @Put(Paths.USER_ID)
    CatalogueUser show(UUID id)

    @Put(Paths.USER_CHANGE_PASSWORD)
    CatalogueUser changePassword(@Body @NonNull ChangePassword changePasswordRequest)

    @Put(Paths.USER_ID)
    CatalogueUser update(@NonNull UUID id, @Body @NonNull CatalogueUser catalogueUser)

    // todo Stub method to enable login with UI
    @Get(Paths.USER_PREFERENCES)
    String showUserPreferences(UUID id)
}
