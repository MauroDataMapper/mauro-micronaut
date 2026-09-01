package org.maurodata.profile

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ProfileClassifier {

    String namespace
    String label
    String description

}
