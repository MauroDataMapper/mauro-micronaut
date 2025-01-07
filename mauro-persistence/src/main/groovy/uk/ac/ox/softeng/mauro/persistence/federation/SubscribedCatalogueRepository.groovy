package uk.ac.ox.softeng.mauro.persistence.federation

import uk.ac.ox.softeng.mauro.domain.federation.SubscribedCatalogue
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SubscribedCatalogueRepository implements ItemRepository<SubscribedCatalogue> {

    @Nullable
    abstract SubscribedCatalogue findById(UUID id)

    @Nullable
    @Query('select * from federation.subscribed_catalogue')
    abstract List<SubscribedCatalogue> findAll()

    @Override
    Class getDomainClass() {
        SubscribedCatalogue
    }

}