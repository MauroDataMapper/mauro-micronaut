package uk.ac.ox.softeng.mauro.persistence.search


import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll


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

    def setupSpec() {

        Folder myFirstFolder = folderRepository.save(new Folder(
            label: "My first Folder"
        ))

        DataModel dataModel = DataModel.build {
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
        System.err.println("Importing model")
        DataModel importedModel = dataModelContentRepository.saveWithContent(dataModel)

    }

    def test() {
        expect:
        List<DataClassSearchDTO> searchResults = dataClassRepository.search(searchTerm)
        searchResults.label == labels

        where:

        searchTerm          | labels
        'first'             | ['First class', 'Twentieth class']
        'class'             | ['First class', 'Second class', 'Twentieth class']
        'classes'           | ['First class', 'Second class', 'Twentieth class']
        'second'            | ['Second class']
        'description'       | ['Twentieth class']
        'nothing'           | []
        'first & class'     | ['First class', 'Twentieth class']
        'first | class'     | ['First class', 'Second class', 'Twentieth class']
        "'first class'"     | ['First class']
    }


}
