package org.maurodata.persistence.federation


import org.maurodata.domain.facet.federation.SubscribedModel
import org.maurodata.persistence.model.ItemRepository

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
    @Query('select * from federation.subscribed_model where subscribed_catalogue_id = :subscribedCatalogueId')
    abstract List<SubscribedModel> findAllBySubscribedCatalogueId(UUID subscribedCatalogueId)

    @Nullable
    @Query('select * from federation.subscribed_model where id = :id  and subscribed_catalogue_id = :subscribedCatalogueId')
    abstract SubscribedModel findByIdAndSubscribedCatalogueId(UUID id, UUID subscribedCatalogueId)

    @Override
    Class getDomainClass() {
        SubscribedModel
    }

    // Not currently pathable
    Boolean handlesPathPrefix(final String pathPrefix) {
        false
    }
}