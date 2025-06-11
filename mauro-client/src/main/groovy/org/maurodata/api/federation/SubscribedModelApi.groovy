package org.maurodata.api.federation

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import org.maurodata.api.Paths
import org.maurodata.api.MauroApi
import org.maurodata.domain.facet.federation.SubscribedModel
import org.maurodata.domain.facet.federation.SubscribedModelFederationParams
import org.maurodata.web.ListResponse

@MauroApi
interface SubscribedModelApi {

    @Get(Paths.SUBSCRIBED_MODELS_LIST)
    ListResponse<SubscribedModel> listAll(@NonNull UUID subscribedCatalogueId)

    @Get(Paths.SUBSCRIBED_MODELS_ID)
    SubscribedModel show(@NonNull UUID subscribedCatalogueId, @NonNull UUID subscribedModelId)

    @Post(Paths.SUBSCRIBED_MODELS_LIST)
    SubscribedModel create(@NonNull UUID subscribedCatalogueId, @Body @NonNull SubscribedModelFederationParams subscribedModelFederationParams)

    @Delete(Paths.SUBSCRIBED_MODELS_ID)
    HttpResponse delete(@NonNull UUID subscribedCatalogueId, @NonNull UUID subscribedModelId, @Body @Nullable SubscribedModel subscribedModel)
}


