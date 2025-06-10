package uk.ac.ox.softeng.mauro.plugin.datatype

import jakarta.inject.Singleton

@Singleton
class DataTypeService implements DataTypePlugin{

    String name = "DataTypeService"

    String description = "Basic Default DataTypes"

    String version = "1.0.0"

    @Override
    List<DefaultDataType> getDataTypes() {
        [
                new DefaultDataType(label: 'Text', description: 'A piece of text'),
                new DefaultDataType(label: 'Number', description: 'A whole number'),
                new DefaultDataType(label: 'Decimal', description: 'A decimal number'),
                new DefaultDataType(label: 'Date', description: 'A date'),
                new DefaultDataType(label: 'DateTime', description: 'A date with a timestamp'),
                new DefaultDataType(label: 'Timestamp', description: 'A timestamp'),
                new DefaultDataType(label: 'Boolean', description: 'A true or false value'),
                new DefaultDataType(label: 'Duration', description: 'A time period in arbitrary units')
        ]
    }
}