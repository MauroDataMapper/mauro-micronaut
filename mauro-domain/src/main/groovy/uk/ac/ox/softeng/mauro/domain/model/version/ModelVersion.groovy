package uk.ac.ox.softeng.mauro.domain.model.version

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import groovy.transform.MapConstructor
import io.micronaut.core.convert.ConversionContext
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import io.micronaut.data.model.runtime.convert.AttributeConverter

import java.util.regex.Matcher
import java.util.regex.Pattern

@JsonDeserialize(converter = ModelVersionConverter)
@MapConstructor(includeSuperFields = true, includeSuperProperties = true)
@TypeDef(type = DataType.STRING, converter = ModelVersionConverter)
class ModelVersion implements Comparable<ModelVersion> {
    int major
    int minor
    int patch
    boolean snapshot

    static final Pattern VERSION_PATTERN = ~/((\d+)(\.(\d+)(\.(\d+))?)?(-SNAPSHOT)?)|SNAPSHOT/

    @Override
    int compareTo(ModelVersion that) {

        def result = this.major <=> that.major
        if (result == 0) result = this.minor <=> that.minor
        if (result == 0) result = this.patch <=> that.patch
        if (result == 0) result = this.snapshot <=> that.snapshot
        result
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ModelVersion version = (ModelVersion) o

        if (major != version.major) return false
        if (minor != version.minor) return false
        if (patch != version.patch) return false
        snapshot == version.snapshot
    }

    @Override
    int hashCode() {
        int result
        result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + (snapshot ? 1 : 0)
        result
    }

    @Override
    String toString() {
        if (major == 0 && minor == 0 && patch == 0 && snapshot) {
            return 'SNAPSHOT'
        }
        snapshot ? "${major}.${minor}.${patch}-SNAPSHOT" : "${major}.${minor}.${patch}"
    }

    ModelVersion nextMajorVersion() {
        new ModelVersion(major: major + 1, minor: 0, patch: 0)
    }

    ModelVersion nextMinorVersion() {
        new ModelVersion(major: major, minor: minor + 1, patch: 0)
    }

    ModelVersion nextPatchVersion() {
        new ModelVersion(major: major, minor: minor, patch: patch + 1)
    }

    ModelVersion nextVersion(VersionChangeType versionChangeType) {
        switch (versionChangeType) {
            case versionChangeType.MAJOR -> nextMajorVersion()
            case versionChangeType.MINOR -> nextMinorVersion()
            case versionChangeType.PATCH -> nextPatchVersion()
        }
    }

    static ModelVersion from(String versionStr) {

        if (!versionStr) throw new IllegalStateException('Must have a version')

        if (versionStr == 'SNAPSHOT') return new ModelVersion(major: 0, minor: 0, patch: 0, snapshot: true)

        Matcher m = VERSION_PATTERN.matcher(versionStr)
        if (!m.matches()) {
            throw new IllegalStateException("Version '${versionStr}' does not match the expected pattern")
        }

        new ModelVersion(major: m.group(2).toInteger(),
                         minor: m.group(4)?.toInteger() ?: 0,
                         patch: m.group(6)?.toInteger() ?: 0,
                         snapshot: m.group(7) ? true : false
        )
    }

    static ModelVersion build(Map args, @DelegatesTo(value = ModelVersion.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new ModelVersion(args).tap(closure)
    }

    static ModelVersion build(@DelegatesTo(value = ModelVersion.class, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    int major(int major) {
        this.major = major
        return major
    }

    int minor(int minor) {
        this.minor = minor
        return minor
    }

    int patch(int patch) {
        this.patch = patch
        return patch
    }

    boolean snapshot(boolean snapshot) {
        this.snapshot = snapshot
        return snapshot
    }



}
