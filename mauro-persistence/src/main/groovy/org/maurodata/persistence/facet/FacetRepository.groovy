package org.maurodata.persistence.facet

import groovy.transform.CompileStatic
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.model.Item
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
trait FacetRepository<F extends Facet> implements ItemRepository<F> {


    abstract Set<F> readAllByMultiFacetAwareItemId(UUID ownerId)

    abstract Set<F> readAllByMultiFacetAwareItemIdIn(Collection<UUID> ownerIds)

    abstract Set<UUID> readAllIdByMultiFacetAwareItemIdIn(Collection<UUID> ownerIds)

    abstract Long deleteByMultiFacetAwareItemIdIn(List<UUID> itemIds)

    abstract Class<F> getDomainClass()

    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    Boolean handles(String domainType) {
        domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 's').equalsIgnoreCase(domainType)
    }

}
