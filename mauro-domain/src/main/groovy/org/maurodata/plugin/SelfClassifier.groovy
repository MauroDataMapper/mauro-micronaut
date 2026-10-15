package org.maurodata.plugin

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class SelfClassifier {

    String namespace
    String label
    String description

}
