package org.maurodata.path

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import spock.lang.Shared
import spock.lang.Unroll

@ContainerizedTest
@Singleton
@Sql(scripts = "classpath:sql/tear-down.sql", phase = Sql.Phase.AFTER_EACH)
class PathControllerIntegrationSpec extends CommonDataSpec {

    static final String EXPECTED_LABEL = 'test label'
    static final String EXPECTED_PATH = 'fo:Test folder|cs:test label$main'
    @Shared
    UUID folderId

    @Shared
    UUID codeSetId


    def setup() {
        folderId = folderApi.create(folder()).id
        codeSetId = codeSetApi.create(folderId, codeSet(EXPECTED_LABEL)).id
    }

    @Unroll
    void 'test getResource by #path given #domainType -should get resource'() {
        when:
        CodeSet codeSet = pathApi.getResourceByPath(domainType, path) as AdministeredItem as CodeSet
        then:
        codeSet
        codeSet.label
        codeSet.path.pathString == EXPECTED_PATH

        where:
        domainType | path
        'codeSet'  | EXPECTED_PATH
        'codesets' | EXPECTED_PATH
    }


    void 'test getResource by path -path not found -shouldThrowException'() {
        when:
        pathApi.getResourceByPath('datamodel', 'not known label')

        then:
        HttpStatusException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'test getResource by path -unknown domainType  -shouldThrowException'() {
        when:
        pathApi.getResourceByPath('whatisthis', EXPECTED_LABEL)

        then:
        HttpStatusException exception = thrown()
        exception.status ==  HttpStatus.NOT_FOUND
    }

    void 'test getResource by Path from Resource -should find resource'(){
        DataModel dataModel = dataModelApi.create(folderId, dataModelPayload('datamodel label '))
        DataType dataType  = dataTypeApi.create(dataModel.id, dataTypesPayload('label for datatype', DataType.DataTypeKind.ENUMERATION_TYPE))
        DataClass dataClass = dataClassApi.create(dataModel.id, dataClassPayload('label for dataclass'))
        DataElement dataElement = dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('label for datatype', dataType))
        DataType enumerationType = dataTypeApi.create(dataModel.id, new DataType(label: 'test enumerationType',
                                                                                dataTypeKind: DataType.DataTypeKind.ENUMERATION_TYPE))
        enumerationValueApi.create(dataModel.id, enumerationType.id, new EnumerationValue().tap {
            key = 'yes'
            value = 'yes value'
        })
        EnumerationValue noValue = enumerationValueApi.create(dataModel.id, enumerationType.id, new EnumerationValue().tap {
            key = 'no'
            value = 'no value'
        })
        when:
        DataElement dataElementResponse  = pathApi.getResourceByPathFromResource( DataModel.class.simpleName, dataModel.id, dataElement.getPath().pathString) as DataElement
        then:
        dataElementResponse
        dataElementResponse.id == dataElement.id

        when:
        EnumerationValue enumerationValueResponse = pathApi.getResourceByPathFromResource(DataModel.class.simpleName, dataModel.id, noValue.getPath().pathString) as EnumerationValue
        then:
        enumerationValueResponse
        enumerationValueResponse.id == noValue.id


    }

    void 'test getResource by Path from Resource - input path not from input model -should throw not found exception'(){
        DataModel dataModel = dataModelApi.create(folderId, dataModelPayload('datamodel label '))
        DataModel otherModel = dataModelApi.create(folderId, dataModelPayload('other label'))
        DataType dataType  = dataTypeApi.create(dataModel.id, dataTypesPayload('label for datatype', DataType.DataTypeKind.ENUMERATION_TYPE))
        DataClass dataClass = dataClassApi.create(dataModel.id, dataClassPayload('label for dataclass'))
        DataElement dataElement = dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('label for datatype', dataType))
        String fullPath = dataElement.getPath().pathString

        when:
        pathApi.getResourceByPathFromResource( DataModel.class.simpleName, otherModel.id, dataElement.getPath().pathString) as DataElement

        then:
        HttpStatusException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'test getResource by Path from Resource - bad input path -should throw not found exception'(){
        DataModel dataModel = dataModelApi.create(folderId, dataModelPayload('datamodel label '))
        DataModel otherModel = dataModelApi.create(folderId, dataModelPayload('other label'))
        DataType dataType  = dataTypeApi.create(dataModel.id, dataTypesPayload('label for datatype', DataType.DataTypeKind.ENUMERATION_TYPE))
        DataClass dataClass = dataClassApi.create(dataModel.id, dataClassPayload('label for dataclass'))
        DataElement dataElement = dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('label for datatype', dataType))
        String fullPath = dataElement.getPath().pathString

        when:
        pathApi.getResourceByPathFromResource( DataModel.class.simpleName, otherModel.id, fullPath + 'something' ) as DataElement

        then:
        HttpStatusException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND


    }
}

