package org.maurodata.api.federation

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.*
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.federation.response.PublishedModelResponse

@MauroApi
interface PublishApi {

    @Get(Paths.PUBLISHED_MODELS)
    PublishedModelResponse show()

    @Get(Paths.PUBLISHED_MODELS_NEWER_VERSIONS)
    PublishedModelResponse newerVersions(@NonNull UUID publishedModelId)

}
