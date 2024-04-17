package uk.ac.ox.softeng.mauro.plugin.importer

import uk.ac.ox.softeng.mauro.plugin.importer.config.ImportGroupConfig
import uk.ac.ox.softeng.mauro.plugin.importer.config.ImportParameterConfig

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
