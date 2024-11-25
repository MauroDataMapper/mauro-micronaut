package uk.ac.ox.softeng.mauro.test.domain.facet

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import uk.ac.ox.softeng.mauro.domain.facet.AnnotationService

import java.text.Annotation

/**
 * Tests for Annotation Service
 */
class AnnotationSpec extends Specification {
    @Override
    Object invokeMethod(String name, Object args) {
        return super.invokeMethod(name, args)
    }

    @Shared
    AnnotationService annotationService

    def setupSpec(){
        annotationService = new AnnotationService()
    }

    void "Test handles()"() {
        when:
        boolean result = annotationService.handles(Annotation)

        then:
        result
    }

    @Unroll()
    void "Test handles domain Type()"() {
        when:
        boolean result = annotationService.handles(iteration)
        then:
        result

        where:
        iteration << [
                Annotation.class.simpleName,
                "annotations"
        ]
    }
}