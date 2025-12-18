package org.maurodata.domain.facet

import groovy.transform.CompileStatic

@CompileStatic
trait CatalogueFileOutput {
    abstract byte[] fileContent()

}