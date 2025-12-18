package org.maurodata.plugin.importer


import org.maurodata.domain.terminology.CodeSet

import groovy.transform.CompileStatic

@CompileStatic
trait CodeSetImporterPlugin<P extends ImportParameters> extends ModelImporterPlugin<CodeSet, P> {

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }

    abstract Class<P> importParametersClass()

    @Override
    Class<CodeSet> getHandlesModelType() {
        CodeSet
    }
}
