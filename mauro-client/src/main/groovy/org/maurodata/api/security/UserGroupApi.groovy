package org.maurodata.api.security

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.security.UserGroup

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post

@MauroApi
interface UserGroupApi {

    @Post(Paths.USER_GROUP_LIST)
    UserGroup create(@Body @NonNull UserGroup userGroup)

}
