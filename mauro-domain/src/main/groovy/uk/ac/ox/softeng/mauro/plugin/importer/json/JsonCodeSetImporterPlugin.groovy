package uk.ac.ox.softeng.mauro.plugin.importer.json


import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.importer.CodeSetImporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.FileImportParameters

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
