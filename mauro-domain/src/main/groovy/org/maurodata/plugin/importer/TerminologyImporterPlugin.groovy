package org.maurodata.plugin.importer

import org.maurodata.domain.terminology.Terminology

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
