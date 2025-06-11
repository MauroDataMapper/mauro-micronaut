package org.maurodata.plugin.exporter.json

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.plugin.exporter.TerminologyExporterPlugin

@Slf4j
@Singleton
class JsonTerminologyExporterPlugin implements TerminologyExporterPlugin {

    String version = '4.0.0'

    String displayName = 'JSON Terminology Exporter'

    Boolean canExportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    byte[] exportModel(Terminology terminology) {
        ExportModel exportModel = new ExportModel(this)
        exportModel.terminology = terminology
        objectMapper.writeValueAsBytes(exportModel)
    }

    @Override
    String getFileExtension() {
        return ".json"
    }

    @Override
    String getContentType() {
        "application/mauro.${Terminology.class.simpleName.toLowerCase()}+json"
    }

    @Override
    String getFileName(Terminology model) {
        return model.label + ".json"
    }

    @Override
    byte[] exportModels(Collection<Terminology> terminologies) {
        ExportModel exportModel = new ExportModel(this)
        exportModel.terminologies = terminologies
        objectMapper.writeValueAsBytes(exportModel)

    }
}
