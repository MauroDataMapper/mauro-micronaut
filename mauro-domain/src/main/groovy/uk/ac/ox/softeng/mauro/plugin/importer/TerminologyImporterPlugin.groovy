package uk.ac.ox.softeng.mauro.plugin.importer

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

trait TerminologyImporterPlugin<P extends ImportParameters> extends ModelImporterPlugin<Terminology, P> {

    abstract List<Terminology> importDomain(P params)

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }

    abstract Class<P> importParametersClass()

    @Override
    Class<Terminology> getHandlesModelType() {
        Terminology
    }


}
