package org.maurodata.plugin.importer

import org.maurodata.domain.folder.Folder

trait FolderImporterPlugin<P extends ImportParameters> extends ModelImporterPlugin<Folder, P> {

    abstract List<Folder> importDomain(P params)

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }

    abstract Class<P> importParametersClass()

    @Override
    Class<Folder> getHandlesModelType() {
        Folder
    }


}
