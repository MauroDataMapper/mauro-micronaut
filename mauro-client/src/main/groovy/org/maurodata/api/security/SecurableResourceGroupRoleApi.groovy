package org.maurodata.api.security

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.SecurableResourceGroupRole

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post

@MauroApi
interface SecurableResourceGroupRoleApi {

    @Post(Paths.SECURABLE_ROLE_GROUP_ID)
    SecurableResourceGroupRole create(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @NonNull Role role, @NonNull UUID userGroupId)

    @Delete(Paths.SECURABLE_ROLE_GROUP_ID)
    HttpResponse delete(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @NonNull Role role, @NonNull UUID userGroupId)
}
