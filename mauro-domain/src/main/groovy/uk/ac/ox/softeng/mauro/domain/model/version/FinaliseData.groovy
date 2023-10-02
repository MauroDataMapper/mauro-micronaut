package uk.ac.ox.softeng.mauro.domain.model.version

import io.micronaut.core.annotation.Introspected

@Introspected
class FinaliseData {

    VersionChangeType versionChangeType

    ModelVersion version

    String versionTag

    String changeNotice
}
