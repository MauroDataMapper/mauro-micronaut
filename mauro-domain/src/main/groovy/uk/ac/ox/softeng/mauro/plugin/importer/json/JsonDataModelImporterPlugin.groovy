package uk.ac.ox.softeng.mauro.plugin.importer.json

import uk.ac.ox.softeng.mauro.plugin.importer.DataModelImporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.FileImportParameters

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.export.ExportModel

import jakarta.inject.Singleton

@Slf4j
@Singleton
class JsonDataModelImporterPlugin implements DataModelImporterPlugin<FileImportParameters> {

    String version = '4.0.0'

    String displayName = 'JSON DataModel Importer'

    Boolean canImportMultipleDomains = true

    static ObjectMapper objectMapper = new ObjectMapper()

    static {
        objectMapper.findAndRegisterModules()
    }

    @Override
    List<DataModel> importDomain(FileImportParameters params) {
        log.info '** start importModel **'
        ExportModel importModel = objectMapper.readValue(params.importFile.fileContents, ExportModel)
        log.info '*** imported JSON model ***'

        if(importModel.dataModel) {
            return [importModel.dataModel]
        } else {
            return importModel.dataModels?:[]
        }

    }

    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == 'application/mauro.datamodel+json'
    }

    @Override
    Class<FileImportParameters> importParametersClass() {
        return FileImportParameters
    }

}
