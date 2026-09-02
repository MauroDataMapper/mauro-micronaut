package org.maurodata.persistence.security

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class CatalogueUserRepository implements ItemRepository<CatalogueUser> {

    @Nullable
    abstract CatalogueUser readByEmailAddress(String emailAddress)

    @Nullable
    abstract boolean existsByEmailAddress(String emailAddress)

    @Nullable
    abstract List<CatalogueUser> readByPendingAndDisabled(boolean pending, boolean disabled)

    @Nullable
    abstract List<CatalogueUser> readAll()

    @Nullable
    @Query('select * from security.catalogue_user cu where exists (select * from security.user_group_catalogue_user ugcu where cu.id=ugcu.catalogue_user_id and ugcu.user_group_id = :userGroupId)')
    abstract List<CatalogueUser> readAllByUserGroupId(UUID userGroupId)

    @Nullable
    abstract List<CatalogueUser> readAllByEmailAddressIlike(String emailAddress)

    @Override
    Class getDomainClass() {
        CatalogueUser
    }

    // Not pathable
    Boolean handlesPathPrefix(final String pathPrefix) {
        false
    }
}
