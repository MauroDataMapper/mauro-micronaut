package uk.ac.ox.softeng.mauro.domain.facet

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id', 'namespace', 'key'], unique = true)])
class Metadata extends Facet {

    String namespace

    String key

    String value
}
