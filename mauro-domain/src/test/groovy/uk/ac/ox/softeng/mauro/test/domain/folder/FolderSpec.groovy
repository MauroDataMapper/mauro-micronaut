package uk.ac.ox.softeng.mauro.test.domain.folder

import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.test.domain.TestModelData

class FolderSpec extends Specification {
    static String LABEL = 'My Test Folder'
    static String AUTHOR = 'My Test Folder author : anon'
    static String DESCRIPTION = 'This is an example of a folder'

    static Folder testFolder = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}

    def "Test the DSL for creating objects"() {

        given:
        testFolder
        when:
        testFolder != null

        then:
        testFolder.label == LABEL
        testFolder.author == AUTHOR
        testFolder.description == DESCRIPTION
    }

    void 'test clone -the folder should be cloned, along with all associated objects'() {
        when:
        Terminology simple  = TestModelData.testSimpleTerminology
        then:
        simple
        when:
        Folder child  = TestModelData.testChildFolder
        child.terminologies = [simple]

        then:
        child

        Folder folder = TestModelData.testComplexFolder
        folder.childFolders.add(child)
        when:
        Folder cloned = folder.clone()
        then:
        cloned
        !cloned.is(folder)
        cloned.childFolders.folder.size() == 1
        folder.childFolders.folder.size() == 1
        !cloned.childFolders.folder[0].is(folder.childFolders.folder[0])

        !cloned.dataModels.is(folder.dataModels)
        cloned.dataModels.dataTypes == folder.dataModels.dataTypes
        !cloned.dataModels.dataTypes.is(folder.dataModels.dataTypes)  //groovy object equality. cloned is not original

        cloned.dataModels.allDataClasses == folder.dataModels.allDataClasses
        !cloned.dataModels.allDataClasses.is(folder.dataModels.allDataClasses)

        cloned.dataModels.dataElements == folder.dataModels.dataElements
        !cloned.dataModels.dataElements.is(folder.dataModels.dataElements)

        cloned.dataModels.enumerationValues == folder.dataModels.enumerationValues
        !cloned.dataModels.enumerationValues.is(folder.dataModels.enumerationValues)  //groovy object equal

        cloned.dataModels.dataElements.dataClass.toSet().each{
            cloned.dataModels.allDataClasses.contains(it)
        }

        folder.dataModels.dataElements.dataClass.toSet().each {
            folder.dataModels.allDataClasses
        }

        cloned.dataModels.dataElements.dataType == folder.dataModels.dataElements.dataType
        cloned.dataModels.dataElements.dataType.toSet().each{
            cloned.dataModels.dataTypes.contains(it)
        }
        folder.dataModels.dataElements.dataType.toSet().each{
            folder.dataModels.dataTypes.contains(it)
        }

        !cloned.dataModels.dataElements.dataType.is(folder.dataModels.dataElements.dataType)

        !cloned.terminologies.is(folder.terminologies)
        cloned.terminologies.terms == folder.terminologies.terms
        !cloned.terminologies.terms.is(folder.terminologies.terms)

        cloned.terminologies.termRelationshipTypes == folder.terminologies.termRelationshipTypes
        !cloned.terminologies.termRelationshipTypes.is(folder.terminologies.termRelationshipTypes)

        cloned.terminologies.termRelationships == folder.terminologies.termRelationships
        !cloned.terminologies.termRelationships.is(folder.terminologies.termRelationships)

        folder.codeSets.size() == 1
        cloned.codeSets.size() == 1
        cloned.codeSets[0] == folder.codeSets[0]
        !cloned.codeSets[0].is(folder.codeSets[0])
        Set<Term> clonedTerms =  cloned.codeSets[0].terms
        Set<Term> originalTerms = folder.codeSets[0].terms
        originalTerms.is(clonedTerms)
        originalTerms == clonedTerms
        Folder originalCodeSetFolder = folder.codeSets[0].folder
        Folder clonedCodeSetFolder = cloned.codeSets[0].folder

        !originalCodeSetFolder.is(clonedCodeSetFolder)

        ObjectDiff diff = folder.diff(cloned)
        diff.numberOfDiffs == 0
    }
}
