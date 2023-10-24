package uk.ac.ox.softeng.mauro.domain.folder

import uk.ac.ox.softeng.mauro.domain.model.ModelService

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@Singleton
@CompileStatic
class FolderService extends ModelService<Folder> {

    Boolean handles(Class clazz) {
        clazz == Folder
    }

    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['folder', 'folders']
    }
}
