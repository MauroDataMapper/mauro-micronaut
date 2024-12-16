package uk.ac.ox.softeng.mauro.api.security.openidprovider

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}/openidConnectProviders')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface OpenidProviderApi {

    @Get
    List<OpenidConnectProvider> list()
}
