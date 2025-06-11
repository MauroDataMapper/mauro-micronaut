package org.maurodata.plugin.importer.json

import jakarta.inject.Inject
import org.maurodata.plugin.importer.DataModelImporterPlugin
import org.maurodata.plugin.importer.FileImportParameters

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.export.ExportModel

import jakarta.inject.Singleton

@Slf4j
@Singleton
class JsonDataModelImporterPlugin implements DataModelImporterPlugin<FileImportParameters> {

    String version = '4.0.0'

    String displayName = 'JSON DataModel Importer'

    Boolean canImportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

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
