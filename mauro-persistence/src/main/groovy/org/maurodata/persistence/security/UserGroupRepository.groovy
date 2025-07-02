package org.maurodata.persistence.security

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.security.UserGroup
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class UserGroupRepository implements ItemRepository<UserGroup> {

    @Query('select * from security.user_group ug where exists (select * from security.user_group_catalogue_user ugcu where ug.id=ugcu.user_group_id and ugcu.catalogue_user_id = :catalogueUserId)')
    abstract List<UserGroup> readAllByCatalogueUserId(UUID catalogueUserId)

    @Query('insert into security.user_group_catalogue_user (catalogue_user_id, user_group_id) values (:catalogueUserId, :uuid)')
    abstract UserGroup addCatalogueUser(@NonNull UUID uuid, @NonNull UUID catalogueUserId)

    @Override
    Class getDomainClass() {
        UserGroup
    }

    // Not pathable
    Boolean handlesPathPrefix(final String pathPrefix) {
        false
    }
}
