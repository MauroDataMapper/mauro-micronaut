package uk.ac.ox.softeng.mauro.plugin.datatype

import jakarta.inject.Singleton

@Singleton
class ProfileSpecificationDataTypeProvider implements DataTypePlugin{

    String name = "ProfileSpecificationDataTypeProvider"

    String description = "Profile Specification DataTypes"

    String version = "1.0.0"

    @Override
    List<DefaultDataType> getDataTypes() {
        [
                [label: 'boolean', description: 'logical Boolean [true/false)'],
                [label: 'string', description: 'short variable-length character string (plain-text)'],
                [label: 'text', description: 'long variable-length character string (may include html / markdown)'],
                [label: 'int', description: 'integer'],
                [label: 'decimal', description: 'decimal'],
                [label: 'date', description: 'calendar date [year, month, day)'],
                [label: 'datetime', description: 'date and time, excluding time zone'],
                [label: 'time', description: 'time of day [no time zone)'],
                [label: 'folder', description: 'pointer to a folder in this Mauro instance'],
                [label: 'model', description: 'pointer to a model in this Mauro instance'],
                [label: 'json', description: 'a text field containing valid json syntax']
        ].collect {Map<String, String> properties -> new DefaultDataType(properties)}
    }
}