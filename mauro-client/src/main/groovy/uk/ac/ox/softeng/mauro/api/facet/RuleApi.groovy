package uk.ac.ox.softeng.mauro.api.facet

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.web.ListResponse

@MauroApi
interface RuleApi extends FacetApi<Rule> {

    @Get(Paths.RULE_LIST)
    ListResponse<Rule> list(@NonNull String domainType, @NonNull UUID domainId )

    @Get(Paths.RULE_ID)
    Rule show(@NonNull String domainType, @NonNull UUID domainId, UUID id)

    @Put(Paths.RULE_ID)
    Rule update(@NonNull String domainType, @NonNull UUID domainId, UUID id, @Body @NonNull Rule rule)

    @Post(Paths.RULE_LIST)
    Rule create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Rule rule)

    @Delete(Paths.RULE_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, UUID id)

}
