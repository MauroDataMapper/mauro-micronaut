package org.maurodata.domain.terminology

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.FieldConstants
import org.maurodata.domain.model.ModelService

/**
 * The CodeSet Service class provides utility functions for manipulating CodeSet objects*/
@CompileStatic
@Singleton
class CodeSetService extends ModelService<CodeSet> {

    Boolean handles(Class clazz) {
        clazz == CodeSet
    }

    Boolean handles(String domainType) {
        return domainType != null && domainType.toLowerCase() in [FieldConstants.CODESET_LOWERCASE, FieldConstants.CODESETS_LOWERCASE]
    }

}