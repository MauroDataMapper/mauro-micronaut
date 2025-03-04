package uk.ac.ox.softeng.mauro.api.federation

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.*
import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.facet.federation.response.PublishedModelResponse

@MauroApi
interface PublishApi {

    @Get(Paths.PUBLISHED_MODELS)
    PublishedModelResponse show()

    @Get(Paths.PUBLISHED_MODELS_NEWER_VERSIONS)
    PublishedModelResponse newerVersions(@NonNull UUID publishedModelId)

}
