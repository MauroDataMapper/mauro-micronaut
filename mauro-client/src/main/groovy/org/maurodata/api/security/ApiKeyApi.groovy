package org.maurodata.api.security

import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.security.ApiKey
import org.maurodata.web.ListResponse

@MauroApi
interface ApiKeyApi {

    @Get(Paths.API_KEY_LIST)
    ListResponse<ApiKey> index(UUID userId)

    @Post(Paths.API_KEY_LIST)
    ApiKey create(UUID userId, @Body ApiKey apiKey)

    @Get(Paths.API_KEY_ID)
    ApiKey show(UUID userId, UUID apiKeyId)

    @Delete(Paths.API_KEY_ID)
    HttpStatus delete(UUID userId, UUID apiKeyId)

    @Put(Paths.API_KEY_ENABLE)
    ApiKey enable(UUID userId, UUID apiKeyId)

    @Put(Paths.API_KEY_DISABLE)
    ApiKey disable(UUID userId, UUID apiKeyId)

    @Put(Paths.API_KEY_REFRESH)
    ApiKey refresh(UUID userId, UUID apiKeyId, long expireInDays)

}
