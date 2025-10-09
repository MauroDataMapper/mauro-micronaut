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
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.model.version.FinaliseData
import org.maurodata.domain.model.version.VersionChangeType
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import org.maurodata.web.ListResponse
import spock.lang.Ignore
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
        pathApi.getResourceByPath('datamodel', 'dm:not known label')

        then:
        HttpStatusException exception = thrown()
        exception.status == HttpStatus.NOT_FOUND
    }

    void 'test getResource by path -unknown domainType  -shouldThrowException'() {
        when:
        pathApi.getResourceByPath('whatisthis', "dm:${EXPECTED_LABEL}" )

        then:
        HttpStatusException exception = thrown()
        exception.status ==  HttpStatus.NOT_FOUND
    }

    void 'test getResource by Path from Resource1 -should find resource'() {
        DataModel dataModel = dataModelApi.create(folderId, dataModelPayload('datamodel label '))
        DataType dataType = dataTypeApi.create(dataModel.id, dataTypesPayload('label for datatype', DataType.DataTypeKind.ENUMERATION_TYPE))
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
        DataElement dataElementResponse = pathApi.getResourceByPathFromResource(DataModel.class.simpleName, dataModel.id, dataElement.getPath().pathString) as DataElement
        then:
        dataElementResponse
        dataElementResponse.id == dataElement.id

        when:
        EnumerationValue enumerationValueResponse =
            pathApi.getResourceByPathFromResource(DataModel.class.simpleName, dataModel.id, noValue.getPath().pathString) as EnumerationValue
        then:
        enumerationValueResponse
        enumerationValueResponse.id == noValue.id

        DataModel finalised = dataModelApi.finalise(dataModel.id,
            new FinaliseData(versionChangeType: VersionChangeType.MAJOR, versionTag: 'random version tag'))


        DataModel newBranchModelVersion = dataModelApi.createNewBranchModelVersion(finalised.id, new CreateNewVersionData().tap {
            branchName: 'newBranchName'
        })

        ListResponse<DataClass> response = dataClassApi.list(newBranchModelVersion.id)
        DataModel retrieved  = dataModelApi.show(newBranchModelVersion.id)
        when:

        DataModel resourceByPath =
            pathApi.getResourceByPathFromResource(DataModel.class.simpleName, newBranchModelVersion.id, retrieved.path.pathString) as DataModel
        then:
        resourceByPath
        resourceByPath.id == newBranchModelVersion.id
    }


    //todo: fixme the version info is not part of the  pathsstring for modelitems so unable to match exact version
    void 'test getResource by Path from Resource2 -should find resource'() {
        DataModel dataModel = dataModelApi.create(folderId, dataModelPayload('datamodel label '))
        DataClass dataClass = dataClassApi.create(dataModel.id, dataClassPayload('label for dataclass'))

        DataModel finalised = dataModelApi.finalise(dataModel.id,
                                                    new FinaliseData(versionChangeType: VersionChangeType.MAJOR, versionTag: 'random version tag'))


        DataModel newBranchModelVersion = dataModelApi.createNewBranchModelVersion(finalised.id, new CreateNewVersionData().tap {
            branchName: 'newBranchName'
        })

        DataModel retrieved  = dataModelApi.show(newBranchModelVersion.id)
        DataClass retrievedDataClass = dataClassApi.list(retrieved.id).items.first()
        DataClass fullRetrievedDataClass = dataClassApi.show(retrieved.id, retrievedDataClass.id)

        String newModelVersionDataClassPathString = fullRetrievedDataClass.path.pathString
        when:
        DataClass resourceByPath =
            pathApi.getResourceByPathFromResource(DataClass.class.simpleName, fullRetrievedDataClass.id, newModelVersionDataClassPathString) as DataClass
        then:
        resourceByPath
        resourceByPath.label == fullRetrievedDataClass.label
    }


    void 'test getResource by Path from Resource - input path not from input model -should throw not found exception'(){
        DataModel dataModel = dataModelApi.create(folderId, dataModelPayload('datamodel label '))
        DataModel otherModel = dataModelApi.create(folderId, dataModelPayload('other label'))
        DataType dataType  = dataTypeApi.create(dataModel.id, dataTypesPayload('label for datatype', DataType.DataTypeKind.ENUMERATION_TYPE))
        DataClass dataClass = dataClassApi.create(dataModel.id, dataClassPayload('label for dataclass'))
        DataElement dataElement = dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('label for datatype', dataType))

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

