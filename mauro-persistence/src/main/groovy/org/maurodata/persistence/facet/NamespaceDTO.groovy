package org.maurodata.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class NamespaceDTO implements Serializable {
    String namespace
    String key
}