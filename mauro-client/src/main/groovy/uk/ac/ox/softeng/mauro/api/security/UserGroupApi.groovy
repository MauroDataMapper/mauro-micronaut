package uk.ac.ox.softeng.mauro.api.security


import uk.ac.ox.softeng.mauro.domain.security.UserGroup

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface UserGroupApi {

    @Post('/userGroups')
    UserGroup create(@Body @NonNull UserGroup userGroup)

}
