package org.maurodata.search

import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.testing.CommonDataSpec

import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.web.ListResponse

@ContainerizedTest
@Singleton
class SearchIntegrationSpec extends CommonDataSpec {

    @Inject
    DataModelContentRepository dataModelContentRepository

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId1

    @Shared
    UUID dataModelId2

    void setupSpec() {
        Folder folder = folderApi.create(new Folder(label: 'Test Folder'))
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

        SearchRequestDTO searchRequestDTO = new SearchRequestDTO(
            searchTerm: searchTerm,
            domainTypes: domainTypes,
            withinModelId: withinModelId)

        ListResponse<SearchResultsDTO> searchResults = searchApi.searchGet(searchRequestDTO)
        searchResults.items.label == expectedLabels

        where:

        searchTerm  | domainTypes                   | withinModelId | expectedLabels
        "first"     | []                            | null          | ["My first DataClass", "My First Test DataModel", "My first DataClass"]
        "first"     | ["DataModel"]                 | null          | ["My First Test DataModel"]
        "first"     | ["DataClass"]                 | null          | ["My first DataClass", "My first DataClass"]
        "first"     | ["DataClass", "DataModel"]    | null          | ["My first DataClass", "My First Test DataModel", "My first DataClass"]
        "first"     | ["DataType"]                  | null          | []
        "first"     | []                            | dataModelId1  | ["My first DataClass", "My First Test DataModel"]
        "first"     | []                            | dataModelId2  | ["My first DataClass"]

    }
}
