package uk.ac.ox.softeng.mauro.api.security

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.authentication.UsernamePasswordCredentials

// This interface is not explicitly implemented in a controller anywhere... it's for the client use only
@MauroApi
interface LoginApi {

    @Post(Paths.LOGIN)
    CatalogueUser login(@Body UsernamePasswordCredentials usernamePasswordCredentials)

    @Get(Paths.LOGOUT)
    HttpResponse logout()


}