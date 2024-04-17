package uk.ac.ox.softeng.mauro.domain.facet

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.model.Item

/**
 * A Service class that provides utility functions for working with facets.
 */
@CompileStatic
abstract class FacetService<I extends Item> {

    abstract Boolean handles(Class clazz)

    abstract Boolean handles(String domainType)


}