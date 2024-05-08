package uk.ac.ox.softeng.mauro.export

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin

import java.time.Instant

@CompileStatic
@Introspected
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class ExportModel {

    ExportMetadata exportMetadata

    Terminology terminology
    List<Terminology> terminologies = []

    DataModel dataModel
    List<DataModel> dataModels = []

    Folder folder
    List<Folder> folders = []

    ExportModel() {

    }

    ExportModel(ModelExporterPlugin plugin) {
        exportMetadata = new ExportMetadata(
                namespace: plugin.namespace,
                name: plugin.name,
                version: plugin.version,
                exportDate: Instant.now(),
                exportedBy: "Anonymous User"
        )
    }

    /****
     * Methods for building a tree-like DSL
     */

    static ExportModel build(
        Map args,
        @DelegatesTo(value = ExportModel, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new ExportModel(args).tap(closure)
    }

    static ExportModel build(
        @DelegatesTo(value = ExportModel, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    ExportMetadata exportMetadata(ExportMetadata exportMetadata) {
        this.exportMetadata = exportMetadata
        exportMetadata
    }


    ExportMetadata exportMetadata(Map args, @DelegatesTo(value = ExportMetadata, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        ExportMetadata exportMetadata1 = ExportMetadata.build(args, closure)
        exportMetadata exportMetadata1
    }

    ExportMetadata exportMetadata(@DelegatesTo(value = ExportMetadata, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        exportMetadata [:], closure
    }


    Terminology terminology(Terminology terminology) {
        if (!this.terminology && terminologies.size() == 0) {
            this.terminology = terminology
        } else {
            if(this.terminology) {
                this.terminologies.add(this.terminology)
                this.terminology = null
            }
            this.terminologies.add(terminology)
        }
        terminology
    }

    Terminology terminology(Map args, @DelegatesTo(value = Terminology, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        Terminology terminology1 = Terminology.build(args, closure)
        terminology terminology1
    }

    Terminology terminology(@DelegatesTo(value = Terminology, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        terminology [:], closure
    }

    List<Terminology> terminologies(List<Terminology> terminologies) {
        if(this.terminology) {
            this.terminologies.add(this.terminology)
            this.terminology = null
        }
        this.terminologies.addAll(terminologies)
        terminologies
    }

    DataModel dataModel(DataModel dataModel) {
        if (!this.dataModel && dataModels.size() == 0) {
            this.dataModel = dataModel
        } else {
            if(this.dataModel) {
                this.dataModels.add(this.dataModel)
                this.dataModel = null
            }
            this.dataModels.add(dataModel)
        }
        dataModel
    }

    DataModel dataModel(Map args, @DelegatesTo(value = DataModel, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        DataModel dataModel1 = DataModel.build(args, closure)
        dataModel dataModel1
    }

    DataModel dataModel(@DelegatesTo(value = DataModel, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        dataModel [:], closure
    }

    List<DataModel> dataModels(List<DataModel> dataModels) {
        if(this.dataModel) {
            this.dataModels.add(this.dataModel)
            this.dataModel = null
        }
        this.dataModels.addAll(dataModels)
        dataModels
    }


}
