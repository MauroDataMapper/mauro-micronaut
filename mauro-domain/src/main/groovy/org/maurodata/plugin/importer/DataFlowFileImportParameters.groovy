package org.maurodata.plugin.importer

import org.maurodata.plugin.importer.config.ImportGroupConfig
import org.maurodata.plugin.importer.config.ImportParameterConfig

class DataFlowFileImportParameters extends FileImportParameters {

    @ImportParameterConfig(
        displayName = 'Source dataModel',
        description = 'The source data model of the imported dataflow',
        order = 4,
        group = @ImportGroupConfig(
            name = 'Source Data Model',
            order = 5
        ))
    UUID sourceDataModelId
}
