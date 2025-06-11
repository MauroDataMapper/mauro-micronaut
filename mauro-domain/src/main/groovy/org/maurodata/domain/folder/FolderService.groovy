package org.maurodata.domain.folder

import org.maurodata.domain.model.ModelService

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@Singleton
@CompileStatic
class FolderService extends ModelService<Folder> {

    Boolean handles(Class clazz) {
        clazz == Folder
    }

    Boolean handles(String domainType) {
        return domainType != null && domainType.toLowerCase() in ['folder', 'folders']
    }
}
