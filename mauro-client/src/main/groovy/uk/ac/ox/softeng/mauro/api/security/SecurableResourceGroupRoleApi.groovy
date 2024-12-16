package uk.ac.ox.softeng.mauro.api.security


import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.domain.security.SecurableResourceGroupRole

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface SecurableResourceGroupRoleApi {

    @Post('/{securableResourceDomainType}/{securableResourceId}/roles/{role}/userGroups/{userGroupId}')
    SecurableResourceGroupRole create(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @NonNull Role role, @NonNull UUID userGroupId)

    @Delete('/{securableResourceDomainType}/{securableResourceId}/roles/{role}/userGroups/{userGroupId}')
    HttpStatus delete(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @NonNull Role role, @NonNull UUID userGroupId)
}
