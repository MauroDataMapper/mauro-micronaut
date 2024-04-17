package uk.ac.ox.softeng.mauro.plugin.importer

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.export.ExportModel

@Slf4j

class JsonDataModelImporterPlugin implements DataModelImporterPlugin<FileImportParameters> {

    String version = '4.0.0'

    String displayName = 'JSON DataModel Importer'

    Boolean canImportMultipleDomains = true

    static ObjectMapper objectMapper = new ObjectMapper()

    static {
        objectMapper.findAndRegisterModules()
    }

    @Override
    DataModel importDomain(FileImportParameters params) {
        log.info '** start importModel **'
        ExportModel importModel = objectMapper.readValue(params.importFile.fileContents, ExportModel)
        log.info '*** imported JSON model ***'

        DataModel imported = importModel.dataModel
        imported.setAssociations()
        imported.updateCreationProperties()
        log.info '* start updateCreationProperties *'
        imported.getAllContents().each {it.updateCreationProperties()}
        log.info '* finish updateCreationProperties *'


        return imported
    }

    @Override
    List<DataModel> importDomains(FileImportParameters params) {
        return null
    }

    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == 'application/mauro.datamodel+json'
    }

    @Override
    Class<FileImportParameters> importParametersClass() {
        return FileImportParameters
    }

    @Override
    Boolean canImportMultipleDomains() {
        return true
    }
}
