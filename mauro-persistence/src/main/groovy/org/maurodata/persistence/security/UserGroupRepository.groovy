package org.maurodata.persistence.security

import org.maurodata.domain.model.AdministeredItem

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
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

    @Query('delete from security.user_group_catalogue_user where catalogue_user_id = :catalogueUserId AND user_group_id = :uuid')
    abstract long deleteCatalogueUser(@NonNull UUID uuid, @NonNull UUID catalogueUserId)

    @Nullable
    abstract List<UserGroup> readAllByName(String name)

    @Override
    Class getDomainClass() {
        UserGroup
    }

    // Not pathable
    Boolean handlesPathPrefix(final String pathPrefix) {
        false
    }
}
