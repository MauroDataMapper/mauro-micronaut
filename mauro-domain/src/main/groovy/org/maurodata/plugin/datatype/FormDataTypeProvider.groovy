package org.maurodata.plugin.datatype

import jakarta.inject.Singleton

@Singleton
class FormDataTypeProvider implements DataTypePlugin{

    String name = "FormDataTypeProvider"

    String description = "Basic Form DataTypes"

    String version = "1.0.0"

    @Override
    List<DefaultDataType> getDataTypes() {
        [
                [
                        label      : 'URI',
                        description: 'A URI'
                ],
                [
                        label      : 'File',
                        description: 'A file attachment'
                ]
        ].collect {Map<String, String> properties -> new DefaultDataType(properties)}
    }
}