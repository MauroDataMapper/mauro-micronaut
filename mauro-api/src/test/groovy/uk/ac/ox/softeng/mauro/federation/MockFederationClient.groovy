package uk.ac.ox.softeng.mauro.federation

import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClient
import uk.ac.ox.softeng.mauro.controller.federation.client.FederationClientConfiguration
import uk.ac.ox.softeng.mauro.domain.facet.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import io.micronaut.context.annotation.Replaces
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Slf4j
@Singleton
@Replaces(FederationClient)
class MockFederationClient extends FederationClient {

    private String publishedModelsString
    private String newerVersionsString

    @Inject
    private ObjectMapper objectMapper

    MockFederationClient(FederationClientConfiguration federationClientConfiguration) {
        super(federationClientConfiguration)
        publishedModelsString = new File('src/test/resources/subscribedCataloguePublishedModels.json').text
        newerVersionsString = new File('src/test/resources/subscribedCataloguePublishedModelsNewerVersions.json').text
    }

    @Override
    void clientSetup(SubscribedCatalogue subscribedCatalogue) {
    }

    @Override
    Map<String, Object> fetchFederatedClientDataAsMap(SubscribedCatalogue subscribedCatalogue, String requestPath) {
        if (requestPath.endsWith("$BaseIntegrationSpec.TEST_MODEL_ID$BaseIntegrationSpec.NEWER_VERSIONS")) {
            return objectMapper.readValue(newerVersionsString, Map.class)
        } else {
            return objectMapper.readValue(publishedModelsString, Map.class)
        }
    }

    @Override
    byte[] retrieveBytesFromClient(SubscribedCatalogue subscribedCatalogue,String url) {
        return new File('src/test/resources/federatedPublishedModelsBytesAsText.json').getBytes()
    }

    @Override
    GPathResult getSubscribedCatalogueModelsFromAtomFeed() {
        return new XmlSlurper().parse(new File('src/test/resources/xml-federated-data.xml'))
    }

}
