package uk.ac.ox.softeng.mauro.search

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

@ContainerizedTest
class SearchIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    DataModelContentRepository dataModelContentRepository

    @Inject
    ObjectMapper objectMapper

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId1

    @Shared
    UUID dataModelId2

    void setupSpec() {
        Folder folder = (Folder) POST("$FOLDERS_PATH", [label: 'Test Folder'], Folder)
        folderId = folder.id

        DataModel dataModel1 = DataModel.build {
            label "My First Test DataModel"
            description "Description of my first model"
            primitiveType {
                label "String"
            }
            primitiveType {
                label "Date"
            }
            dataClass {
                label "My first DataClass"
                description "Description of my first class"
            }
        }
        DataModel dataModel2 = DataModel.build {
            label "My Second Test DataModel"
            description "Description of my second model"
            primitiveType {
                label "String"
            }
            primitiveType {
                label "Date"
            }
            dataClass {
                label "My first DataClass"
                description "Description of my first class in another data model"
            }
        }
        dataModelId1 = importDataModel(dataModel1, folder)
        dataModelId2 = importDataModel(dataModel2, folder)

    }

    def "Test Get Search"() {

        expect:

        ListResponse<SearchResultsDTO> searchResults = (ListResponse<SearchResultsDTO>) GET("/search?${queryParams}", ListResponse<SearchResultsDTO>)
        searchResults.items.label == expectedLabels

        where:

        queryParams                                                     | expectedLabels
        "searchTerm=first"                                              | ["My first DataClass", "My First Test DataModel", "My first DataClass"]
        "searchTerm=first&domainTypes=DataModel"                        | ["My First Test DataModel"]
        "searchTerm=first&domainTypes=DataClass"                        | ["My first DataClass", "My first DataClass"]
        "searchTerm=first&domainTypes=DataClass&domainTypes=DataModel"  | ["My first DataClass", "My First Test DataModel", "My first DataClass"]
        "searchTerm=first&domainTypes=DataType"                         | []
        "searchTerm=first&withinModelId=$dataModelId1"                  | ["My first DataClass", "My First Test DataModel"]
        "searchTerm=first&withinModelId=$dataModelId2"                  | ["My first DataClass"]

    }
}
