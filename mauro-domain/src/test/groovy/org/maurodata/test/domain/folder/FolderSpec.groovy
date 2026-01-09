package org.maurodata.test.domain.folder

import spock.lang.Ignore
import spock.lang.Specification
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.test.domain.TestModelData

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

    // This fails:
    /*
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

        cloned.dataModels.dataElements.dataType.hashCode() != folder.dataModels.dataElements.dataType.hashCode()
        cloned.dataModels.dataElements.dataType.id == folder.dataModels.dataElements.dataType.id
        cloned.dataModels.dataElements.dataType.toSet().each{
            cloned.dataModels.dataTypes.contains(it)
        }
        folder.dataModels.dataElements.dataType.toSet().each{
            folder.dataModels.dataTypes.contains(it)
        }

        !cloned.dataModels.dataElements.dataType.is(folder.dataModels.dataElements.dataType)

        !cloned.terminologies.is(folder.terminologies)
        cloned.terminologies.terms.hashCode() != folder.terminologies.terms.hashCode()
        cloned.terminologies.terms.id == folder.terminologies.terms.id
        !cloned.terminologies.terms.is(folder.terminologies.terms)

        cloned.terminologies.termRelationshipTypes.hashCode() != folder.terminologies.termRelationshipTypes.hashCode()
        cloned.terminologies.termRelationshipTypes.id == folder.terminologies.termRelationshipTypes.id
        !cloned.terminologies.termRelationshipTypes.is(folder.terminologies.termRelationshipTypes)

        cloned.terminologies.termRelationships.hashCode() != folder.terminologies.termRelationships.hashCode()
        cloned.terminologies.termRelationships.id == folder.terminologies.termRelationships.id
        !cloned.terminologies.termRelationships.is(folder.terminologies.termRelationships)

        folder.codeSets.size() == 1
        cloned.codeSets.size() == 1
        //cloned.codeSets[0] == folder.codeSets[0]
        !cloned.codeSets[0].is(folder.codeSets[0])
        cloned.codeSets[0].label == cloned.codeSets[0].label

        Set<Term> clonedTerms =  cloned.codeSets[0].terms
        Set<Term> originalTerms = folder.codeSets[0].terms
        !originalTerms.is(clonedTerms)
        originalTerms == clonedTerms
        Folder originalCodeSetFolder = folder.codeSets[0].folder
        Folder clonedCodeSetFolder = cloned.codeSets[0].folder

        !originalCodeSetFolder.is(clonedCodeSetFolder)

        ObjectDiff diff = folder.diff(cloned)
        diff.numberOfDiffs == 0
    }*/

    void 'deep test clone -the folder should be cloned, along with all associated objects'() {
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
        Folder cloned = (Folder) folder.deepClone()
        cloned.setAssociations()
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

        cloned.dataModels.dataElements.dataType.hashCode() != folder.dataModels.dataElements.dataType.hashCode()
        cloned.dataModels.dataElements.dataType.id == folder.dataModels.dataElements.dataType.id
        cloned.dataModels.dataElements.dataType.toSet().each{
            cloned.dataModels.dataTypes.contains(it)
        }
        folder.dataModels.dataElements.dataType.toSet().each{
            folder.dataModels.dataTypes.contains(it)
        }

        !cloned.dataModels.dataElements.dataType.is(folder.dataModels.dataElements.dataType)

        !cloned.terminologies.is(folder.terminologies)
        cloned.terminologies.terms.hashCode() != folder.terminologies.terms.hashCode()
        cloned.terminologies.terms.id == folder.terminologies.terms.id
        !cloned.terminologies.terms.is(folder.terminologies.terms)

        cloned.terminologies.termRelationshipTypes.hashCode() != folder.terminologies.termRelationshipTypes.hashCode()
        cloned.terminologies.termRelationshipTypes.id == folder.terminologies.termRelationshipTypes.id
        !cloned.terminologies.termRelationshipTypes.is(folder.terminologies.termRelationshipTypes)

        cloned.terminologies.termRelationships.hashCode() != folder.terminologies.termRelationships.hashCode()
        cloned.terminologies.termRelationships.id == folder.terminologies.termRelationships.id
        !cloned.terminologies.termRelationships.is(folder.terminologies.termRelationships)

        folder.codeSets.size() == 1
        cloned.codeSets.size() == 1
        cloned.codeSets[0].terms.size() == folder.codeSets[0].terms.size()
        !cloned.codeSets[0].is(folder.codeSets[0])
        Set<Term> clonedTerms =  cloned.codeSets[0].terms
        Set<Term> originalTerms = folder.codeSets[0].terms
        !originalTerms.is(clonedTerms)
        originalTerms == clonedTerms
        Folder originalCodeSetFolder = folder.codeSets[0].folder
        Folder clonedCodeSetFolder = cloned.codeSets[0].folder

        !originalCodeSetFolder.is(clonedCodeSetFolder)

        ObjectDiff diff = folder.diff(cloned)
        diff.numberOfDiffs == 0
    }

    def "Test non versioned folder with non versioned data model"()
    {
        given:
            Folder folderWithoutVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}
            DataModel dataModelWithoutVersion = DataModel.build(label: 'My Test DataModel with Version', id: UUID.randomUUID()) {}
        when:
            dataModelWithoutVersion.setParent(folderWithoutVersion)
        then:
            dataModelWithoutVersion.getModelVersion() == null &&
            folderWithoutVersion.getModelVersion() == null &&
            dataModelWithoutVersion.getPathModelIdentifier() == null &&
            folderWithoutVersion.getPathModelIdentifier() == null
    }

    def "Test non versioned folder with non versioned sub folder"()
    {
        given:
            Folder folderWithoutVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}
            Folder subFolderWithoutVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}
        when:
            subFolderWithoutVersion.setParent(folderWithoutVersion)
        then:
            subFolderWithoutVersion.getModelVersion() == null &&
            folderWithoutVersion.getModelVersion() == null &&
            subFolderWithoutVersion.getPathModelIdentifier() == null &&
            folderWithoutVersion.getPathModelIdentifier() == null
    }

    def "Test non versioned folder with versioned data model"()
    {
        given:
            Folder folderWithoutVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}
            DataModel dataModelWithVersion = DataModel.build(label: 'My Test DataModel with Version', id: UUID.randomUUID()) {}
            ModelVersion modelVersion = ModelVersion.build(major: 2, minor: 3, patch: 0, snapshot: false){}
            dataModelWithVersion.setModelVersion( modelVersion )
        when:
            dataModelWithVersion.setParent(folderWithoutVersion)
        then:
            folderWithoutVersion.getModelVersion() == null &&
            dataModelWithVersion.getModelVersion() == modelVersion &&
            dataModelWithVersion.getPathModelIdentifier() == "2.3.0" &&
            folderWithoutVersion.getPathModelIdentifier() == null
    }

    @Ignore("'Versioned non-versionable' is no-longer a thing")
    def "Test a versioned non-versionable folder "()
    {
        given:
            Folder folderWithVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}
            ModelVersion modelVersion1 = ModelVersion.build(major: 1, minor: 2, patch: 3, snapshot: true){}
            folderWithVersion.setModelVersion( modelVersion1 )

        when:
            true
        then:
            folderWithVersion.getModelVersion() == modelVersion1 &&
            folderWithVersion.getPathModelIdentifier() == null
    }

    def "Test a versioned versionable folder "()
    {
        given:
        Folder folderWithVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}
        ModelVersion modelVersion1 = ModelVersion.build(major: 1, minor: 2, patch: 3, snapshot: true){}
        folderWithVersion.setModelVersion( modelVersion1 )

        when:
        true
        then:
        folderWithVersion.getModelVersion() == modelVersion1 &&
        folderWithVersion.getPathModelIdentifier() == "1.2.3-SNAPSHOT"
    }

    @Ignore("'Versioned non-versionable' is no-longer a thing")
    def "Test a versioned versionable folder with a versioned non-versionable sub folder"()
    {
        given:
        Folder folderWithVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}
        Folder subFolderWithVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}

        ModelVersion modelVersion1 = ModelVersion.build(major: 1, minor: 2, patch: 3, snapshot: true){}
        ModelVersion modelVersion2 = ModelVersion.build(major: 2, minor: 2, patch: 3, snapshot: false){}

        folderWithVersion.setModelVersion( modelVersion1 )
        subFolderWithVersion.setModelVersion( modelVersion2 )

        when:
            subFolderWithVersion.setParent(folderWithVersion)
        then:
            folderWithVersion.getModelVersion() == modelVersion1 &&
            subFolderWithVersion.getModelVersion() == modelVersion1 &&
            subFolderWithVersion.getPathModelIdentifier() == null &&
            folderWithVersion.getPathModelIdentifier() == "1.2.3-SNAPSHOT"
    }

    @Ignore("VersionedFolders should live inside VersionedFolders")
    def "Test a versioned versionable folder with a versioned versionable sub folder"()
    {
        given:
        Folder folderWithVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}
        Folder subFolderWithVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}

        ModelVersion modelVersion1 = ModelVersion.build(major: 1, minor: 2, patch: 3, snapshot: true){}
        ModelVersion modelVersion2 = ModelVersion.build(major: 2, minor: 2, patch: 3, snapshot: false){}

        folderWithVersion.setModelVersion( modelVersion1 )
        subFolderWithVersion.setModelVersion( modelVersion2 )

        when:
        subFolderWithVersion.setParent(folderWithVersion)
        then:
        folderWithVersion.getModelVersion() == modelVersion1 &&
                subFolderWithVersion.getModelVersion() == modelVersion1 &&
                subFolderWithVersion.getPathModelIdentifier() == null &&
                folderWithVersion.getPathModelIdentifier() == "1.2.3-SNAPSHOT"
    }

    @Ignore("'Versioned non-versionable' is no-longer a thing")
    def "Test a versioned non-versionable folder with a versioned datamodel"()
    {
        given:
        Folder folderWithVersion = Folder.build(label: "$LABEL", author: "$AUTHOR", description: "$DESCRIPTION") {}
        DataModel dataModelWithVersion = DataModel.build(label: 'My Test DataModel with Version', id: UUID.randomUUID()) {}

        ModelVersion modelVersion1 = ModelVersion.build(major: 1, minor: 2, patch: 3, snapshot: true){}
        ModelVersion modelVersion2 = ModelVersion.build(major: 2, minor: 2, patch: 3, snapshot: false){}

        folderWithVersion.setModelVersion( modelVersion1 )
        dataModelWithVersion.setModelVersion( modelVersion2 )

        when:
        dataModelWithVersion.setParent(folderWithVersion)
        then:
            folderWithVersion.getModelVersion() == modelVersion1 &&
            dataModelWithVersion.getModelVersion() == modelVersion2 &&
            dataModelWithVersion.getPathModelIdentifier() == "2.2.3" &&
            folderWithVersion.getPathModelIdentifier() == null
    }
}
