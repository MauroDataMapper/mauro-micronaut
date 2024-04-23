package uk.ac.ox.softeng.mauro.plugin.importer.json

import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.importer.FileImportParameters
import uk.ac.ox.softeng.mauro.plugin.importer.TerminologyImporterPlugin

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j

@Slf4j
class JsonTerminologyImporterPlugin implements TerminologyImporterPlugin<FileImportParameters> {

    String version = '4.0.0'

    String displayName = 'JSON Terminology Importer'

    static ObjectMapper objectMapper = new ObjectMapper()

    static {
        objectMapper.findAndRegisterModules()
    }

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
        return contentType == 'application/mauro.datamodel+json'
    }

    @Override
    Class<FileImportParameters> importParametersClass() {
        return FileImportParameters
    }

}
