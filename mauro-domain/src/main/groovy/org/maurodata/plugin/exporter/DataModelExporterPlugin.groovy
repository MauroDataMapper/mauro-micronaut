package org.maurodata.plugin.exporter

import org.maurodata.domain.datamodel.DataModel

import groovy.transform.CompileStatic

@CompileStatic
trait DataModelExporterPlugin extends ModelExporterPlugin<DataModel> {

    @Override
    Class<DataModel> getHandlesModelType() {
        DataModel
    }
}
