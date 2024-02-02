package uk.ac.ox.softeng.mauro.domain.security

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import uk.ac.ox.softeng.mauro.domain.model.Item

@CompileStatic
@MappedEntity(schema = 'security')
@AutoClone
@Indexes([@Index(columns = ['name'], unique = true)])
class UserGroup extends Item {

    String name

    @Nullable
    String description

    Boolean undeletable

    ApplicationRole applicationRole

    @Relation(Relation.Kind.MANY_TO_MANY)
    Set<CatalogueUser> groupMembers = []
}
