package uk.ac.ox.softeng.mauro.test.domain.referencefile


import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.test.domain.TestModelData

/**
 * TerminologySpec is a class for testing functionality relating to the Terminology class
 * @see Terminology
 */
class ReferenceFileSpec extends Specification {

    void 'clone -should clone ReferenceFile'() {
        given:
        ReferenceFile original = TestModelData.testReferenceFile

        when:
        ReferenceFile cloned = original.clone()
        then:

        !cloned.is(original)

        cloned.fileName == original.fileName

        ObjectDiff diff = cloned.diff(original)
        diff.numberOfDiffs == 0
    }

    void 'diff - should report difference on filename field only'() {
        given:
        ReferenceFile original = TestModelData.testReferenceFile
        ReferenceFile cloned = original.clone()
        cloned.fileName = 'new test filename'
        cloned.fileSize = cloned.fileName.size()

        when:
        ObjectDiff diff = cloned.diff(original)

        then:
        diff.numberOfDiffs == 1
    }
}