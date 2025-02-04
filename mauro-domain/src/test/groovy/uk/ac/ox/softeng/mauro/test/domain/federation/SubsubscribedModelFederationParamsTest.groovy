package uk.ac.ox.softeng.mauro.test.domain.federation

import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModel
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedModelFederationParams

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class SubsubscribedModelFederationParamsTest extends Specification {
    @Inject
    ObjectMapper mapper

    void foo() {
        given:
        SubscribedModel subscribedModel = new SubscribedModel().tap {
            subscribedModelId = "subscribedModelId"
            subscribedModelType = "DataModel"
            folderId = UUID.randomUUID()
        }
        SubscribedModelFederationParams subscribedModelFederationParams = new SubscribedModelFederationParams()
        subscribedModelFederationParams.subscribedModel = subscribedModel

        when:
        String output = mapper.writeValueAsString(subscribedModelFederationParams)

        then:
        output


    }
}
