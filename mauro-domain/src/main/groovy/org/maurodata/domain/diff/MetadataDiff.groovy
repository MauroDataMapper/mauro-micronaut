package org.maurodata.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class MetadataDiff extends CollectionDiff{

    String namespace
    String key
    String value

    MetadataDiff(UUID id, String namespace, String key, String value, String diffIdentifier) {
        super(id,diffIdentifier)
        this.namespace = namespace
        this.key = key
        this.value = value
    }
}
