package org.maurodata.api.facet

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@MauroApi
interface SemanticLinksApi extends FacetApi<SemanticLink> {

    @Get(Paths.SEMANTIC_LINKS_LIST)
    ListResponse<SemanticLinkDTO> list(@NonNull String domainType, @NonNull UUID domainId)

    @Get(Paths.SEMANTIC_LINKS_LIST_PAGED)
    ListResponse<SemanticLinkDTO> list(@NonNull String domainType, @NonNull UUID domainId, @Nullable PaginationParams params)

    @Post(Paths.SEMANTIC_LINKS_LIST)
    SemanticLinkDTO create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull SemanticLinkCreateDTO semanticLink)
}