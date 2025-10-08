package org.maurodata.plugin.exporter.json

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.export.ExportModel
import org.maurodata.plugin.exporter.DataFlowExporterPlugin

@Slf4j
@Singleton
class  JsonDataFlowExporterPlugin implements DataFlowExporterPlugin {

    String version = '4.0.0'

    String displayName = 'JSON DataFlow Exporter'

    Boolean canExportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    byte[] exportModelItem(DataFlow dataFlow) {
        ExportModel exportModel = new ExportModel(this)
        exportModel.dataFlow = dataFlow
        objectMapper.writeValueAsBytes(exportModel)

    }

    @Override
    String getFileExtension() {
        return ".json"
    }

    @Override
    String getFileName(DataFlow modelItem) {
        return modelItem.label + ".json"
    }

    @Override
    byte[] exportModelItems(Collection<DataFlow> dataFlows) {
        ExportModel exportModel = new ExportModel(this)
        exportModel.dataFlows = dataFlows
        objectMapper.writeValueAsBytes(exportModel)
    }

    @Override
    String getContentType() {
        "application/mauro.${DataFlow.class.simpleName.toLowerCase()}+json"
    }
}
