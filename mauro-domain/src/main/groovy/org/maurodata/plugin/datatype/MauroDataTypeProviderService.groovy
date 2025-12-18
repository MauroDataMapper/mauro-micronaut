package org.maurodata.plugin.datatype

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataType

@CompileStatic
@Singleton
class MauroDataTypeProviderService implements DefaultDataTypeProviderPlugin {

    String name = "MauroDataTypeProviderService"

    String displayName = "Basic Default DataTypes"

    String description = "Basic Default DataTypes"

    String version = "1.0.0"

    @Override
    List<DataType> getDataTypes() {
        [
            'Text': 'A piece of text',
            'Number': 'A whole number',
            'Decimal': 'A decimal number',
            'Date': 'A date',
            'DateTime': 'A date with a timestamp',
            'Timestamp': 'A timestamp',
            'Boolean': 'A true or false value',
            'Duration': 'A time period in arbitrary units'
        ].collect { name, desc ->
            DataType.build {
                label = name
                description = desc
                dataTypeKind = DataType.DataTypeKind.PRIMITIVE_TYPE
            }
        }
    }
}