package org.maurodata.plugin.importer.json

import io.micronaut.http.HttpStatus
import org.maurodata.ErrorHandler
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.export.ExportModel
import org.maurodata.plugin.importer.CodeSetImporterPlugin
import org.maurodata.plugin.importer.FileImportParameters

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Slf4j
@Singleton
class JsonCodeSetImporterPlugin implements CodeSetImporterPlugin<FileImportParameters> {

    String version = '4.0.0'

    String displayName = 'JSON CodeSet Importer'

    Boolean canImportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    List<CodeSet> importDomain(FileImportParameters params) {
        log.info '** start importModel **'
        ExportModel importModel = objectMapper.readValue(params.importFile.fileContents, ExportModel)
        log.info '*** imported JSON model ***'
        if (!importModel.codeSet){
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, 'Cannot import JSON as codeSet/s not present')
        }
        if(importModel.codeSet) {
            return [importModel.codeSet]
        } else {
            return importModel.codeSets?:[]
        }

    }

    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == "application/mauro.${CodeSet.class.simpleName.toLowerCase()}+json"
    }

    @Override
    Class<FileImportParameters> importParametersClass() {
        return FileImportParameters
    }

}
