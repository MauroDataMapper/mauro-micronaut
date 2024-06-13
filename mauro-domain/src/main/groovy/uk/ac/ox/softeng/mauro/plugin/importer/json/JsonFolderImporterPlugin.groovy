package uk.ac.ox.softeng.mauro.plugin.importer.json

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.JsonPluginConstants
import uk.ac.ox.softeng.mauro.plugin.importer.FileImportParameters
import uk.ac.ox.softeng.mauro.plugin.importer.FolderImporterPlugin

@Slf4j
@Singleton
class JsonFolderImporterPlugin implements FolderImporterPlugin<FileImportParameters> {

    String version = JsonPluginConstants.VERSION

    String displayName = 'JSON Folder Importer'

    Boolean canImportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    List<Folder> importDomain(FileImportParameters params) {
        log.info '** start importModel **'
        ExportModel importModel = objectMapper.readValue(params.importFile.fileContents, ExportModel)
        log.info '*** imported JSON model ***'

        if(importModel.folder) {
            return [importModel.folder]
        } else {
            return importModel.folders?:[]
        }
    }

    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == 'application/mauro.folder+json'
    }

    @Override
    Class<FileImportParameters> importParametersClass() {
        return FileImportParameters
    }

}