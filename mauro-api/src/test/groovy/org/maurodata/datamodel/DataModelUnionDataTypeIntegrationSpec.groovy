package org.maurodata.datamodel

import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.api.datamodel.DataModelUnionRequestDTO
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec
import spock.lang.Shared

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down.sql",
        "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class DataModelUnionDataTypeIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId

    void setup() {
        folderId = folderApi.create(new Folder(label: 'Union DataType test folder')).id
    }

    void 'union uses owned primitive stubs labelled from contributing source DataTypes'() {
        given:
        createSourceModel('Zeta source', 'Zeta Type')
        createSourceModel('Alpha source', 'Alpha Type')

        when:
        DataModel union = dataModelApi.createUnion(new DataModelUnionRequestDTO(
            rootDomainType: 'Folder', rootId: folderId, folderId: folderId, label: 'Union'))

        then:
        List<DataClass> classes = dataClassApi.list(union.id).items
        classes.size() == 1
        List<DataElement> elements = dataElementApi.list(union.id, classes[0].id).items
        elements*.label.toSet() == ['Shared type', 'Combined type'] as Set

        DataElement shared = elements.find { it.label == 'Shared type' }
        DataElement combined = elements.find { it.label == 'Combined type' }
        shared.dataType.label == 'Common Type'
        combined.dataType.label == 'Alpha Type / Zeta Type'
        shared.dataType.dataTypeKind == DataType.DataTypeKind.PRIMITIVE_TYPE
        combined.dataType.dataTypeKind == DataType.DataTypeKind.PRIMITIVE_TYPE

        List<DataType> unionTypes = dataTypeApi.list(union.id).items
        unionTypes*.label.toSet() == ['Common Type', 'Alpha Type / Zeta Type'] as Set
        unionTypes.every { DataType type ->
            type.dataTypeKind == DataType.DataTypeKind.PRIMITIVE_TYPE &&
                type.description == null && type.units == null && type.referenceClass == null
        }
        shared.dataType.id == unionTypes.find { it.label == 'Common Type' }.id
        combined.dataType.id == unionTypes.find { it.label == 'Alpha Type / Zeta Type' }.id
    }

    private void createSourceModel(String modelLabel, String differingTypeLabel) {
        DataModel source = dataModelApi.create(folderId, new DataModel(label: modelLabel))
        DataClass dataClass = dataClassApi.create(source.id, new DataClass(label: 'Record'))
        DataType commonType = dataTypeApi.create(source.id, new DataType(label: 'Common Type',
            description: 'Source detail not copied to the union', units: 'source units',
            dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))
        DataType differingType = dataTypeApi.create(source.id, new DataType(label: differingTypeLabel,
            description: 'Source detail not copied to the union', units: 'source units',
            dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE))

        dataElementApi.create(source.id, dataClass.id,
            new DataElement(label: 'Shared type', dataType: new DataType(id: commonType.id)))
        dataElementApi.create(source.id, dataClass.id,
            new DataElement(label: 'Combined type', dataType: new DataType(id: differingType.id)))
    }
}
