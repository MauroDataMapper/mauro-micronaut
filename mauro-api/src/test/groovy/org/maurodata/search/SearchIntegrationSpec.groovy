package org.maurodata.search

import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.testing.CommonDataSpec

import jakarta.inject.Singleton
import spock.lang.Shared
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest

import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.web.ListResponse

@ContainerizedTest
@Singleton
class SearchIntegrationSpec extends CommonDataSpec {

    @Shared
    Folder folder

    @Shared
    UUID dataModelId1

    @Shared
    UUID dataModelId2

    @Shared
    UUID dataModelId3

    void setupSpec() {
        folder = folderApi.create(new Folder(label: 'Test Folder'))

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

    void "Test Get Search"() {

        searchApi.rebuildIndexes()

        expect:

        SearchRequestDTO searchRequestDTO = new SearchRequestDTO(
            searchTerm: searchTerm,
            domainTypes: domainTypes,
            withinModelId: withinModelId)

        ListResponse<SearchResultsDTO> searchResults = searchApi.searchGet(searchRequestDTO)
        searchResults.items.label == expectedLabels

        where:

        searchTerm  | domainTypes                   | withinModelId | expectedLabels
        "first"     | []                            | null          | ["My First Test DataModel", "My first DataClass", "My first DataClass"]
        "first"     | ["DataModel"]                 | null          | ["My First Test DataModel"]
        "first"     | ["DataClass"]                 | null          | ["My first DataClass", "My first DataClass"]
        "first"     | ["DataClass", "DataModel"]    | null          | ["My First Test DataModel", "My first DataClass", "My first DataClass"]
        "first"     | ["DataType"]                  | null          | []
        "first"     | []                            | dataModelId1  | ["My First Test DataModel", "My first DataClass"]
        "first"     | []                            | dataModelId2  | ["My first DataClass"]

    }

    void "Test Get Search after auto rebuild"() {

        when:

        DataModel dataModel3 = DataModel.build {
            label "My Third Test DataModel"
            description "Description of my third model"
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
        dataModelId3 = importDataModel(dataModel3, folder)


        then: // Search results are unchanged until after rebuild

        testCases.each { testCase ->
            SearchRequestDTO searchRequestDTO = new SearchRequestDTO(
                searchTerm: testCase.searchTerm,
                domainTypes: testCase.domainTypes as List<String>,
                withinModelId: testCase.withinModelId == "dataModelId3"?dataModelId3:(testCase.withinModelId as UUID))

            ListResponse<SearchResultsDTO> searchResults = searchApi.searchGet(searchRequestDTO)

            assert searchResults.items.label == testCase.expectedLabels
        }


        when:

        Thread.sleep(5*1000)

        then: // Search results are unchanged until after rebuild

        testCases.each { testCase ->
            SearchRequestDTO searchRequestDTO = new SearchRequestDTO(
                searchTerm: testCase.searchTerm,
                domainTypes: testCase.domainTypes as List<String>,
                withinModelId: testCase.withinModelId == "dataModelId3"?dataModelId3:(testCase.withinModelId as UUID))

            ListResponse<SearchResultsDTO> searchResults = searchApi.searchGet(searchRequestDTO)

            assert searchResults.items.label == testCase.expectedLabelsAfterAddition
        }

    }

    List<Map> testCases = [
        [searchTerm: "first",
         domainTypes: [],
         withinModelId: null,
         expectedLabels: ["My First Test DataModel", "My first DataClass", "My first DataClass"],
         expectedLabelsAfterAddition: ["My First Test DataModel", "My first DataClass", "My first DataClass", "My first DataClass"],
        ],
        [searchTerm: "first",
         domainTypes: ["DataModel"],
         withinModelId: null,
         expectedLabels: ["My First Test DataModel"],
         expectedLabelsAfterAddition: ["My First Test DataModel"]],
        [searchTerm: "first",
         domainTypes: ["DataClass"],
         withinModelId: null,
         expectedLabels: ["My first DataClass", "My first DataClass"],
         expectedLabelsAfterAddition: ["My first DataClass", "My first DataClass", "My first DataClass"]],
        [searchTerm: "first",
         domainTypes: ["DataClass","DataModel"],
         withinModelId: null,
         expectedLabels: ["My First Test DataModel", "My first DataClass", "My first DataClass"],
         expectedLabelsAfterAddition: ["My First Test DataModel", "My first DataClass", "My first DataClass", "My first DataClass"]],
        [searchTerm: "first",
         domainTypes: ["DataType"],
         withinModelId: null,
         expectedLabels: [],
         expectedLabelsAfterAddition: []],
        [searchTerm: "first",
         domainTypes: [],
         withinModelId: dataModelId1,
         expectedLabels: ["My First Test DataModel", "My first DataClass"],
         expectedLabelsAfterAddition: ["My First Test DataModel", "My first DataClass"]],
        [searchTerm: "first",
         domainTypes: [],
         withinModelId: dataModelId2,
         expectedLabels: ["My first DataClass"],
         expectedLabelsAfterAddition: ["My first DataClass"]],
        [searchTerm: "first",
         domainTypes: [],
         withinModelId: "dataModelId3",
         expectedLabels: [],
         expectedLabelsAfterAddition: ["My first DataClass"]]
    ]


}
