package org.maurodata.api.domainexports

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.security.ApiKey
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface DomainExportsApi {

    @Get(Paths.DOMAIN_EXPORTS_LIST_PAGED)
    ListResponse<Map> index(@Nullable PaginationParams params)
}
