package uk.ac.ox.softeng.mauro.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.testing.BaseIntegrationSpec
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.http.HttpStatus
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import spock.lang.Shared

@ContainerizedTest
class DataClassIntegrationSpec extends BaseIntegrationSpec {

    @Inject
    EmbeddedApplication<?> application

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID dataTypeId1

    @Shared
    UUID dataTypeId2

    @Shared
    UUID dataTypeId3

    @Shared
    UUID dataClassId1

    @Shared
    UUID dataClassId2

    @Shared
    UUID dataClassId3

    @Shared
    UUID dataElementId1

    @Shared
    UUID dataElementId2

    @Shared
    UUID enumerationValueId

    void 'test data class'() {
        given:
        Folder folder = (Folder) POST('/folders', [label: 'Test folder'], Folder)
        DataModel dataModel = (DataModel) POST("/folders/$folder.id/dataModels", [label: 'Test data model'], DataModel)
        dataModelId = dataModel.id

        when:

        DataClass dataClass = (DataClass) POST("/dataModels/$dataModelId/dataClasses", [label: 'My first Data Class'], DataClass)

        then:
        dataClass.label == 'My first Data Class'
        dataClass.path.toString() == 'dm:Test data model$main|dc:My first Data Class'
    }

    void 'test extend data class'() {
        given:
            Folder folder = (Folder) POST('/folders', [label: 'Test folder'], Folder)
            DataModel dataModel = (DataModel) POST("/folders/$folder.id/dataModels", [label: 'Test data model'], DataModel)
            DataClass dataClass1 = (DataClass) POST("/dataModels/$dataModel.id/dataClasses", [label: 'My first Data Class'], DataClass)
            DataClass dataClass2 = (DataClass) POST("/dataModels/$dataModel.id/dataClasses", [label: 'My second Data Class'], DataClass)

        when:
            Map response = PUT("/dataModels/$dataModel.id/dataClasses/$dataClass2.id/extends/$dataModel.id/$dataClass1.id", null)

        then:
            response.label == 'My second Data Class'
            response.extendsDataClasses.size() == 1
            response.extendsDataClasses.first().label == 'My first Data Class'

        when:
            response = GET("/dataModels/$dataModel.id/dataClasses/$dataClass2.id")

        then:
            response.label == 'My second Data Class'
            response.extendsDataClasses.size() == 1
            response.extendsDataClasses.first().label == 'My first Data Class'

        when:
            response = DELETE("/dataModels/$dataModel.id/dataClasses/$dataClass2.id/extends/$dataModel.id/$dataClass1.id")

        then:
            response.label == 'My second Data Class'
            !response.extendsDataClasses

        when:
            response = GET("/dataModels/$dataModel.id/dataClasses/$dataClass2.id")

        then:
            response.label == 'My second Data Class'
            !response.extendsDataClasses

    }


}
