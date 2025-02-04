package uk.ac.ox.softeng.mauro.domain.facet.federation.response

import uk.ac.ox.softeng.mauro.domain.facet.federation.PublishedModel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.Transient

import java.time.Instant

@CompileStatic
@Slf4j
class SubscribedCataloguesPublishedModelsNewerVersions {
    @NonNull
    @Transient
    Instant lastUpdated

    @Transient

    List<PublishedModel> newerPublishedModels


}
