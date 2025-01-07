package uk.ac.ox.softeng.mauro.domain.federation

import uk.ac.ox.softeng.mauro.domain.authority.Authority

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Transient

import java.time.Instant

@CompileStatic
@Introspected
class PublishedModelResponse {
    @Nullable
    @Transient
    @JsonProperty('authority')
    AuthorityResponse authorityResponse

    @Transient
    Instant lastUpdated = publishedModels?.max {it.lastUpdated}?.lastUpdated ?: Instant.now()

    @Transient
    List<PublishedModel> publishedModels = []

    PublishedModelResponse(AuthorityResponse authorityResponse, List<PublishedModel> publishedModels) {
        this.authorityResponse = authorityResponse
        this.publishedModels = publishedModels
    }
}