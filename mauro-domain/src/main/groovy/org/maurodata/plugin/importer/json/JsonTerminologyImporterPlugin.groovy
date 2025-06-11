package org.maurodata.plugin.importer.json

import jakarta.inject.Inject
import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.plugin.importer.FileImportParameters
import org.maurodata.plugin.importer.TerminologyImporterPlugin

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Singleton

@Slf4j
@Singleton
class JsonTerminologyImporterPlugin implements TerminologyImporterPlugin<FileImportParameters> {

    String version = '4.0.0'

    String displayName = 'JSON Terminology Importer'

    @Inject
    ObjectMapper objectMapper

    @Override
    List<Terminology> importDomain(FileImportParameters params) {
        log.info '** start importModel **'
        ExportModel importModel = objectMapper.readValue(params.importFile.fileContents, ExportModel)
        log.info '*** imported JSON model ***'

        if(importModel.terminology) {
            return [importModel.terminology]
        } else {
            return importModel.terminologies?:[]
        }
    }



    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == 'application/mauro.terminology+json'
    }

    @Override
    Class<FileImportParameters> importParametersClass() {
        return FileImportParameters
    }

}
