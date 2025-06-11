package org.maurodata.api.federation

import org.maurodata.domain.facet.federation.SubscribedCatalogueAuthenticationType
import org.maurodata.domain.facet.federation.SubscribedCatalogueType

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.federation.PublishedModel
import org.maurodata.domain.facet.federation.SubscribedCatalogue
import org.maurodata.domain.facet.federation.response.SubscribedCataloguesPublishedModelsNewerVersions
import org.maurodata.web.ListResponse

@MauroApi
interface SubscribedCatalogueApi {

    @Get(Paths.ADMIN_SUBSCRIBED_CATALOGUES_LIST)
    ListResponse<SubscribedCatalogue> listAll()

    @Get(Paths.SUBSCRIBED_CATALOGUES_ID)
    SubscribedCatalogue show(@NonNull UUID subscribedCatalogueId)

    @Get(Paths.SUBSCRIBED_CATALOGUES_LIST)
    ListResponse<SubscribedCatalogue> listSubscribedCatalogues(@Nullable @QueryValue Integer max)

    @Post(Paths.ADMIN_SUBSCRIBED_CATALOGUES_LIST)
    SubscribedCatalogue create(@Body @NonNull SubscribedCatalogue subscribedCatalogue)

    @Put(Paths.ADMIN_SUBSCRIBED_CATALOGUES_ID)
    SubscribedCatalogue update(@NonNull UUID subscribedCatalogueId, @Body @NonNull SubscribedCatalogue subscribedCatalogue)

    @Get(Paths.SUBSCRIBED_CATALOGUES_TYPES)
    ListResponse<SubscribedCatalogueType> types()

    @Get(Paths.SUBSCRIBED_CATALOGUES_AUTHENTICATION_TYPES)
    ListResponse<SubscribedCatalogueAuthenticationType> authenticationTypes()

    @Get(Paths.ADMIN_SUBSCRIBED_CATALOGUES_TEST_CONNECTION)
    HttpResponse testConnection(@NonNull UUID subscribedCatalogueId)


    @Get(Paths.SUBSCRIBED_CATALOGUES_PUBLISHED_MODELS)
    ListResponse<PublishedModel> publishedModels(@NonNull UUID subscribedCatalogueId)

    @Get(Paths.SUBSCRIBED_CATALOGUES_PUBLISHED_MODELS_NEWER_VERSIONS)
    SubscribedCataloguesPublishedModelsNewerVersions publishedModelsNewerVersions(@NonNull UUID subscribedCatalogueId, @NonNull String publishedModelId)


    @Delete(Paths.ADMIN_SUBSCRIBED_CATALOGUES_ID)
    HttpResponse delete(@NonNull UUID subscribedCatalogueId, @Body @Nullable SubscribedCatalogue subscribedCatalogue)
}





