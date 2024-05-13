package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
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
    @java.beans.Transient
    ObjectDiff<Metadata> diff(Metadata other) {
        ObjectDiff od = DiffBuilder.objectDiff(Metadata)
                .leftHandSide(id.toString(), this)
                .rightHandSide(other.id.toString(), other)
        if (this.value != other.value){
            od.appendString('value', this.value, other.value)
        }
        od.namespace = this.namespace
        od.key = this.key
        od
    }
}