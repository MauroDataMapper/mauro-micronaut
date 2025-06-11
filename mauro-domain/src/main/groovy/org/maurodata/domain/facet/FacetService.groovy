package org.maurodata.domain.facet

import groovy.transform.CompileStatic
import org.maurodata.domain.model.Item

/**
 * A Service class that provides utility functions for working with facets.
 */
@CompileStatic
abstract class FacetService<I extends Item> {

    abstract Boolean handles(Class clazz)

    abstract Boolean handles(String domainType)


}