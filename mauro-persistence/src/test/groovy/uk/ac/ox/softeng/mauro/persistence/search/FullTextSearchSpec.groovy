package uk.ac.ox.softeng.mauro.persistence.search


import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRepository

import java.time.LocalDate
import java.sql.Date

@ContainerizedTest
class FullTextSearchSpec extends Specification {

    @Inject
    @Shared
    DataModelContentRepository dataModelContentRepository

    @Inject
    DataClassRepository dataClassRepository

    @Inject
    @Shared
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Inject
    SearchRepository searchRepository

    @Shared
    UUID folderId

    @Shared
    DataModel dataModel1

    @Shared
    DataModel dataModel2

    def setupSpec() {
        Folder myFirstFolder = folderRepository.save(new Folder(
            label: "My Search Folder"
        ))

        dataModel1 = DataModel.build {
            label "An import model"
            description "The description goes here"
            folder myFirstFolder
            dataClass {
                label "First class"
            }
            dataClass {
                label "Second class"
            }
            dataClass {
                label "Twentieth class"
                description "First description"
            }
        }
        dataModel2 = DataModel.build {
            label "Another import model"
            description ""
            folder myFirstFolder
        }
        dataModelContentRepository.saveWithContent(dataModel1)
        dataModelContentRepository.saveWithContent(dataModel2)
    }

    def "test search results across all domains" () {
        expect:
        List<SearchResultsDTO> searchResults = searchRepository.search(searchTerm)
        isSortedByRank(searchResults)
        searchResults.label == labels

        where:

        searchTerm          | labels
        'first'             | ['First class', 'Twentieth class']
        'class'             | ['First class', 'Second class', 'Twentieth class']
        'classes'           | ['First class', 'Second class', 'Twentieth class']
        'second'            | ['Second class']
        'description'       | ['An import model', 'Twentieth class']
        'import'            | ['Another import model', 'An import model']
        'nothing'           | []
        'first & class'     | ['First class', 'Twentieth class']
        'first | class'     | ['First class', 'Second class', 'Twentieth class']
        "'first class'"     | ['First class']
    }

    def "test prefix search results across all domains" () {
        expect:
        List<SearchResultsDTO> searchResults = searchRepository.prefixSearch(searchTerm)
        isSortedByLabel(searchResults)
        searchResults.label == labels

        where:

        searchTerm          | labels
        'f'                 | ['First class']
        'a'                 | ['An import model', 'Another import model']
        'first'             | ['First class']
        'First'             | ['First class']
        'class'             | []
        'second'            | ['Second class']
        'an import'         | ['An import model']
        'nothing'           | []
        'first class'       | ['First class']
    }

    def "test search results across particular domains" () {
        expect:
        List<SearchResultsDTO> searchResults = searchRepository.search(searchTerm, domainTypes)
        isSortedByRank(searchResults)
        searchResults.label == labels

        where:

        searchTerm          | domainTypes           | labels
        'first'             | []                    | ['First class', 'Twentieth class']
        'class'             | ['DataClass']         | ['First class', 'Second class', 'Twentieth class']
        'class'             | ['DataElement']       | []
        'description'       | []                    | ['An import model', 'Twentieth class']
        'description'       | ['DataModel']         | ['An import model']
        'description'       | ['DataClass']         | ['Twentieth class']
        'nothing'           | []                    | []
    }

    def "test prefix search results across particular domains" () {

        expect:
        List<SearchResultsDTO> searchResults = searchRepository.prefixSearch(searchTerm, domainTypes)
        isSortedByLabel(searchResults)
        searchResults.label == labels

        where:

        searchTerm          | domainTypes                           | labels
        'f'                 | []                                  | ['First class']
        'a'                 | []                                  | ['An import model', 'Another import model']
        'first'             | []                                  | ['First class']
        'first'             | ['DataClass']                       | ['First class']
        'first'             | ['DataModel']                       | []
        'first'             | ['DataModel', 'DataElement']        | []
        'first'             | ['DataClass', 'DataElement']        | ['First class']
        'First'             | []                                  | ['First class']
        'First'             | ['DataClass']                       | ['First class']
        'First'             | ['DataModel']                       | []
        'First'             | ['DataModel', 'DataElement']        | []
        'First'             | ['DataClass', 'DataElement']        | ['First class']
        'class'             | []                                  | []
        'second'            | ['DataClass']                       | ['Second class']
        'an import'         | ['DataModel']                       | ['An import model']
        'nothing'           | []                                  | []
        'first class'       | []                                  | ['First class']
        'first class'       | ['DataClass']                       | ['First class']
        'first class'       | ['DataElement']                     | []
    }


    def "Test searching with date parameters"() {
        // Test dates
        LocalDate today = LocalDate.now()
        LocalDate tomorrow = today.plusDays(1)

        expect:
        List<SearchResultsDTO> searchResults = searchRepository.search(searchTerm, [])
        List<SearchResultsDTO> searchResultsCreatedBeforeToday = searchRepository.search(searchTerm, [], null, Date.valueOf(today))
        List<SearchResultsDTO> searchResultsCreatedBeforeTomorrow = searchRepository.search(searchTerm, [], null, Date.valueOf(tomorrow))
        List<SearchResultsDTO> searchResultsCreatedAfterToday = searchRepository.search(searchTerm, [], null, null, Date.valueOf(today))
        List<SearchResultsDTO> searchResultsCreatedAfterTomorrow = searchRepository.search(searchTerm, [], null, null, Date.valueOf(tomorrow))
        List<SearchResultsDTO> searchResultsUpdatedBeforeToday = searchRepository.search(searchTerm, [], null, null, null, Date.valueOf(today))
        List<SearchResultsDTO> searchResultsUpdatedBeforeTomorrow = searchRepository.search(searchTerm, [], null, null, null, Date.valueOf(tomorrow))
        List<SearchResultsDTO> searchResultsUpdatedAfterToday = searchRepository.search(searchTerm, [], null, null, null, null, Date.valueOf(today))
        List<SearchResultsDTO> searchResultsUpdatedAfterTomorrow = searchRepository.search(searchTerm, [], null, null, null, null, Date.valueOf(tomorrow))

        searchResults.label == labels
        searchResultsCreatedBeforeToday.label == []
        searchResultsCreatedBeforeTomorrow.label == labels
        searchResultsCreatedAfterToday.label == labels
        searchResultsCreatedAfterTomorrow.label == []
        searchResultsUpdatedBeforeToday.label == []
        searchResultsUpdatedBeforeTomorrow.label == labels
        searchResultsUpdatedAfterToday.label == labels
        searchResultsUpdatedAfterTomorrow.label == []

        where:
        searchTerm          | labels
        'first'             | ['First class', 'Twentieth class']
        'class'             | ['First class', 'Second class', 'Twentieth class']
        'classes'           | ['First class', 'Second class', 'Twentieth class']
        'second'            | ['Second class']
        'description'       | ['An import model', 'Twentieth class']
        'import'            | ['Another import model', 'An import model']
        'nothing'           | []
        'first & class'     | ['First class', 'Twentieth class']
        'first | class'     | ['First class', 'Second class', 'Twentieth class']
        "'first class'"     | ['First class']

    }

    def "test search results within a model" () {
        expect:
        List<SearchResultsDTO> searchResults = searchRepository.search(searchTerm, [], modelId)
        isSortedByRank(searchResults)
        searchResults.label == labels

        where:

        searchTerm          | modelId               | labels
        'first'             | null                  | ['First class', 'Twentieth class']
        'first'             | dataModel1.id         | ['First class', 'Twentieth class']
        'first'             | dataModel2.id         | []
        'class'             | null                  | ['First class', 'Second class', 'Twentieth class']
        'class'             | dataModel1.id         | ['First class', 'Second class', 'Twentieth class']
        'class'             | dataModel2.id         | []
        'import'            | null                  | ['Another import model', 'An import model']
        'import'            | dataModel1.id         | ['An import model']
        'import'            | dataModel2.id         | ['Another import model']
    }

    def "test prefix search results within a model" () {

        expect:
        List<SearchResultsDTO> searchResults = searchRepository.prefixSearch(searchTerm, [], dataModelId)
        isSortedByLabel(searchResults)
        searchResults.label == labels

        where:

        searchTerm          | dataModelId                         | labels
        'f'                 | null                                | ['First class']
        'f'                 | dataModel1.id                       | ['First class']
        'f'                 | dataModel2.id                       | []
        'a'                 | null                                | ['An import model', 'Another import model']
        'a'                 | dataModel1.id                       | ['An import model']
        'a'                 | dataModel2.id                       | ['Another import model']
    }



    private boolean isSortedByRank(List<SearchResultsDTO> results) {
        results.size() < 2 || (1..<results.size()).every {
            results[it - 1].tsRank > results[it].tsRank
            || (results[it - 1].tsRank == results[it].tsRank && results[it - 1].label <= results[it].label)
        }
    }

    private boolean isSortedByLabel(List<SearchResultsDTO> results) {
        results.size() < 2 || (1..<results.size()).every {
            (results[it - 1].label.compareToIgnoreCase(results[it].label) <= 0)
        }
    }

}
