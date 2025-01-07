package uk.ac.ox.softeng.mauro.test.domain

import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class SubscribedCatalogueTypeTest extends Specification{

    void 'subscribedCatalogueType -with label can be constructed'(){
        when:
        ObjectMapper mapper = new ObjectMapper()
        SubscribedCatalogueType type1 = mapper.readValue("\"Mauro JSON\"", SubscribedCatalogueType.class)
        then:
        type1
        type1.label == 'Mauro JSON'
    }

    void 'subscribed catalogue type can be constructed from label string'(){
        when:
        SubscribedCatalogueType type = SubscribedCatalogueType.fromString("Atom")
        then:
        type
        type.label == 'Atom'
    }

}
