package org.maurodata.test.domain.referencefile


import spock.lang.Specification
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.terminology.Terminology
import org.maurodata.test.domain.TestModelData

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

    void 'deep clone -should clone ReferenceFile'() {
        given:
        ReferenceFile original = TestModelData.testReferenceFile

        when:
        ReferenceFile cloned = (ReferenceFile) original.deepClone()
        then:

        !cloned.is(original)

        cloned.fileName == original.fileName

        ObjectDiff diff = cloned.diff(original)
        diff.numberOfDiffs == 0
    }

    void 'deep clone diff - should report difference on filename field only'() {
        given:
        ReferenceFile original = TestModelData.testReferenceFile
        ReferenceFile cloned = (ReferenceFile) original.deepClone()
        cloned.fileName = 'new test filename'
        cloned.fileSize = cloned.fileName.size()

        when:
        ObjectDiff diff = cloned.diff(original)

        then:
        diff.numberOfDiffs == 1
    }
}