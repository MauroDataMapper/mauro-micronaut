package org.maurodata.test.domain.model

import groovy.transform.CompileStatic
import spock.lang.Specification
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.model.version.VersionChangeType

/**
 * ModelVersionSpec is a class for testing functionality relating to the ModelVersion class
 * @see ModelVersion
 */
class ModelVersionSpec extends Specification {

    def "Test the DSL for creating objects"() {

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

    def "Test incrementing the version"() {

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
