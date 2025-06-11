package org.maurodata.plugin.exporter


import org.maurodata.domain.folder.Folder

trait FolderExporterPlugin extends ModelExporterPlugin<Folder> {

    @Override
    Class<Folder> getHandlesModelType() {
        Folder
    }
}
