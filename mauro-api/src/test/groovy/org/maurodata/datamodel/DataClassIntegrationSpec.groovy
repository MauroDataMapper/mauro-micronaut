package org.maurodata.datamodel

import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down-dataclass.sql", phase = Sql.Phase.AFTER_ALL)
class DataClassIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    @Shared
    UUID dataModelId

    @Shared
    UUID dataClassId1

    @Shared
    UUID dataClassId2

    @Shared
    UUID dataClassId3

    void 'test data class'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'Test folder'))
        folderId = folder.id
        DataModel dataModel = dataModelApi.create(folder.id,  new DataModel(label: 'Test data model'))
        dataModelId = dataModel.id

        when:

        DataClass dataClass = dataClassApi.create(dataModelId, new DataClass(label: 'My first Data Class'))
        dataClassId1 = dataClass.id

        then:
        dataClass.label == 'My first Data Class'
        dataClass.path.toString() == 'fo:Test folder|dm:Test data model$main|dc:My first Data Class'
    }

    void 'test extend data class'() {
        given:
            DataClass dataClass2 = dataClassApi.create(dataModelId, new DataClass(label: 'My second Data Class'))
            dataClassId2 = dataClass2.id
        when:
            DataClass response = dataClassApi.createExtension(dataModelId, dataClassId2, dataModelId, dataClassId1)

        then:
            response.label == 'My second Data Class'
            response.extendsDataClasses.size() == 1
            response.extendsDataClasses.first().label == 'My first Data Class'

        when:
            response = dataClassApi.show(dataModelId, dataClassId2)

        then:
            response.label == 'My second Data Class'
            response.extendsDataClasses.size() == 1
            response.extendsDataClasses.first().label == 'My first Data Class'

        when:
            response = dataClassApi.deleteExtension(dataModelId, dataClassId2, dataModelId, dataClassId1)

        then:
            response.label == 'My second Data Class'
            !response.extendsDataClasses

        when:
            response = dataClassApi.show(dataModelId, dataClassId2)

        then:
            response.label == 'My second Data Class'
            !response.extendsDataClasses

    }

    void 'test move data class'() {
        when:
        DataClass dataClass = dataClassApi.create(dataModelId, new DataClass(label: 'My new Data Class'))
        dataClassId3 = dataClass.id

        ListResponse<DataClass> dataClasses = dataClassApi.list(dataModelId)
        then:
        dataClasses.items.size() == 3

        when:
        dataClass = dataClassApi.moveDataClass(dataModelId, dataClassId3, new DataClass(parentDataClass: new DataClass(id: dataClassId1)))
        then:
        dataClass.parentDataClass.id == dataClassId1

        when:
        dataClasses = dataClassApi.list(dataModelId)
        then:
        dataClasses.items.size() == 2

        when:
        dataClasses = dataClassApi.list(dataModelId, dataClassId1)
        then:
        dataClasses.items.size() == 1

        // Now move it back again
        when:
        dataClass = dataClassApi.moveDataClass(dataModelId, dataClassId3, new DataClass())
        then:
        dataClass.parentDataClass == null

        when:
        dataClasses = dataClassApi.list(dataModelId)
        then:
        dataClasses.items.size() == 3

        when:
        dataClasses = dataClassApi.list(dataModelId, dataClassId1)
        then:
        dataClasses.items.size() == 0


    }

    void 'test move class into sub-class'() {

        when:
        DataClass childDataClass = dataClassApi.create(dataModelId, dataClassId1, new DataClass(label: "Child data class"))

        dataClassApi.moveDataClass(dataModelId, dataClassId1, new DataClass(parentDataClass: new DataClass(id: childDataClass.id)))
        then:
        HttpClientResponseException exception = thrown()
        exception.status == HttpStatus.UNPROCESSABLE_ENTITY


    }




}
