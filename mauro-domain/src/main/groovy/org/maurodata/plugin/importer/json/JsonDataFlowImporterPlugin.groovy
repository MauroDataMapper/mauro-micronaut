package org.maurodata.plugin.importer.json

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.ErrorHandler
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.export.ExportModel
import org.maurodata.plugin.importer.DataFlowFileImportParameters
import org.maurodata.plugin.importer.DataFlowImporterPlugin

@CompileStatic
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
        long start = System.nanoTime()
        if (!params.importFile) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, 'Import file is required')
        }
        ExportModel importModel = params.importFile.inputStream.withCloseable {InputStream inputStream ->
            objectMapper.readValue(inputStream, ExportModel)
        }
        log.info '*** imported JSON model in {} ms ***', (System.nanoTime() - start).intdiv(1000000L)

        if (importModel.dataFlow) {
            return [importModel.dataFlow]
        } else {
            return importModel.dataFlows ?: []
        }

    }

    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == 'application/mauro.dataflow+json'
    }

    @Override
    Class<DataFlowFileImportParameters> importParametersClass() {
        return DataFlowFileImportParameters
    }

}
