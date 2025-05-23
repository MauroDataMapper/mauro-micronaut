package uk.ac.ox.softeng.mauro.profile

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ProfileSection {

    String label
    String description

    List<ProfileField> fields

    @Deprecated
    String getName() {
        label
    }

}
