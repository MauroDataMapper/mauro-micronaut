package uk.ac.ox.softeng.mauro.plugin.importer

import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin

trait ModelImporterPlugin <D extends Model, P extends ImportParameters> extends MauroPlugin {

    abstract D importDomain(P params)

    abstract List<D> importDomains(P params)

    abstract Boolean canImportMultipleDomains

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }



}