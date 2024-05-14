package uk.ac.ox.softeng.mauro.persistence.search

import spock.lang.Unroll
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRepository


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

    def setupSpec() {
        Folder myFirstFolder = folderRepository.save(new Folder(
            label: "My Search Folder"
        ))

        DataModel dataModel1 = DataModel.build {
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
        DataModel dataModel2 = DataModel.build {
            label "Another import model"
            description ""
            folder myFirstFolder
        }
        dataModelContentRepository.saveWithContent(dataModel1)
        dataModelContentRepository.saveWithContent(dataModel2)
    }

    def "test search results across all domains" () {
        expect:
        List<SearchDTO> searchResults = searchRepository.search(searchTerm)
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

    def "test search results across particular domains" () {
        expect:
        List<SearchDTO> searchResults = searchRepository.search(searchTerm, domainTypes)
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


    private boolean isSortedByRank(List<SearchDTO> results) {
        results.size() < 2 || (1..<results.size()).every {
            results[it - 1].tsRank > results[it].tsRank
            || (results[it - 1].tsRank == results[it].tsRank && results[it - 1].label <= results[it].label)
        }
    }

}
