package org.maurodata.controller.federation.client


import org.xml.sax.SAXParseException
import spock.lang.Specification

class FederationClientTest extends Specification {

    void 'test Parse exception - should throw '() {
        FederationClient federationClient = new MockSAXErrorFederationClient()
        when:
        federationClient.getSubscribedCatalogueModelsFromAtomFeed()
        then:
        Exception exception = thrown()
        exception.class.isAssignableFrom(SAXParseException.class)
    }

    void 'test IO exception - should throw '() {
        FederationClient federationClient = new MockIOErrorFederationClient()
        when:
        federationClient.getSubscribedCatalogueModelsFromAtomFeed()
        then:
        Exception exception = thrown()
        exception.class.isAssignableFrom(FileNotFoundException.class)
    }
}
