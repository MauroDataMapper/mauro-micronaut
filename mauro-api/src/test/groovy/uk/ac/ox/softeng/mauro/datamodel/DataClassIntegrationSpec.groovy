package uk.ac.ox.softeng.mauro.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.annotation.Sql
import jakarta.inject.Inject
import spock.lang.Shared

@ContainerizedTest
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataClassIntegrationSpec extends CommonDataSpec {

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

    void setup(){
        Folder folder = folderApi.create(folder())
        DataModel dataModel = dataModelApi.create(folder.id, dataModelPayload('Test data model'))
        dataModelId = dataModel.id

    }
    void 'test data class'() {
        when:
        DataClass dataClass = dataClassApi.create(dataModelId, dataClassPayload('My first Data Class'))

        then:
        dataClass.label == 'My first Data Class'
        dataClass.path.toString() == 'fo:Test folder|dm:Test data model$main|dc:My first Data Class'
    }

    void 'test extend data class'() {
        given:
        DataClass dataClass1 = dataClassApi.create(dataModelId, dataClassPayload('My first Data Class'))
        DataClass dataClass2 = dataClassApi.create(dataModelId, dataClassPayload('My second Data Class'))

        when:
        DataClass response = dataClassApi.createExtension(dataModelId, dataClass2.id, dataModelId, dataClass1.id)

        then:
        response.label == 'My second Data Class'
        response.extendsDataClasses.size() == 1
        response.extendsDataClasses.first().label == 'My first Data Class'

        when:
        response = dataClassApi.show(dataModelId, dataClass2.id)

        then:
        response.label == 'My second Data Class'
        response.extendsDataClasses.size() == 1
        response.extendsDataClasses.first().label == 'My first Data Class'

        when:
        response = dataClassApi.deleteExtension(dataModelId, dataClass2.id, dataModelId, dataClass1.id)

        then:
        response.label == 'My second Data Class'
        !response.extendsDataClasses

        when:
        response = dataClassApi.show(dataModelId, dataClass2.id)


        then:
        response.label == 'My second Data Class'
        !response.extendsDataClasses

    }

    void 'test delete dataclass with children -should delete all associations'(){
        given:
        DataClass dataClass = dataClassApi.create(dataModelId, dataClassPayload('My first Data Class'))
        DataClass child = dataClassApi.create(dataModelId, dataClass.id, dataClassPayload('child  Data Class'))
        DataType reference = dataTypeApi.create(dataModelId, referenceTypeDataTypePayload(dataClass.id, 'dataclass reference label'))
        DataType referenceChild = dataTypeApi.create(dataModelId, referenceTypeDataTypePayload(child.id, 'child reference label'))
        DataElement dataElement = dataElementApi.create(dataModelId, dataClass.id, dataElementPayload('data type label', reference))

        when:
        HttpResponse httpResponse = dataClassApi.delete(dataModelId, dataClass.id, dataClass)
        then:
        httpResponse.status == HttpStatus.NO_CONTENT

        when:
        DataClass retrieved = dataClassApi.show(dataModelId, dataClass.id)
        then:
        !retrieved
        when:
        DataClass retrievedChild = dataClassApi.show(dataModelId, child.id)
        then:
        !retrievedChild

        when:
        DataElement retrievedDataElement = dataElementApi.show(dataModelId, dataClass.id, dataElement.id)
        then:
        !retrievedDataElement


        when:
        DataType referenceType = dataTypeApi.show(dataModelId, reference.id)
        then:
        !referenceType
        when:
        DataType referenceTypeChild = dataTypeApi.show(dataModelId, referenceChild.id)
        then:
        !referenceTypeChild
    }

}
