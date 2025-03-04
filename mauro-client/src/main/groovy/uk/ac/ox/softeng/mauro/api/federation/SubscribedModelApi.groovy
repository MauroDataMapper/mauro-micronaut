package uk.ac.ox.softeng.mauro.api.federation

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModelFederationParams
import uk.ac.ox.softeng.mauro.web.ListResponse

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


