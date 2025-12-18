package org.maurodata.plugin.exporter


import org.maurodata.domain.folder.Folder

import groovy.transform.CompileStatic

@CompileStatic
trait FolderExporterPlugin extends ModelExporterPlugin<Folder> {

    @Override
    Class<Folder> getHandlesModelType() {
        Folder
    }
}
