package uk.ac.ox.softeng.mauro.plugin.importer

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin

trait DataModelImporterPlugin <P extends ImportParameters> extends ModelImporterPlugin<DataModel, P> {

    abstract DataModel importDomain(P params)

    abstract List<DataModel> importDomains(P params)

    abstract Boolean canImportMultipleDomains()

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }

    abstract Class<P> importParametersClass()




}
