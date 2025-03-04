package uk.ac.ox.softeng.mauro.plugin.importer


import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet

trait CodeSetImporterPlugin<P extends ImportParameters> extends ModelImporterPlugin<CodeSet, P> {

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }

    abstract Class<P> importParametersClass()

    @Override
    Class<CodeSet> getHandlesModelType() {
        CodeSet
    }
}
