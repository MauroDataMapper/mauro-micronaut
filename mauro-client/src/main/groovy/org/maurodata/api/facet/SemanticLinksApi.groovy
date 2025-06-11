package org.maurodata.api.facet

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Get

@MauroApi
interface SemanticLinksApi {

    @Get(Paths.SEMANTIC_LINKS_LIST)
    ListResponse list(@NonNull String domainType, @NonNull UUID domainId )

}