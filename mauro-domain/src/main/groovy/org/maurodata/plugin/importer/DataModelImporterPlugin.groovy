package org.maurodata.plugin.importer

import org.maurodata.domain.datamodel.DataModel

trait DataModelImporterPlugin <P extends ImportParameters> extends ModelImporterPlugin<DataModel, P> {

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }

    abstract Class<P> importParametersClass()

    @Override
    Class<DataModel> getHandlesModelType() {
        DataModel
    }




}
