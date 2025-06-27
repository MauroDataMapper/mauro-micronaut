package org.maurodata.api.facet

import org.maurodata.web.PaginationParams

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.Rule
import org.maurodata.web.ListResponse

@MauroApi
interface RuleApi extends FacetApi<Rule> {

    @Get(Paths.RULE_LIST)
    ListResponse<Rule> list(@NonNull String domainType, @NonNull UUID domainId)

    @Get(Paths.RULE_LIST_PAGED)
    ListResponse<Rule> list(@NonNull String domainType, @NonNull UUID domainId, @Nullable PaginationParams params)

    @Get(Paths.RULE_ID)
    Rule show(@NonNull String domainType, @NonNull UUID domainId, UUID id)

    @Put(Paths.RULE_ID)
    Rule update(@NonNull String domainType, @NonNull UUID domainId, UUID id, @Body @NonNull Rule rule)

    @Post(Paths.RULE_LIST)
    Rule create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Rule rule)

    @Delete(Paths.RULE_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, UUID id)

}
