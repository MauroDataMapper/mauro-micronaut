package uk.ac.ox.softeng.mauro.api

import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

@MicronautTest
class PathPopulationSpec extends Specification {

    static final String randomUUID = UUID.randomUUID()

    void "test path population"() {
        given:


        expect:

        expected == PathPopulation.populatePath(original, replacements)

        where:

        original            | replacements                  | expected
        "{id}"              | [id: randomUUID]              | randomUUID
        "{/id}"              | [id: randomUUID]              | randomUUID

    }

}
