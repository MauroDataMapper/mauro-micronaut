package org.maurodata.plugin.exporter.json

import org.maurodata.domain.terminology.CodeSet
import org.maurodata.export.ExportModel
import org.maurodata.plugin.exporter.CodeSetExporterPlugin

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.visitor.common.SmallExportVisitor
import org.maurodata.visitor.common.TreeifyVisitor

@CompileStatic
@Slf4j
@Singleton
class JsonCodeSetExporterPlugin implements CodeSetExporterPlugin {

    String version = '4.0.0'

    String displayName = 'JSON CodeSet Exporter'

    Boolean canExportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    byte[] exportModel(CodeSet codeSet) {
        ExportModel exportModel = new ExportModel(this)
        exportModel.codeSet = codeSet
        objectMapper.writeValueAsBytes(exportModel)
    }

    @Override
    String getFileExtension() {
        return ".json"
    }

    @Override
    String getFileName(CodeSet model) {
        return model.label + ".json"
    }

    @Override
    byte[] exportModels(Collection<CodeSet> codeSets) {
        ExportModel exportModel = new ExportModel(this)
        if(codeSets.size() > 1) {
            exportModel.codeSets = codeSets.toList()
        } else {
            exportModel.codeSet = codeSets[0]
        }
        def visitor = new TreeifyVisitor() + new SmallExportVisitor()

        codeSets.each {codeSet ->
            codeSet.accept(visitor)
        }

        objectMapper.writeValueAsBytes(exportModel)
    }

    @Override
    String getContentType() {
        "application/mauro.${CodeSet.class.simpleName.toLowerCase()}+json"
    }

}
