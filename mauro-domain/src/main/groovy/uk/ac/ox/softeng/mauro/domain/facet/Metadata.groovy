package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient
import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.MetadataDiff

import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id', 'namespace', 'key'], unique = true)])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Metadata extends Facet implements DiffableItem<Metadata> {

    String namespace

    String key

    String value

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new MetadataDiff(id, namespace, key, value)
    }


    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        "${this.namespace}.${this.key}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Metadata> diff(Metadata other) {
        ObjectDiff od = DiffBuilder.objectDiff(Metadata)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)
        od.appendString(DiffBuilder.VALUE, this.value, other.value, this, other)
        od.namespace = this.namespace
        od.key = this.key
        od
    }

    /****
     * Methods for building a tree-like DSL
     */

    static Metadata build(
            Map args,
            @DelegatesTo(value = Metadata, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new Metadata(args).tap(closure)
    }

    static Metadata build(
            @DelegatesTo(value = Metadata, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    /**
     * DSL helper method for setting the namespace.  Returns the namespace passed in.
     *
     * @see #namespace
     */
    String namespace(String namespace) {
        this.namespace = namespace
        this.namespace
    }

    /**
     * DSL helper method for setting the key.  Returns the key passed in.
     *
     * @see #key
     */
    String key(String key) {
        this.key = key
        this.key
    }

    /**
     * DSL helper method for setting the value.  Returns the value passed in.
     *
     * @see #value
     */
    String value(String value) {
        this.value = value
        this.value
    }


}