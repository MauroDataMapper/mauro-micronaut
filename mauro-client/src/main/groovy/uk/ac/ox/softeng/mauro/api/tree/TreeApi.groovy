package uk.ac.ox.softeng.mauro.api.tree


import uk.ac.ox.softeng.mauro.domain.tree.TreeItem

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}/tree')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface TreeApi {

    @Get('/folders{/id}')
    List<TreeItem> folderTree(@Nullable UUID id)

    @Get('/folders/{domainType}/{id}')
    List<TreeItem> itemTree(String domainType, UUID id)

}
