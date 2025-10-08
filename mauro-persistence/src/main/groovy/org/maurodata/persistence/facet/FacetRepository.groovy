package org.maurodata.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.repository.GenericRepository
import jakarta.validation.Valid
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.model.Item
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
trait FacetRepository<F extends Facet> implements ItemRepository<F> {


    abstract List<F> readAllByMultiFacetAwareItemId(UUID ownerId)

    abstract List<F> readAllByMultiFacetAwareItemIdIn(Collection<UUID> ownerIds)


    abstract Class<F> getDomainClass()

    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    Boolean handles(String domainType) {
        domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 's').equalsIgnoreCase(domainType)
    }

}
