package uk.ac.ox.softeng.mauro.plugin.exporter


import uk.ac.ox.softeng.mauro.domain.folder.Folder

trait FolderExporterPlugin extends ModelExporterPlugin<Folder> {

    @Override
    Class<Folder> getHandlesModelType() {
        Folder
    }
}
