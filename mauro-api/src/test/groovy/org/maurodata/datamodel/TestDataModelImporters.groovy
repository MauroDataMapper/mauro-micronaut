package org.maurodata.datamodel

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.plugin.importer.DataModelImporterPlugin
import org.maurodata.plugin.importer.FileImportParameters
import org.maurodata.plugin.importer.ImportParameters

@CompileStatic
@Singleton
class ZeroFileDataModelImporterPlugin implements DataModelImporterPlugin<ImportParameters> {

    static Integer importCount = 0

    String version = '1.0.0'
    String displayName = 'Zero File DataModel Importer'

    static void reset() {
        importCount = 0
    }

    @Override
    List<DataModel> importDomain(ImportParameters params) {
        importCount++
        [new DataModel(label: params.modelName ?: 'Zero file data model')]
    }

    @Override
    Boolean handlesContentType(String contentType) {
        false
    }

    @Override
    Class<ImportParameters> importParametersClass() {
        ImportParameters
    }
}

@CompileStatic
@Singleton
class MultiFileDataModelImporterPlugin implements DataModelImporterPlugin<FileImportParameters> {

    static List<String> importedFileNames = []

    String version = '1.0.0'
    String displayName = 'Multi File DataModel Importer'

    static void reset() {
        importedFileNames = []
    }

    @Override
    List<DataModel> importDomain(FileImportParameters params) {
        importedFileNames.add(params.importFile.fileName)
        [new DataModel(label: "Imported ${params.importFile.fileName}".toString())]
    }

    @Override
    Boolean handlesContentType(String contentType) {
        true
    }

    @Override
    Class<FileImportParameters> importParametersClass() {
        FileImportParameters
    }
}
