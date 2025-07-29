package org.maurodata.web

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.model.AdministeredItem
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@MicronautTest
class ListResponseTest extends Specification {

    @Shared
    List<DataModel> dataModelsList
    @Shared
    List<DataType> dataTypesList

    void setupSpec() {
        DataModel dataModel1 = new DataModel().tap {
            id = UUID.randomUUID()
            label = 'data model first label'
            readableByEveryone = true
        }

        DataModel dataModel2 = new DataModel().tap {
            id = UUID.randomUUID()
            label = 'data model second model'
            readableByEveryone = true
        }
        DataModel dataModel3 = new DataModel().tap {
            id = UUID.randomUUID()
            label = 'data model third model'
            readableByEveryone = true
            domainType = 'EnumerationType'
        }

        dataModelsList = List.of(dataModel2, dataModel3, dataModel1)

        DataType dataType1 = new DataType().tap {
            id = UUID.randomUUID()
            label = 'datatype label first'
            domainType = 'EnumerationType'
            DataType.DataTypeKind.ENUMERATION_TYPE
        }
        DataType dataType2 = new DataType().tap {
            id = UUID.randomUUID()
            label = 'datatype label second'
            domainType = 'ModelType'
            dataTypeKind = DataType.DataTypeKind.MODEL_TYPE
        }
        DataType dataType3 = new DataType().tap {
            id = UUID.randomUUID()
            label = 'datatype label third'
            domainType = 'ReferenceType'
            DataType.DataTypeKind.REFERENCE_TYPE
        }
        dataTypesList = List.of(dataType2, dataType3, dataType1)
    }

    @Unroll
    void 'test listResponse #list with #params -should filter and sort returning #expectedNumber and first label should be #expectedLabel'() {
        when:
        ListResponse listResponse = ListResponse.from(list, params)


        then:
        listResponse.items.size() == expectedNumber
        AdministeredItem firstItem = listResponse.items.first()
        firstItem.label == expectedLabel

        where:
        list           | params                 | expectedNumber | expectedLabel
        dataModelsList | paramsWithSortAndMax() | 2              | 'data model first label'
        dataModelsList | paramsWithAllAndSort() | 3              | 'data model first label'
        dataTypesList  | paramsWithFilter()     | 1              | 'datatype label first'

    }

    protected PaginationParams paramsWithSortAndMax() {
        PaginationParams params = new PaginationParams()
        params.max = 2
        params.sort = 'label'
        params
    }

    protected PaginationParams paramsWithAllAndSort() {
        PaginationParams params = new PaginationParams()
        params.sort = 'label'
        params.all = 'true'
        params
    }

    protected PaginationParams paramsWithFilter() {
        PaginationParams params = new PaginationParams()
        params.domainType = 'EnumerationType'
        params
    }

}

