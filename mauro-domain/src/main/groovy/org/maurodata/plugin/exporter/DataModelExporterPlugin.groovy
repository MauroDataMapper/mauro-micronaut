package org.maurodata.plugin.exporter

import org.maurodata.domain.datamodel.DataModel

trait DataModelExporterPlugin extends ModelExporterPlugin<DataModel> {

    @Override
    Class<DataModel> getHandlesModelType() {
        DataModel
    }
}
