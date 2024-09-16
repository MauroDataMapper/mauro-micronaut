package uk.ac.ox.softeng.mauro.persistence.model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.repository.GenericRepository
import jakarta.validation.Valid
import uk.ac.ox.softeng.mauro.domain.model.Item

@CompileStatic
trait ItemRepository<I extends Item> implements GenericRepository<I, UUID> {

    // Should be implemented by override with facet joins, possibly using a DTO
    @Nullable
    abstract I findById(UUID id)

    @Nullable
    abstract I readById(UUID id)

    abstract I save(@Valid @NonNull I item)

    abstract List<I> saveAll(@Valid @NonNull Iterable<I> items)

    abstract I update(@Valid @NonNull I item)

    abstract List<I> updateAll(@Valid @NonNull Iterable<I> item)

    abstract Long delete(@NonNull I item)

    abstract Long deleteById(@NonNull UUID id)

    abstract Long deleteAll(@NonNull Iterable<I> items)

    abstract Class<I> getDomainClass()

    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    Boolean handles(String domainType) {
        domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 's').equalsIgnoreCase(domainType)
    }
}
