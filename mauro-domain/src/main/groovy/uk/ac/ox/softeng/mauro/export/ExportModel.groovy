package uk.ac.ox.softeng.mauro.export

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class ExportModel {

    ExportMetadata exportMetadata

    Terminology terminology
    List<Terminology> terminologies

    DataModel dataModel
    List<DataModel> dataModels

    Folder folder
    List<Folder> folders = []
}
