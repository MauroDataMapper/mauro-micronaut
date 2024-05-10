package uk.ac.ox.softeng.mauro.domain.facet

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id', 'namespace', 'key'], unique = true)])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Metadata extends Facet {

    String namespace

    String key

    String value

    Metadata() {}

    static Metadata build(
        Map args,
        @DelegatesTo(value = Metadata, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new Metadata(args).tap(closure)
    }

    static Metadata build(@DelegatesTo(value = Metadata, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    String namespace(String namespace) {
        this.namespace = namespace
        this.namespace
    }

    String key(String key) {
        this.key = key
        this.key
    }

    String value(String value) {
        this.value = value
        this.value
    }


}
