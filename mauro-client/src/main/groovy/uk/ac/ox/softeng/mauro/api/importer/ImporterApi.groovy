package uk.ac.ox.softeng.mauro.api.importer


import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface ImporterApi {

    // TODO: Update interface to handle a more intelligent rendering of this information
    @Get('/importer/parameters/{namespace}/{name}/{version}')
    Map<String, Object> getImporterParameters(String namespace, String name, @Nullable String version)

}
