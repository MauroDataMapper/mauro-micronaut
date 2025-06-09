package uk.ac.ox.softeng.mauro.domain.facet

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

/**
 * A Service class that provides utility functions for working with data models.
 */
@CompileStatic
@Singleton
class AnnotationService extends FacetService<Annotation> {

   Boolean handles(Class clazz){
       clazz.simpleName == Annotation.simpleName
   }

   Boolean handles(String domainType){
       return domainType!=null && domainType.toLowerCase() in ['annotation', 'annotations']
   }
}