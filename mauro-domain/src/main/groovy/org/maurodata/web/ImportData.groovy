package org.maurodata.web

import org.maurodata.export.ExportModel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class ImportData {

    ExportModel importFile
    UUID folderId
}
