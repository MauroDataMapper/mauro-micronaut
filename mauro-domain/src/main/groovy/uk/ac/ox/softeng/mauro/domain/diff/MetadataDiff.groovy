package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
class MetadataDiff extends CollectionDiff{

    String namespace
    String key
    String value

    MetadataDiff(UUID id, String namespace, String key, String value) {
        super(id)
        this.namespace = namespace
        this.key = key
        this.value = value
    }
}
