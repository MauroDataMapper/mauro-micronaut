package uk.ac.ox.softeng.mauro.domain.facet

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.MetadataDiff

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id', 'namespace', 'key'], unique = true)])
class Metadata extends Facet implements DiffableItem {

    String namespace

    String key

    String value

    @Override
    CollectionDiff fromItem() {
        new MetadataDiff(id, namespace, key, value)
    }
}