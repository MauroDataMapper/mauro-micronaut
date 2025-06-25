package org.maurodata.plugin.datatype

import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataType

@Singleton
class ProfileSpecificationDataTypeProvider implements DefaultDataTypeProviderPlugin{

    String name = "ProfileSpecificationDataTypeProvider"

    String displayName = "Profile Specification DataTypes"

    String description = "Profile Specification DataTypes"

    String version = "1.0.0"

    @Override
    List<DataType> getDataTypes() {
        [
            'boolean' : 'logical Boolean [true/false)',
            'string'  : 'short variable-length character string (plain-text)',
            'text'    : 'long variable-length character string (may include html / markdown)',
            'int'     : 'integer',
            'decimal' : 'decimal',
            'date'    : 'calendar date [year, month, day)',
            'datetime': 'date and time, excluding time zone',
            'time'    : 'time of day [no time zone)',
            'folder'  : 'pointer to a folder in this Mauro instance',
            'model'   : 'pointer to a model in this Mauro instance',
            'json'    : 'a text field containing valid json syntax'
        ].collect {
            name, desc ->
                new DataType(
                    label: name, description: desc, dataTypeKind: DataType.DataTypeKind.PRIMITIVE_TYPE
                )
        }
    }
}