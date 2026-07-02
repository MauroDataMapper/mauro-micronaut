package org.maurodata.plugin.importer.json

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.plugin.importer.FileImportParameters
import org.maurodata.plugin.importer.TerminologyImporterPlugin

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Singleton

@CompileStatic
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
        long start = System.nanoTime()
        if (!params.importFile) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, 'Import file is required')
        }
        ExportModel importModel = params.importFile.inputStream.withCloseable {InputStream inputStream ->
            objectMapper.readValue(inputStream, ExportModel)
        }
        log.info '*** imported JSON model in {} ms ***', (System.nanoTime() - start).intdiv(1000000L)
        if (!importModel.terminology){
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, 'Cannot import JSON as terminology/ies is not present')
        }
        if(importModel.terminology && !importModel.terminologies) {
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

    @Override
    Class<Terminology> getHandlesModelType() {
        Terminology
    }
}
