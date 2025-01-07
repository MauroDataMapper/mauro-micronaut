package uk.ac.ox.softeng.mauro.persistence.federation


import uk.ac.ox.softeng.mauro.domain.federation.SubscribedModel
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SubscribedModelRepository implements ItemRepository<SubscribedModel> {

    @Nullable
    abstract SubscribedModel findById(UUID id)

    @Nullable
    @Query('select * from federation.subscribed_model')
    abstract List<SubscribedModel> findAll()

    @Nullable
    @Query('select * from federation.subscribed_model')
    abstract List<SubscribedModel> findAllBySubscribedCatalogueId(UUID uuid)

    @Override
    Class getDomainClass() {
        SubscribedModel
    }
}