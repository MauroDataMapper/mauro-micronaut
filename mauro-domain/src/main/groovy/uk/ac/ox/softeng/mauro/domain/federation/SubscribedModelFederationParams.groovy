package uk.ac.ox.softeng.mauro.domain.federation

import uk.ac.ox.softeng.mauro.importdata.ImportMetadata

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import jakarta.validation.constraints.NotNull

@CompileStatic
@Slf4j
class SubscribedModelFederationParams {
    @NonNull
    String url
    @NonNull
    String contentType
    SubscribedModel subscribedModel
    @JsonProperty("importerProviderService")
    ImportMetadata importMetadata


    @JsonCreator
    SubscribedModelFederationParams() {
    }

    SubscribedModelFederationParams(@NonNull String url, @NonNull String contentType,
                                    @Nullable ImportMetadata importMetadata,
                                    @Nullable SubscribedModel subscribedModel) {
        this.url = url
        this.contentType = contentType
        this.importMetadata =  importMetadata
        this.subscribedModel = subscribedModel
    }


    boolean hasImporterPlugin() {
        importMetadata
    }
}
