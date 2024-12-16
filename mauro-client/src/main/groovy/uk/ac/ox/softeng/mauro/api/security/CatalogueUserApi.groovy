package uk.ac.ox.softeng.mauro.api.security


import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.web.ChangePassword

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface CatalogueUserApi {

    @Post('/admin/catalogueUsers/adminRegister')
    CatalogueUser adminRegister(@Body @NonNull CatalogueUser newUser)

    @Get('/catalogueUsers/currentUser')
    CatalogueUser currentUser()

    @Put('/catalogueUsers/currentUser/changePassword')
    CatalogueUser changePassword(@Body @NonNull ChangePassword changePasswordRequest)

    @Put('/catalogueUsers/{id}')
    CatalogueUser update(@NonNull UUID id, @Body @NonNull CatalogueUser catalogueUser)

    // todo Stub method to enable login with UI
    @Get('/catalogueUsers/{id}/userPreferences')
    String showUserPreferences(UUID id)
}
