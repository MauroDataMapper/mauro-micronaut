package uk.ac.ox.softeng.mauro.test.domain.federation

import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.federation.PublishedModel
import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.mauro.domain.federation.converter.SubscribedCatalogueConverter
import uk.ac.ox.softeng.mauro.domain.federation.converter.SubscribedCatalogueConverterService

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import spock.lang.Unroll

import static uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogueType.MAURO_JSON

@MicronautTest
class SubscribedCatalogueConverterServiceTest extends Specification {

    @Inject
    ObjectMapper mapper

    @Inject
    SubscribedCatalogueConverterService subscribedCatalogueConverterService

    void "test subscribedCatalogueConverter loads"() {
        when:
        int numberOfConverters = subscribedCatalogueConverterService.getNumberOfConverters()
        then:
        numberOfConverters > 0

        when:
        SubscribedCatalogueConverter converter = subscribedCatalogueConverterService.getSubscribedCatalogueConverter(MAURO_JSON)
        then:
        converter
        converter.handles(SubscribedCatalogueType.MAURO_JSON)
    }

    @Unroll
    void "subscribedCatalogueConverter -convert #dataFile to PublishedModel with #expectedLinksSize and #expectedAuthorityLabel, #expectedAuthorityUrl"() {
        given:
        String jsonData = new File(dataFile).text
        Map<String, Object> dataAsMap = mapper.readValue(jsonData, Map.class)

        when:
        List<PublishedModel> publishedModels = subscribedCatalogueConverterService.getSubscribedCatalogueConverter(MAURO_JSON).toPublishedModels(dataAsMap).v2

        then:
        publishedModels
        publishedModels.size() == 1
        PublishedModel publishedModel = publishedModels.first()
        publishedModel.modelId == "0b97751d-b6bf-476c-a9e6-95d3352e8008"
        publishedModel.links.size() == expectedLinksSize

        when:
        Authority authority = subscribedCatalogueConverterService.getSubscribedCatalogueConverter(MAURO_JSON).toPublishedModels(dataAsMap).v1

        then:
        authority.label == expectedAuthorityLabel
        authority.url == expectedAuthorityUrl


        where:
        dataFile                                                       | expectedLinksSize | expectedAuthorityLabel | expectedAuthorityUrl
        'src/test/resources/publishedModels.json'                      | 9                 | 'Mauro Sandbox'        | "http://modelcatalogue.cs.ox.ac.uk/sandbox"
        'src/test/resources/publishedModelsWithNoLinks.json'           | 0                 | 'Mauro Sandbox'        | "http://modelcatalogue.cs.ox.ac.uk/sandbox"
        'src/test/resources/publishedModelsNullAuthorityAndDates.json' | 9                 | null                   | null
    }

}
