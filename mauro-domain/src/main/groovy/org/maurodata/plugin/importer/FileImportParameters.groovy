package org.maurodata.plugin.importer

import org.maurodata.plugin.importer.config.ImportGroupConfig
import org.maurodata.plugin.importer.config.ImportParameterConfig

import groovy.transform.CompileStatic

@CompileStatic
class FileImportParameters extends ImportParameters{

    @ImportParameterConfig(
            displayName = 'File',
            description = 'The file containing the data to be imported',
            order = -1,
            group = @ImportGroupConfig(
                    name = 'Source',
                    order = -1
            )
    )
    FileParameter importFile

}
