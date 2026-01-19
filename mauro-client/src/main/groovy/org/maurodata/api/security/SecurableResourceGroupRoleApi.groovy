package org.maurodata.api.security

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.security.Role
import org.maurodata.domain.security.SecurableResourceGroupRole

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@MauroApi
interface SecurableResourceGroupRoleApi {

    @Get(Paths.SECURABLE_RESOURCE_GROUP_ROLES)
    ListResponse<SecurableResourceGroupRole> listSecurableResourceGroupRoles(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @Nullable PaginationParams params)

    @Get(Paths.SECURABLE_RESOURCE_GROUP_ROLES)
    ListResponse<SecurableResourceGroupRole> listSecurableResourceGroupRoles(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId)

    @Get(Paths.GROUP_ROLES)
    ListResponse<Role.RoleDTO> listGroupRoles(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @Nullable PaginationParams params)

    @Get(Paths.GROUP_ROLES)
    ListResponse<Role.RoleDTO> listGroupRoles(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId)

    @Post(Paths.SECURABLE_ROLE_GROUP_ID)
    SecurableResourceGroupRole create(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @NonNull Role role, @NonNull UUID userGroupId)

    @Delete(Paths.SECURABLE_ROLE_GROUP_ID)
    HttpResponse delete(@NonNull String securableResourceDomainType, @NonNull UUID securableResourceId, @NonNull Role role, @NonNull UUID userGroupId)
}
