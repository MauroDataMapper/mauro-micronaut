package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.facet.SemanticLink
import uk.ac.ox.softeng.mauro.web.ListResponse
import uk.ac.ox.softeng.mauro.web.PaginationParams

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
    SemanticLinkDTO create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull SemanticLinkDTO semanticLink)
}