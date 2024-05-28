package uk.ac.ox.softeng.mauro.plugin.exporter

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel

trait DataModelExporterPlugin extends ModelExporterPlugin<DataModel> {

    @Override
    Class<DataModel> getHandlesModelType() {
        DataModel
    }
}
