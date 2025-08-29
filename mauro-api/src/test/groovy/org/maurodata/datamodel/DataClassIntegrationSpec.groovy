package org.maurodata.datamodel

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.BaseIntegrationSpec

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
        Folder folder = (Folder) POST('/api/folders', [label: 'Test folder'], Folder)
        DataModel dataModel = (DataModel) POST("/api/folders/$folder.id/dataModels", [label: 'Test data model'], DataModel)
        dataModelId = dataModel.id

        when:

        DataClass dataClass = (DataClass) POST("/api/dataModels/$dataModelId/dataClasses", [label: 'My first Data Class'], DataClass)

        then:
        dataClass.label == 'My first Data Class'
        dataClass.path.toString() == 'fo:Test folder|dm:Test data model$main|dc:My first Data Class'
    }

    void 'test extend data class'() {
        given:
            Folder folder = (Folder) POST('/api/folders', [label: 'Test folder'], Folder)
            DataModel dataModel = (DataModel) POST("/api/folders/$folder.id/dataModels", [label: 'Test data model'], DataModel)
            DataClass dataClass1 = (DataClass) POST("/api/dataModels/$dataModel.id/dataClasses", [label: 'My first Data Class'], DataClass)
            DataClass dataClass2 = (DataClass) POST("/api/dataModels/$dataModel.id/dataClasses", [label: 'My second Data Class'], DataClass)

        when:
            Map response = PUT("/api/dataModels/$dataModel.id/dataClasses/$dataClass2.id/extends/$dataModel.id/$dataClass1.id", null)

        then:
            response.label == 'My second Data Class'
            response.extendsDataClasses.size() == 1
            response.extendsDataClasses.first().label == 'My first Data Class'

        when:
            response = GET("/api/dataModels/$dataModel.id/dataClasses/$dataClass2.id")

        then:
            response.label == 'My second Data Class'
            response.extendsDataClasses.size() == 1
            response.extendsDataClasses.first().label == 'My first Data Class'

        when:
            response = DELETE("/api/dataModels/$dataModel.id/dataClasses/$dataClass2.id/extends/$dataModel.id/$dataClass1.id")

        then:
            response.label == 'My second Data Class'
            !response.extendsDataClasses

        when:
            response = GET("/api/dataModels/$dataModel.id/dataClasses/$dataClass2.id")

        then:
            response.label == 'My second Data Class'
            !response.extendsDataClasses

    }


}
