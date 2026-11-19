package org.maurodata.test.domain.model

import spock.lang.Specification
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.model.version.VersionChangeType
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder

/**
 * ModelVersionSpec is a class for testing functionality relating to the ModelVersion class
 * @see ModelVersion
 */
class ModelVersionSpec extends Specification {

    void 'path model identifier matching distinguishes finalised identifiers from draft branches'() {
        given:
        DataModel finalisedModel = new DataModel(finalised: true, modelVersion: ModelVersion.from('1.2.3'), modelVersionTag: 'published')
        DataModel draftModel = new DataModel(finalised: false, branchName: 'main')

        expect:
        finalisedModel.matchesFinalisedPathModelIdentifier('1.2.3')
        finalisedModel.matchesFinalisedPathModelIdentifier('published')
        !finalisedModel.matchesDraftPathModelIdentifier('main')
        draftModel.matchesDraftPathModelIdentifier('main')
        !draftModel.matchesFinalisedPathModelIdentifier('1.2.3')
        !draftModel.matchesFinalisedPathModelIdentifier('main')
    }

    void 'path model identifier matching uses the version owning ancestor'() {
        given:
        Folder versionOwningFolder = new Folder(finalised: true, modelVersion: ModelVersion.from('2.0.0'), modelVersionTag: 'release')
        DataModel nestedModel = new DataModel(folder: versionOwningFolder)

        expect:
        nestedModel.matchesFinalisedPathModelIdentifier('2.0.0')
        nestedModel.matchesFinalisedPathModelIdentifier('release')
        !nestedModel.matchesDraftPathModelIdentifier('main')
    }

    void "Test the DSL for creating objects"() {

        when:

        ModelVersion modelVersion1 = ModelVersion.build(major: 1, minor: 2, patch: 3, snapshot: true)
        ModelVersion modelVersion2 = ModelVersion.build {
            major 1
            minor 2
            patch 3
            snapshot true

        }
        ModelVersion modelVersion3 = ModelVersion.from("1.2.3-SNAPSHOT")

        ModelVersion modelVersion4 = ModelVersion.build {
            major 5
            minor 6
            patch 7
            snapshot false

        }

        then:
        modelVersion1 == modelVersion2
        modelVersion2 == modelVersion3

        modelVersion1.toString() == "1.2.3-SNAPSHOT"

        modelVersion4.toString() == "5.6.7"

    }

    void "Test incrementing the version"() {

        when:
        ModelVersion modelVersion1 = ModelVersion.build(major: 1, minor: 2, patch: 3, snapshot: true)

        then:
        modelVersion1.nextMajorVersion().toString() == "2.0.0"
        modelVersion1.nextMinorVersion().toString() == "1.3.0"
        modelVersion1.nextPatchVersion().toString() == "1.2.4"

        modelVersion1.nextVersion(VersionChangeType.MAJOR).toString() == "2.0.0"
        modelVersion1.nextVersion(VersionChangeType.MINOR).toString() == "1.3.0"
        modelVersion1.nextVersion(VersionChangeType.PATCH).toString() == "1.2.4"

    }
}
