package uk.ac.ox.softeng.mauro.domain.terminology

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.model.ModelService
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem

/**
 * The CodeSet Service class provides utility functions for manipulating CodeSet objects*/
@CompileStatic
@Singleton
class CodeSetService extends ModelService<CodeSet> {

    Boolean handles(Class clazz) {
        clazz == CodeSet
    }

    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['codeSet', 'codeSets']
    }

}