package org.maurodata.profile

import com.fasterxml.jackson.annotation.JsonAlias
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ProfileSection {

    @JsonAlias('sectionName')
    String label

    @JsonAlias('sectionDescription')
    String description

    List<ProfileField> fields = []

    List<ProfileSection> sections = []

    @Deprecated
    String getName() {
        label
    }

}
