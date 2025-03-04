package uk.ac.ox.softeng.mauro.controller.federation.client

import uk.ac.ox.softeng.mauro.federation.MockFederationClient

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

class MockSAXErrorFederationClient extends MockFederationClient{

    MockSAXErrorFederationClient(FederationClientConfiguration federationClientConfiguration) {
        super(federationClientConfiguration)
    }

    @Override
    GPathResult getSubscribedCatalogueModelsFromAtomFeed() {
        return new XmlSlurper().parse(new File('src/test/resources/bad-xml-federated-data.xml'))
    }
}
