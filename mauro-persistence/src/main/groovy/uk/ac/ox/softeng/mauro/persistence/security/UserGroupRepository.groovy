package uk.ac.ox.softeng.mauro.persistence.security

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import uk.ac.ox.softeng.mauro.domain.security.UserGroup
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class UserGroupRepository implements ItemRepository<UserGroup> {

    @Query('select * from security.user_group ug where exists (select * from security.user_group_catalogue_user ugcu where ug.id=ugcu.user_group_id and ugcu.catalogue_user_id = :catalogueUserId)')
    abstract UserGroup readAllByCatalogueUserId(UUID catalogueUserId)

    @Override
    Class getDomainClass() {
        UserGroup
    }
}
