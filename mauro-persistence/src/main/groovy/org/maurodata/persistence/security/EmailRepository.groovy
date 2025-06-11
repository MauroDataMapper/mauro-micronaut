package org.maurodata.persistence.security

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import io.micronaut.data.repository.PageableRepository
import jakarta.validation.Valid
import org.maurodata.domain.authority.Authority
import org.maurodata.domain.email.Email
import org.maurodata.domain.security.CatalogueUser
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class EmailRepository implements PageableRepository<Email, UUID> {

    @Nullable
    abstract Optional<Email> findById(UUID id)

    abstract List<Email> readAll()

    @Nullable
    abstract Email readById(UUID id)

    abstract Email save(@Valid @NonNull Email item)

    abstract List<Email> saveAll(@Valid @NonNull Iterable<Email> items)

    abstract Email update(@Valid @NonNull Email item)

    abstract List<Email> updateAll(@Valid @NonNull Iterable<Email> item)

    abstract void delete(@NonNull Email item)

    abstract void deleteAll(@NonNull Iterable<Email> items)





}
