package org.maurodata.plugin.importer.json

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.export.ExportModel
import org.maurodata.plugin.importer.DataFlowFileImportParameters
import org.maurodata.plugin.importer.DataFlowImporterPlugin
import org.maurodata.plugin.importer.FileImportParameters

@Slf4j
@Singleton
class JsonDataFlowImporterPlugin implements DataFlowImporterPlugin<DataFlowFileImportParameters> {

    String version = '4.0.0'

    String displayName = 'JSON DataFlow Importer'

    Boolean canImportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    List<DataFlow> importDomain(DataFlowFileImportParameters params) {
        log.info '** start importModel **'
        ExportModel importModel = objectMapper.readValue(params.importFile.fileContents, ExportModel)
        log.info '*** imported JSON model ***'

        if(importModel.dataFlow) {
            return [importModel.dataFlow]
        } else {
            return importModel.dataFlows?:[]
        }

    }

    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == 'application/mauro.dataflow+json'
    }

    @Override
    Class<FileImportParameters> importParametersClass() {
        return DataFlowFileImportParameters
    }

}
