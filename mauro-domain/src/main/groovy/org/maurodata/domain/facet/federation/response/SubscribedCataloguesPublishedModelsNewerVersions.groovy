package org.maurodata.domain.facet.federation.response

import org.maurodata.domain.facet.federation.PublishedModel

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.Transient

import java.time.Instant

@CompileStatic
@Slf4j
@JsonInclude(JsonInclude.Include.ALWAYS)
class SubscribedCataloguesPublishedModelsNewerVersions {
    @NonNull
    @Transient
    Instant lastUpdated

    @Transient
    @JsonProperty("newerPublishedModels")
    List<PublishedModel> newerPublishedModels = new ArrayList<PublishedModel>()


}
