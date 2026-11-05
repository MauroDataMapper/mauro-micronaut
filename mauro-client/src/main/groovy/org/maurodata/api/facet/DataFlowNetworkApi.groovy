package org.maurodata.api.facet

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.link.ItemLinkDTO
import org.maurodata.domain.datamodel.DataComponent
import org.maurodata.web.ListResponse

@MauroApi
interface DataFlowNetworkApi {
    @Get(Paths.DATA_FLOW_NETWORK_SOURCE_LIST)
    ListResponse<ItemLinkDTO> list(@NonNull String domainType, @NonNull UUID domainId)

    @Post(Paths.DATA_FLOW_NETWORK_SOURCE_LIST)
    ItemLinkDTO create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull DataFlowSourceCreateDTO request)

    @Get(Paths.DATA_FLOW_NETWORK_SOURCE_ID)
    ItemLinkDTO show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

    @Delete(Paths.DATA_FLOW_NETWORK_SOURCE_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

    @Get(Paths.DATA_FLOW_NETWORK_COMPONENT_LIST)
    ListResponse<DataComponent> listDataFlowNetworkComponents(@NonNull UUID dataModelId)

    @Post(Paths.DATA_FLOW_NETWORK_COMPONENT_LIST)
    DataComponent createDataFlowNetworkComponent(@NonNull UUID dataModelId, @Body @NonNull DataComponentCreateDTO request)

    @Get(Paths.DATA_FLOW_NETWORK_COMPONENT_ID)
    DataComponent showDataFlowNetworkComponent(@NonNull UUID dataModelId, @NonNull UUID id)

    @Delete(Paths.DATA_FLOW_NETWORK_COMPONENT_ID)
    HttpResponse deleteDataFlowNetworkComponent(@NonNull UUID dataModelId, @NonNull UUID id)
}
