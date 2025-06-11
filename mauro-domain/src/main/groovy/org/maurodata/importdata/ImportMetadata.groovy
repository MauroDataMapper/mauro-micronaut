package org.maurodata.importdata

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable

@CompileStatic
class ImportMetadata {

    String namespace
    String name
    @Nullable
    String version

    boolean allFieldsPresent() {
        namespace && name && version
    }

    boolean hasName() {
        name
    }

    boolean hasNameSpace() {
        namespace
    }
}
