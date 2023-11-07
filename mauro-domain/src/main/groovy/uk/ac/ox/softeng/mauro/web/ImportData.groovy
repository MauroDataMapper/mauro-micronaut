package uk.ac.ox.softeng.mauro.web

import uk.ac.ox.softeng.mauro.export.ExportModel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class ImportData {

    ExportModel importFile
    UUID folderId
}
