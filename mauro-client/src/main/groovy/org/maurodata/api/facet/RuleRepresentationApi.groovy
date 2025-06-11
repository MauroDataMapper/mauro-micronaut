package org.maurodata.api.facet

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.RuleRepresentation
import org.maurodata.web.ListResponse

@MauroApi
interface RuleRepresentationApi {


    @Post(Paths.RULE_REPRESENTATIONS_LIST)
    RuleRepresentation create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                              @Body RuleRepresentation ruleRepresentation)

    @Get(Paths.RULE_REPRESENTATIONS_LIST)
    ListResponse<RuleRepresentation> list(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId)

    @Get(Paths.RULE_REPRESENTATIONS_ID)
    RuleRepresentation show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                              @NonNull UUID id)

    @Put(Paths.RULE_REPRESENTATIONS_ID)
    RuleRepresentation update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                                 @NonNull UUID id, @Body @NonNull RuleRepresentation ruleRepresentation)

    @Delete(Paths.RULE_REPRESENTATIONS_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID ruleId,
                        @NonNull UUID id)

}