package uk.ac.ox.softeng.mauro.federation

import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClient
import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClientConfiguration
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Slf4j
@Singleton
@Replaces(FederationClient)
class MockFederationClient extends FederationClient {

    private FederationClientConfiguration federationClientConfiguration

    private String jsonString

    @Inject
    private ObjectMapper objectMapper

    MockFederationClient(FederationClientConfiguration federationClientConfiguration) {
        super(federationClientConfiguration)
        jsonString = new File('src/test/resources/subscribedCataloguePublishedModels.json').text
    }

    @Override
    void clientSetup(SubscribedCatalogue subscribedCatalogue) {
    }

    @Override
    Map<String, Object> fetchFederatedClientDataAsMap(SubscribedCatalogue subscribedCatalogue, String requestPath) {
        if (requestPath.endsWith("newerVersions")){
            jsonString = getNewerVersionsTestData()
        }
        Map<String, Object> federatedDataAsMap = objectMapper.readValue(jsonString, Map.class)
        federatedDataAsMap
    }

    @Override
    byte[] retrieveBytesFromClient(SubscribedCatalogue subscribedCatalogue,String url) {
        return new File('src/test/resources/federatedPublishedModelsBytesAsText.json').getBytes()
    }

    String getNewerVersionsTestData() {
        jsonString = new File('src/test/resources/subscribedCataloguePublishedModelsNewerVersions.json').text
    }
}
