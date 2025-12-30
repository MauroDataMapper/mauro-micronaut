package org.maurodata.api.facet

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.VersionLinkDTO
import org.maurodata.domain.facet.VersionLink
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@MauroApi
interface VersionLinksApi extends FacetApi<VersionLink> {

    @Get(Paths.VERSION_LINKS_LIST)
    ListResponse<VersionLinkDTO> list(@NonNull String domainType, @NonNull UUID domainId)

    @Get(Paths.VERSION_LINKS_LIST_PAGED)
    ListResponse<VersionLinkDTO> list(@NonNull String domainType, @NonNull UUID domainId, @Nullable PaginationParams params)

//    @Post(Paths.VERSION_LINKS_LIST)
//    VersionLinkDTO create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull SemanticLinkCreateDTO semanticLink)
}