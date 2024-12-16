package uk.ac.ox.softeng.mauro.api.config

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.Authentication

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface SessionApi {

    @Get('/isAuthenticated')
    Map<String, Boolean> isAuthenticated()

    @Get('/isApplicationAdministration')
    Map<String, Boolean> isApplicationAdministration()

    @Get('/authenticationDetails')
    Map authenticationDetails(@Nullable Authentication authentication)

    @Get('/checkAuthenticated')
    String checkAuthenticated()

    @Get('/checkAnonymous')
    String checkAnonymous()
}
