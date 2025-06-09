package uk.ac.ox.softeng.mauro.plugin.datatype

import jakarta.inject.Singleton

@Singleton
class OpenClinicaV3CrfDefaultDataTypeProviderService implements DataTypePlugin{

    String name = "OpenClinicaV3CrfDefaultDataTypeProviderService"

    String description = "OpenClinica 3.x CRF DataTypes"

    String version = "1.0.0-SNAPSHOT"

    @Override
    List<DefaultDataType> getDataTypes() {
        [
                [
                        label      : 'ST',
                        description: 'String.  Any characters can be provided for this data type.'
                ],
                [
                        label      : 'INT',
                        description: 'Integer.  Only numbers with no decimal places are allowed for this data type.'
                ],
                [
                        label      : 'REAL',
                        description: 'Numbers with decimal places are allowed for this data type.'
                ],
                [
                        label      : 'DATE',
                        description: 'Only full dates are allowed for this data type.  The default date format the user must provide the value in is DD-MMM-YYYY.'
                ],
                [
                        label      : 'PDATE',
                        description: 'Partial dates are allowed for this data type.  The default date format is DD-MMM-YYYY so users can provide either MMM-YYYY or YYYY values.'
                ],
                [
                        label      : 'FILE',
                        description: 'This data type allows files to be attached to the item.  It must be used in conjunction with a RESPONSE_TYPE of file.  The attached file is ' +
                                'saved to the server and a URL is displayed to the user viewing the form.'
                ]
        ].collect {Map<String, String> properties -> new DefaultDataType(properties)}
    }
}