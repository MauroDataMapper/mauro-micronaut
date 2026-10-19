package org.maurodata.api.link

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.web.ListResponse

@MauroApi
@CompileStatic
interface ItemLinkGraphApi {
    @Get(Paths.ITEM_LINK_KINDS)
    ListResponse<ItemLinkKindDTO> listSupportedLinkKinds()

    @Get(Paths.ITEM_LINK_GRAPH)
    ItemLinkGraphDTO traverseIncomingLinks(@NonNull String domainType,
                                           @NonNull UUID domainId,
                                           @QueryValue("linkTypes") @NonNull List<String> linkTypes)
}
