package org.maurodata.domain.facet

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.MetadataDiff

import org.maurodata.domain.diff.ObjectDiff

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
        new MetadataDiff(id, namespace, key, value, getDiffIdentifier())
    }


    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if (multiFacetAwareItem != null) {
            return "${multiFacetAwareItem.getDiffIdentifier()}|${pathPrefix}:${this.namespace}.${this.key}"
        }
        return "${pathPrefix}:${this.namespace}.${this.key}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Metadata> diff(Metadata other, String lhsPathRoot, String rhsPathRoot) {
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
        @DelegatesTo(value = Metadata, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Metadata(args).tap(closure)
    }

    static Metadata build(
        @DelegatesTo(value = Metadata, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
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

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'md'
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        "${this.namespace}.${this.key}"
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Metadata intoMetadata = (Metadata) into
        intoMetadata.namespace = ItemUtils.copyItem(this.namespace, intoMetadata.namespace)
        intoMetadata.key = ItemUtils.copyItem(this.key, intoMetadata.key)
        intoMetadata.value = ItemUtils.copyItem(this.value, intoMetadata.value)
    }

    @Override
    Item shallowCopy() {
        Metadata metadataShallowCopy = new Metadata()
        this.copyInto(metadataShallowCopy)
        return metadataShallowCopy
    }
}