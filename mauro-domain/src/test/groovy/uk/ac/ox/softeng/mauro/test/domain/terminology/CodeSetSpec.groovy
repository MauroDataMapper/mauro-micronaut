package uk.ac.ox.softeng.mauro.test.domain.terminology

import spock.lang.Specification
import spock.lang.Unroll
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.test.domain.TestModelData

/**
 * Tests for CodeSet domain object
 */
class CodeSetSpec extends Specification {
    @Override
    Object invokeMethod(String name, Object args) {
        return super.invokeMethod(name, args)
    }

    @Unroll()
    void "Testing the constructor  #iteration.testName"() {
        when:
        def codeSet = new CodeSet()
        codeSet.label = iteration.label
        codeSet.catalogueUser = new CatalogueUser(emailAddress: iteration.createdBy)
        codeSet.folder = iteration.folder
        codeSet.authority = iteration.authority
        codeSet.documentationVersion = iteration.documentationVersion
        codeSet.readableByEveryone = iteration.readableByEveryone
        codeSet.metadata = iteration.metadata
        codeSet.terms = iteration.terms

        then:
        codeSet != null
        codeSet.label == iteration.label
        codeSet.createdBy == iteration.createdBy
        codeSet.folder == iteration.folder
        codeSet.authority == iteration.authority
        codeSet.documentationVersion == iteration.documentationVersion
        codeSet.readableByEveryone == iteration.readableByEveryone
        codeSet.metadata == iteration.metadata


        where:
        iteration << [
                [testName            : "Basic minimal CodeSet ",
                 label               : "codeset label",
                 createdBy           : "codeset@email.com",
                 folder              : new Folder(),
                 authority           : new Authority(),
                 documentationVersion: null,
                 readableByEveryone  : true,
                 metadata            : [new Metadata().with {
                     namespace = 'namespace-1'
                     key = 'key-1'
                     value = 'value-1'
                     return it
                 },
                                        new Metadata().with {
                                            namespace = 'namespace-2'
                                            key = 'key-2'
                                            value = 'value-2'
                                            return it
                                        }
                 ]
                ],

                [testName            : "CodeSet with Terms ",
                 label               : "another  label",
                 createdBy           : "codeset@email.com",
                 folder              : new Folder(),
                 authority           : new Authority(),
                 documentationVersion: "version 1.1",
                 terms               :
                         [
                                 new Term().with {
                                     code: "B15.0"
                                     definition: "Hepatitis A with hepatic coma"
                                 },
                                 new Term().with {
                                     code: "B15.9"
                                     definition "Hepatitis A without hepatic coma"
                                     description "Hepatitis A (acute)(viral) NOS"
                                 }
                         ]
                ]
        ]
    }

    void 'clone -should clone object with same folder and terms '() {
        given:
        CodeSet original = TestModelData.testCodeSet
        CodeSet cloned = original.clone()

        cloned.is(original)
        cloned.domainType.is(original.domainType)
        cloned.terms.size() == original.terms.size()
        cloned.folder== original.folder
        cloned.folder.is(original.folder)

        cloned.terms.is(original.terms)
        cloned.terms.toSorted() ==  original.terms.toSorted()

        ObjectDiff objectDiff = original.diff(cloned)
        objectDiff.numberOfDiffs == 0
    }

}